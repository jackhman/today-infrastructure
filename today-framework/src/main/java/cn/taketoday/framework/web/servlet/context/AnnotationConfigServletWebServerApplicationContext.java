/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.web.servlet.context;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.BeanNameGenerator;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.AnnotationConfigRegistry;
import cn.taketoday.context.annotation.AnnotatedBeanDefinitionReader;
import cn.taketoday.context.annotation.AnnotationBeanNameGenerator;
import cn.taketoday.context.annotation.AnnotationConfigUtils;
import cn.taketoday.context.annotation.AnnotationScopeMetadataResolver;
import cn.taketoday.context.annotation.ClassPathBeanDefinitionScanner;
import cn.taketoday.context.loader.ScopeMetadataResolver;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * {@link ServletWebServerApplicationContext} that accepts annotated classes as input - in
 * particular {@link cn.taketoday.context.annotation.Configuration @Configuration}
 * -annotated classes, but also plain {@link Component @Component} classes and JSR-330
 * compliant classes using {@code javax.inject} annotations. Allows for registering
 * classes one by one (specifying class names as config location) as well as for classpath
 * scanning (specifying base packages as config location).
 * <p>
 * Note: In case of multiple {@code @Configuration} classes, later {@code @Bean}
 * definitions will override ones defined in earlier loaded files. This can be leveraged
 * to deliberately override certain bean definitions via an extra Configuration class.
 *
 * @author Phillip Webb
 * @see #register(Class...)
 * @see #scan(String...)
 * @see ServletWebServerApplicationContext
 * @see AnnotationConfigServletWebApplicationContext
 * @since 4.0
 */
public class AnnotationConfigServletWebServerApplicationContext extends ServletWebServerApplicationContext
        implements AnnotationConfigRegistry {

  private final AnnotatedBeanDefinitionReader reader;

  private final ClassPathBeanDefinitionScanner scanner;

  private final Set<Class<?>> annotatedClasses = new LinkedHashSet<>();

  @Nullable
  private String[] basePackages;

  /**
   * Create a new {@link AnnotationConfigServletWebServerApplicationContext} that needs
   * to be populated through {@link #register} calls and then manually
   * {@linkplain #refresh refreshed}.
   */
  public AnnotationConfigServletWebServerApplicationContext() {
    this.reader = new AnnotatedBeanDefinitionReader(this);
    this.scanner = new ClassPathBeanDefinitionScanner(this);
  }

  /**
   * Create a new {@link AnnotationConfigServletWebServerApplicationContext} with the
   * given {@code StandardBeanFactory}. The context needs to be populated through
   * {@link #register} calls and then manually {@linkplain #refresh refreshed}.
   *
   * @param beanFactory the StandardBeanFactory instance to use for this context
   */
  public AnnotationConfigServletWebServerApplicationContext(StandardBeanFactory beanFactory) {
    super(beanFactory);
    this.reader = new AnnotatedBeanDefinitionReader(this);
    this.scanner = new ClassPathBeanDefinitionScanner(this);
  }

  /**
   * Create a new {@link AnnotationConfigServletWebServerApplicationContext}, deriving
   * bean definitions from the given annotated classes and automatically refreshing the
   * context.
   *
   * @param annotatedClasses one or more annotated classes, e.g. {@code @Configuration}
   * classes
   */
  public AnnotationConfigServletWebServerApplicationContext(Class<?>... annotatedClasses) {
    this();
    register(annotatedClasses);
    refresh();
  }

  /**
   * Create a new {@link AnnotationConfigServletWebServerApplicationContext}, scanning
   * for bean definitions in the given packages and automatically refreshing the
   * context.
   *
   * @param basePackages the packages to check for annotated classes
   */
  public AnnotationConfigServletWebServerApplicationContext(String... basePackages) {
    this();
    scan(basePackages);
    refresh();
  }

  /**
   * {@inheritDoc}
   * <p>
   * Delegates given environment to underlying {@link AnnotatedBeanDefinitionReader} and
   * {@link ClassPathBeanDefinitionScanner} members.
   */
  @Override
  public void setEnvironment(ConfigurableEnvironment environment) {
    super.setEnvironment(environment);
    this.reader.setEnvironment(environment);
    this.scanner.setEnvironment(environment);
  }

  /**
   * Provide a custom {@link BeanNameGenerator} for use with
   * {@link AnnotatedBeanDefinitionReader} and/or
   * {@link ClassPathBeanDefinitionScanner}, if any.
   * <p>
   * Default is
   * {@link AnnotationBeanNameGenerator}.
   * <p>
   * Any call to this method must occur prior to calls to {@link #register(Class...)}
   * and/or {@link #scan(String...)}.
   *
   * @param nameGenerator the bean name generator
   * @see AnnotatedBeanDefinitionReader#setBeanNameGenerator
   * @see ClassPathBeanDefinitionScanner#setBeanNameGenerator
   */
  public void setBeanNameGenerator(BeanNameGenerator nameGenerator) {
    this.reader.setBeanNameGenerator(nameGenerator);
    this.scanner.setBeanNameGenerator(nameGenerator);
    getBeanFactory().registerSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, nameGenerator);
  }

  /**
   * Set the {@link ScopeMetadataResolver} to use for detected bean classes.
   * <p>
   * The default is an {@link AnnotationScopeMetadataResolver}.
   * <p>
   * Any call to this method must occur prior to calls to {@link #register(Class...)}
   * and/or {@link #scan(String...)}.
   *
   * @param scopeMetadataResolver the scope metadata resolver
   */
  public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
    this.reader.setScopeMetadataResolver(scopeMetadataResolver);
    this.scanner.setScopeMetadataResolver(scopeMetadataResolver);
  }

  /**
   * Register one or more annotated classes to be processed. Note that
   * {@link #refresh()} must be called in order for the context to fully process the new
   * class.
   * <p>
   * Calls to {@code #register} are idempotent; adding the same annotated class more
   * than once has no additional effect.
   *
   * @param annotatedClasses one or more annotated classes, e.g. {@code @Configuration}
   * classes
   * @see #scan(String...)
   * @see #refresh()
   */
  @Override
  public final void register(Class<?>... annotatedClasses) {
    Assert.notEmpty(annotatedClasses, "At least one annotated class must be specified");
    this.annotatedClasses.addAll(Arrays.asList(annotatedClasses));
  }

  /**
   * Perform a scan within the specified base packages. Note that {@link #refresh()}
   * must be called in order for the context to fully process the new class.
   *
   * @param basePackages the packages to check for annotated classes
   * @see #register(Class...)
   * @see #refresh()
   */
  @Override
  public final void scan(String... basePackages) {
    Assert.notEmpty(basePackages, "At least one base package must be specified");
    this.basePackages = basePackages;
  }

  @Override
  protected void prepareRefresh() {
    this.scanner.clearCache();
    super.prepareRefresh();
  }

  @Override
  protected void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {
    super.postProcessBeanFactory(beanFactory);
    if (this.basePackages != null && this.basePackages.length > 0) {
      this.scanner.scan(this.basePackages);
    }
    if (!this.annotatedClasses.isEmpty()) {
      this.reader.register(ClassUtils.toClassArray(this.annotatedClasses));
    }
  }

}
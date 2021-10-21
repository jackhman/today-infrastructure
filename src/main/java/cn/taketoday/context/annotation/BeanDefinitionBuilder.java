/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.DefaultBeanDefinition;
import cn.taketoday.beans.factory.FactoryMethodBeanDefinition;
import cn.taketoday.beans.factory.PropertySetter;
import cn.taketoday.beans.factory.Scope;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.loader.AutowiredPropertyResolver;
import cn.taketoday.context.loader.PropertyResolvingContext;
import cn.taketoday.context.loader.PropertyValueResolverComposite;
import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Autowired;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.NonNull;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

import static cn.taketoday.lang.Constant.VALUE;

/**
 * @author TODAY 2021/10/2 22:45
 * @since 4.0
 */
public class BeanDefinitionBuilder {
  public static final Class<? extends Annotation>
          PostConstruct = ClassUtils.load("javax.annotation.PostConstruct");

  @Nullable
  private ApplicationContext context;

  @Nullable
  private PropsReader propsReader;

  @Nullable
  private PropertyResolvingContext resolvingContext;

  @Nullable
  private PropertyValueResolverComposite resolvingStrategies;

  /** bean name. */
  private String name;
  /** bean class. */
  private Class<?> beanClass;
  /** bean scope. */
  private String scope;

  /**
   * Invoke before {@link cn.taketoday.beans.InitializingBean#afterPropertiesSet}
   *
   * @since 2.3.3
   */
  private String[] initMethods = Constant.EMPTY_STRING_ARRAY;

  /**
   * @since 2.3.3
   */
  private String[] destroyMethods = Constant.EMPTY_STRING_ARRAY;

  /**
   * Mark as a {@link cn.taketoday.beans.FactoryBean}.
   *
   * @since 2.0.0
   */
  private boolean factoryBean = false;

  /** Child implementation */
  private BeanDefinition childDef;

  /** lazy init flag @since 3.0 */
  private boolean lazyInit;

  /** @since 3.0 bean instance supplier */
  private Supplier<?> instanceSupplier;

  /** @since 4.0 */
  private boolean synthetic = false;

  /** @since 4.0 */
  private int role = BeanDefinition.ROLE_APPLICATION;

  /** @since 4.0 */
  private boolean primary = false;

  private Method factoryMethod;
  /** Declaring name @since 2.1.2 */
  private String declaringName;

  private boolean resolveProps = true;

  private boolean resolveInitMethods = true;

  private boolean autowire = true;

  public BeanDefinitionBuilder() { }

  public BeanDefinitionBuilder(@Nullable ApplicationContext context) {
    this.context = context;
  }

  public BeanDefinitionBuilder name(String name) {
    this.name = name;
    return this;
  }

  public BeanDefinitionBuilder instanceSupplier(Supplier<?> instanceSupplier) {
    this.instanceSupplier = instanceSupplier;
    return this;
  }

  public BeanDefinitionBuilder childDefinition(BeanDefinition childDef) {
    this.childDef = childDef;
    return this;
  }

  public BeanDefinitionBuilder factoryBean(boolean factoryBean) {
    this.factoryBean = factoryBean;
    return this;
  }

  public BeanDefinitionBuilder lazyInit(boolean lazyInit) {
    this.lazyInit = lazyInit;
    return this;
  }

  public BeanDefinitionBuilder synthetic(boolean synthetic) {
    this.synthetic = synthetic;
    return this;
  }

  public BeanDefinitionBuilder role(int role) {
    this.role = role;
    return this;
  }

  public BeanDefinitionBuilder primary(boolean primary) {
    this.primary = primary;
    return this;
  }

  public BeanDefinitionBuilder beanClass(Class<?> beanClass) {
    this.beanClass = beanClass;
    return this;
  }

  public BeanDefinitionBuilder declaringName(String declaringName) {
    this.declaringName = declaringName;
    return this;
  }

  public BeanDefinitionBuilder factoryMethod(Method factoryMethod) {
    this.factoryMethod = factoryMethod;
    return this;
  }

  public BeanDefinitionBuilder scope(String scope) {
    this.scope = scope;
    return this;
  }

  /**
   * set scope 'singleton'
   *
   * @return this
   * @see Scope#SINGLETON
   */
  public BeanDefinitionBuilder singleton() {
    this.scope = Scope.SINGLETON;
    return this;
  }

  /**
   * set scope 'prototype'
   *
   * @return this
   * @see Scope#PROTOTYPE
   */
  public BeanDefinitionBuilder prototype() {
    this.scope = Scope.PROTOTYPE;
    return this;
  }

  public BeanDefinitionBuilder initMethods(String... initMethods) {
    this.initMethods = initMethods;
    return this;
  }

  public BeanDefinitionBuilder destroyMethods(String... destroyMethods) {
    this.destroyMethods = destroyMethods;
    return this;
  }

  /**
   * Enable resolving {@link Props}?
   *
   * @param resolveProps resolve {@link Props}
   * @return this
   * @see Props
   * @see PropsReader
   */
  public BeanDefinitionBuilder resolveProps(boolean resolveProps) {
    this.resolveProps = resolveProps;
    return this;
  }

  /**
   * Enable resolving init-method
   *
   * @param computeInitMethod compute InitMethod
   * @return this
   * @see BeanDefinition#getInitMethods()
   * @see #computeInitMethod(Class, String...)
   */
  public BeanDefinitionBuilder resolveInitMethods(boolean computeInitMethod) {
    this.resolveInitMethods = computeInitMethod;
    return this;
  }

  /**
   * Enable resolving autowire property-values
   *
   * @param autowire resolve {@link PropertySetter} ?
   * @return this
   */
  public BeanDefinitionBuilder resolvePropertyValues(boolean autowire) {
    this.autowire = autowire;
    return this;
  }

  //

  /**
   * apply scope,initMethods,destroyMethods
   *
   * @param component AnnotationAttributes
   * @see #scope(String)
   * @see #initMethods(String...)
   * @see #destroyMethods(String...)
   */
  public void attributes(AnnotationAttributes component) {
    if (CollectionUtils.isNotEmpty(component)) {
      this.scope = component.getString(BeanDefinition.SCOPE);
      this.initMethods = component.getStringArray(BeanDefinition.INIT_METHODS);
      this.destroyMethods = component.getStringArray(BeanDefinition.DESTROY_METHODS);
    }
  }

  public BeanDefinitionBuilder context(ApplicationContext context) {
    this.context = context;
    return this;
  }

  public BeanDefinitionBuilder propsReader(PropsReader propsReader) {
    this.propsReader = propsReader;
    return this;
  }

  public BeanDefinitionBuilder resolving(PropertyResolvingContext resolvingContext) {
    this.resolvingContext = resolvingContext;
    return this;
  }

  public BeanDefinitionBuilder resolvingStrategies(PropertyValueResolverComposite resolvingStrategies) {
    this.resolvingStrategies = resolvingStrategies;
    return this;
  }

  // reset

  public void reset() {
    this.role = BeanDefinition.ROLE_APPLICATION;
    this.initMethods = Constant.EMPTY_STRING_ARRAY;
    this.destroyMethods = Constant.EMPTY_STRING_ARRAY;

    this.name = null;
    this.scope = null;
    this.beanClass = null;
    this.childDef = null;
    this.lazyInit = false;
    this.declaringName = null;
    this.factoryMethod = null;
    this.instanceSupplier = null;

    this.primary = false;
    this.synthetic = false;
    this.factoryBean = false;

    this.resolveProps = false;
    this.resolveInitMethods = true;
    this.autowire = true;

  }

  public void resetAttributes() {
    this.scope = null;
    this.initMethods = Constant.EMPTY_STRING_ARRAY;
    this.destroyMethods = Constant.EMPTY_STRING_ARRAY;
  }

  // getter

  @Nullable
  public ApplicationContext getContext() {
    return context;
  }

  @Nullable
  public PropsReader getPropsReader() {
    return propsReader;
  }

  @Nullable
  public PropertyResolvingContext getResolvingContext() {
    return resolvingContext;
  }

  @Nullable
  public PropertyValueResolverComposite getResolvingStrategies() {
    return resolvingStrategies;
  }

  //---------------------------------------------------------------------
  // build
  //---------------------------------------------------------------------

  @NonNull
  private DefaultBeanDefinition create() {
    if (factoryMethod != null) {
      FactoryMethodBeanDefinition factoryMethodDef = new FactoryMethodBeanDefinition(factoryMethod);
      factoryMethodDef.setName(name);
      factoryMethodDef.setDeclaringName(declaringName);
      if (beanClass != null) {
        factoryMethodDef.setBeanClass(beanClass);
      }
      return factoryMethodDef;
    }
    return new DefaultBeanDefinition(name, beanClass);
  }

  public BeanDefinition build() {
    DefaultBeanDefinition definition = create();

    if (resolveInitMethods) {
      Method[] initMethod = computeInitMethod(initMethods, definition.getBeanClass());
      definition.setInitMethods(initMethod);
    }

    definition.setRole(role);
    definition.setScope(scope);
    definition.setChild(childDef);
    definition.setPrimary(primary);
    definition.setLazyInit(lazyInit);
    definition.setSynthetic(synthetic);
    definition.setFactoryBean(factoryBean);
    definition.setSupplier(instanceSupplier);
    definition.setDestroyMethods(destroyMethods);

    LinkedHashSet<PropertySetter> propertySetters = null;

    if (autowire) {
      Assert.state(beanClass != null, "bean-class must not be null");
      propertySetters = resolvePropertyValue(beanClass);
    }

    if (resolveProps) {
      if (propertySetters == null) {
        propertySetters = new LinkedHashSet<>();
      }
      // fix missing @Props injection
      List<PropertySetter> resolvedProps = propsReader().read(definition);
      propertySetters.addAll(resolvedProps);
    }

    if (propertySetters != null) {
      definition.setPropertyValues(propertySetters);
    }

    return definition;
  }

  // BiConsumer

  public void build(
          String defaultName,
          AnnotationAttributes component,
          BiConsumer<AnnotationAttributes, BeanDefinition> consumer) {
    build(defaultName, new AnnotationAttributes[] { component }, consumer);
  }

  public void build(
          String defaultName,
          BiConsumer<AnnotationAttributes, BeanDefinition> consumer,
          AnnotationAttributes... components) {
    build(defaultName, components, consumer);
  }

  public void build(
          String defaultName, @Nullable AnnotationAttributes[] components,
          BiConsumer<AnnotationAttributes, BeanDefinition> consumer) {
    if (ObjectUtils.isEmpty(components)) {
      name(defaultName);
      BeanDefinition definition = build();
      consumer.accept(null, definition);
    }
    else {
      for (AnnotationAttributes component : components) {
        attributes(component);
        for (String name : determineName(defaultName, component.getStringArray(VALUE))) {
          name(name);
          BeanDefinition definition = build();
          consumer.accept(component, definition);
        }
      }
    }
  }

  //

  public void build(
          String defaultName, AnnotatedElement annotated,
          BiConsumer<AnnotationAttributes, BeanDefinition> consumer) {
    AnnotationAttributes[] components = AnnotationUtils.getAttributesArray(annotated, Component.class);
    build(defaultName, components, consumer);
  }

  // Consumer

  public void build(
          String defaultName,
          AnnotationAttributes component,
          Consumer<BeanDefinition> consumer) {
    build(defaultName, new AnnotationAttributes[] { component }, consumer);
  }

  public void build(
          String defaultName,
          Consumer<BeanDefinition> consumer,
          AnnotationAttributes... components) {
    build(defaultName, components, consumer);
  }

  public void build(
          String defaultName,
          @Nullable AnnotationAttributes[] components,
          Consumer<BeanDefinition> consumer) {
    build(defaultName, components, (attributes, definition) -> consumer.accept(definition));
  }

  public void build(
          String defaultName, AnnotatedElement annotated, Consumer<BeanDefinition> consumer) {
    AnnotationAttributes[] components = AnnotationUtils.getAttributesArray(annotated, Component.class);
    build(defaultName, components, consumer);
  }

  //---------------------------------------------------------------------
  // PropertyValue (PropertySetter) resolving @since 3.0
  //---------------------------------------------------------------------

  /**
   * Process bean's property (field)
   *
   * @param beanClass Bean class
   * @since 3.0
   */
  public LinkedHashSet<PropertySetter> resolvePropertyValue(Class<?> beanClass) {
    LinkedHashSet<PropertySetter> propertySetters = new LinkedHashSet<>(32);
    ReflectionUtils.doWithFields(beanClass, field -> {
      // if property is required and PropertyValue is null will throw ex in PropertyValueResolver
      PropertySetter created = resolveProperty(field);
      // not required
      if (created != null) {
        propertySetters.add(created);
      }
    });

    return propertySetters;
  }

  /**
   * Create property value
   *
   * @param field Property
   * @return A new {@link PropertySetter}
   */
  @Nullable
  public PropertySetter resolveProperty(Field field) {
    if (resolvingStrategies == null) {
      resolvingStrategies = new PropertyValueResolverComposite();
    }
    if (resolvingContext == null) {
      resolvingContext = new PropertyResolvingContext(context, propsReader());
    }
    return resolvingStrategies.resolveProperty(resolvingContext, field);
  }

  public PropsReader propsReader() {
    if (propsReader == null) {
      Assert.state(context != null, "No Application Context");
      propsReader = new PropsReader(context.getEnvironment());
    }
    return propsReader;
  }

  //---------------------------------------------------------------------
  // static utils
  //---------------------------------------------------------------------

  /**
   * Find bean names
   *
   * @param defaultName Default bean name
   * @param names Annotation values
   * @return Bean names
   */
  public static String[] determineName(String defaultName, String... names) {
    if (ObjectUtils.isEmpty(names)) {
      return new String[] { defaultName }; // default name
    }
    HashSet<String> hashSet = new HashSet<>();
    CollectionUtils.addAll(hashSet, names);
    hashSet.remove(Constant.BLANK);
    if (hashSet.isEmpty()) {
      return new String[] { defaultName }; // default name
    }
    return names;
  }

  /**
   * @param beanClass Bean class
   * @param initMethods Init Method s
   * @since 2.1.2
   */
  public static Method[] computeInitMethod(Class<?> beanClass, String... initMethods) {
    return computeInitMethod(initMethods, beanClass);
  }

  /**
   * Add a method which annotated with {@link javax.annotation.PostConstruct}
   * or {@link  Autowired}
   *
   * @param beanClass Bean class
   * @param initMethods Init Method name
   * @see AutowiredPropertyResolver#isInjectable(AnnotatedElement)
   * @since 2.1.7
   */
  public static Method[] computeInitMethod(@Nullable String[] initMethods, Class<?> beanClass) {
    ArrayList<Method> methods = new ArrayList<>(2);
    boolean initMethodsNotEmpty = ObjectUtils.isNotEmpty(initMethods);
    // @since 4.0 use ReflectionUtils.doWithMethods
    ReflectionUtils.doWithMethods(beanClass, method -> {
      if (AnnotationUtils.isPresent(method, PostConstruct)
              || AutowiredPropertyResolver.isInjectable(method)) { // method Injection
        methods.add(method);
      }
      else if (initMethodsNotEmpty) {
        String name = method.getName();
        for (String initMethod : initMethods) {
          if (initMethod.equals(name)) { // equals
            methods.add(method);
          }
        }
      }
    });

    if (methods.isEmpty()) {
      return BeanDefinition.EMPTY_METHOD;
    }
    AnnotationAwareOrderComparator.sort(methods);
    return methods.toArray(new Method[methods.size()]);
  }

  public static DefaultBeanDefinition empty() {
    return new DefaultBeanDefinition();
  }

  public static DefaultBeanDefinition defaults(Class<?> candidate) {
    Assert.notNull(candidate, "bean-class must not be null");
    String defaultBeanName = defaultBeanName(candidate);
    return defaults(defaultBeanName, candidate, null);
  }

  public static DefaultBeanDefinition defaults(String name, Class<?> beanClass) {
    return defaults(name, beanClass, null);
  }

  public static DefaultBeanDefinition defaults(
          String name, Class<?> beanClass, @Nullable AnnotationAttributes attributes) {
    Assert.notNull(name, "bean-name must not be null");
    Assert.notNull(beanClass, "bean-class must not be null");

    DefaultBeanDefinition def = new DefaultBeanDefinition(name, beanClass);
    if (attributes == null) {
      def.setDestroyMethods(Constant.EMPTY_STRING_ARRAY);
      def.setInitMethods(computeInitMethod(null, beanClass));
    }
    else {
      def.setScope(attributes.getString(BeanDefinition.SCOPE));
      def.setDestroyMethods(attributes.getStringArray(BeanDefinition.DESTROY_METHODS));
      def.setInitMethods(computeInitMethod(attributes.getStringArray(BeanDefinition.INIT_METHODS), beanClass));
    }
    return def;
  }

  public static List<BeanDefinition> from(Class<?> candidate) {
    Assert.notNull(candidate, "bean-class must not be null");
    String defaultBeanName = defaultBeanName(candidate);

    AnnotationAttributes[] annotationAttributes =
            AnnotationUtils.getAttributesArray(candidate, Component.class);
    // has Component
    if (ObjectUtils.isNotEmpty(annotationAttributes)) {
      ArrayList<BeanDefinition> definitions = new ArrayList<>(2);
      for (AnnotationAttributes attributes : annotationAttributes) {
        String[] determineName = BeanDefinitionBuilder.determineName(
                defaultBeanName, attributes.getStringArray(Constant.VALUE));
        for (String beanName : determineName) {
          BeanDefinition defaults = defaults(beanName, candidate, attributes);
          definitions.add(defaults);
        }
      }
      return definitions;
    }
    else {
      BeanDefinition defaults = defaults(defaultBeanName, candidate, null);
      return Collections.singletonList(defaults);
    }
  }

  public static String defaultBeanName(String className) {
    return StringUtils.uncapitalize(ClassUtils.getSimpleName(className));
  }

  public static String defaultBeanName(Class<?> clazz) {
    String simpleName = clazz.getSimpleName();
    return StringUtils.uncapitalize(simpleName);
  }

}

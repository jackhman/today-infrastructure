/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;

import cn.taketoday.beans.BeanInstantiationException;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.factory.Aware;
import cn.taketoday.beans.factory.BeanClassLoaderAware;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.DependencyInjector;
import cn.taketoday.context.BootstrapContext;
import cn.taketoday.context.BootstrapContextAware;
import cn.taketoday.context.EnvironmentAware;
import cn.taketoday.context.ResourceLoaderAware;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.io.PatternResourceLoader;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ReflectionUtils;

/**
 * Common delegate code for the handling of parser strategies, e.g.
 * {@code TypeFilter}, {@code ImportSelector}, {@code ImportBeanDefinitionRegistrar}
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class ParserStrategyUtils {

  /**
   * Instantiate a class using an appropriate constructor and return the new
   * instance as the specified assignable type. The returned instance will
   * have {@link BeanClassLoaderAware}, {@link BeanFactoryAware},
   * {@link EnvironmentAware}, and {@link ResourceLoaderAware} contracts
   * invoked if they are implemented by the given object.
   */
  @SuppressWarnings("unchecked")
  static <T> T newInstance(Class<?> clazz, Class<T> assignableTo, BootstrapContext context) {
    Assert.notNull(clazz, "Class must not be null");
    Assert.isAssignable(assignableTo, clazz);
    if (clazz.isInterface()) {
      throw new BeanInstantiationException(clazz, "Specified class is an interface");
    }
    BeanDefinitionRegistry registry = context.getRegistry();
    PatternResourceLoader resourceLoader = context.getResourceLoader();
    Environment environment = context.getEnvironment();
    ClassLoader classLoader = registry instanceof ConfigurableBeanFactory cbf
                              ? cbf.getBeanClassLoader()
                              : resourceLoader.getClassLoader();
    T instance = (T) createInstance(clazz, environment, resourceLoader, registry, classLoader);
    ParserStrategyUtils.invokeAwareMethods(instance, environment,
            resourceLoader, registry, classLoader, context);
    return instance;
  }

  private static Object createInstance(Class<?> clazz, Environment environment,
          ResourceLoader resourceLoader, BeanDefinitionRegistry registry,
          @Nullable ClassLoader classLoader) {

    Constructor<?>[] constructors = clazz.getDeclaredConstructors();
    if (constructors.length == 1 && constructors[0].getParameterCount() > 0) {
      try {
        Constructor<?> constructor = constructors[0];
        Object[] args = resolveArgs(
                constructor, environment, resourceLoader, registry, classLoader);
        ReflectionUtils.makeAccessible(constructor);
        return BeanUtils.newInstance(constructor, args);
      }
      catch (BeanInstantiationException e) {
        throw e;
      }
      catch (Exception ex) {
        throw new BeanInstantiationException(clazz, "No suitable constructor found", ex);
      }
    }
    return BeanUtils.newInstance(clazz);
  }

  private static Object[] resolveArgs(Constructor<?> constructor,
          Environment environment, ResourceLoader resourceLoader,
          BeanDefinitionRegistry registry, @Nullable ClassLoader classLoader) {
    int i = 0;
    Parameter[] parameters = constructor.getParameters();
    Object[] args = new Object[parameters.length];
    Object[] providedArgs = new Object[] {
            classLoader, environment, resourceLoader, registry
    };
    for (Parameter parameter : parameters) {
      Object arg = DependencyInjector.findProvided(parameter, providedArgs);
      if (arg == null) {
        throw new IllegalStateException(
                "Illegal method parameter type: " + parameter.getType().getName());
      }
      args[i++] = arg;
    }
    return args;
  }

  private static void invokeAwareMethods(Object parserStrategyBean, Environment environment,
          ResourceLoader resourceLoader, BeanDefinitionRegistry registry,
          @Nullable ClassLoader classLoader, BootstrapContext loadingContext) {

    if (parserStrategyBean instanceof Aware) {
      if (parserStrategyBean instanceof BeanClassLoaderAware aware && classLoader != null) {
        aware.setBeanClassLoader(classLoader);
      }
      if (parserStrategyBean instanceof BeanFactoryAware aware && registry instanceof BeanFactory) {
        aware.setBeanFactory((BeanFactory) registry);
      }
      if (parserStrategyBean instanceof EnvironmentAware aware) {
        aware.setEnvironment(environment);
      }
      if (parserStrategyBean instanceof ResourceLoaderAware aware) {
        aware.setResourceLoader(resourceLoader);
      }
      if (parserStrategyBean instanceof BootstrapContextAware aware) {
        aware.setBootstrapContext(loadingContext);
      }
    }
  }

}

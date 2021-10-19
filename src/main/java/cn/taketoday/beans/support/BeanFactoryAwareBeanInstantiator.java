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

package cn.taketoday.beans.support;

import java.lang.reflect.Constructor;

import cn.taketoday.beans.ArgumentsResolver;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanInstantiationException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * provide Bean Constructor Arguments resolving
 *
 * @author TODAY 2021/10/4 22:26
 * @since 4.0
 */
public class BeanFactoryAwareBeanInstantiator {

  @Nullable
  private BeanFactory beanFactory;
  private ArgumentsResolver argumentsResolver;
  private BeanInstantiatorFactory instantiatorFactory = ReflectiveInstantiatorFactory.INSTANCE;

  public BeanFactoryAwareBeanInstantiator() { }

  public BeanFactoryAwareBeanInstantiator(@Nullable BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
    if (beanFactory != null) {
      this.argumentsResolver = beanFactory.getArgumentsResolver();
    }
  }

  public <T> T instantiate(Class<T> beanClass) {
    return instantiate(beanClass, null);
  }

  /**
   * use obtainConstructor to get {@link Constructor} to create bean instance.
   *
   * @param beanClass target bean class
   * @param providedArgs User provided arguments
   * @return bean class 's instance
   * @throws BeanInstantiationException if any reflective operation exception occurred
   * @see BeanUtils#obtainConstructor(Class)
   * @since 4.0
   */
  @SuppressWarnings("unchecked")
  public <T> T instantiate(Class<T> beanClass, @Nullable Object[] providedArgs) {
    Constructor<T> constructor = BeanUtils.obtainConstructor(beanClass);
    if (constructor.getParameterCount() == 0) {
      return (T) instantiatorFactory.newInstantiator(constructor).instantiate();
    }
    if (argumentsResolver == null) {
      argumentsResolver = ArgumentsResolver.getSharedInstance();
    }
    Object[] args = argumentsResolver.resolve(constructor, beanFactory, providedArgs);
    BeanInstantiator beanInstantiator = instantiatorFactory.newInstantiator(constructor);
    return (T) beanInstantiator.instantiate(args);
  }

  @SuppressWarnings("unchecked")
  public <T> T instantiate(
          Class<T> beanClass,
          ArgumentsResolver argumentsResolver,
          BeanInstantiatorFactory instantiatorFactory, @Nullable Object[] providedArgs) {
    Constructor<T> constructor = BeanUtils.obtainConstructor(beanClass);
    Object[] args = argumentsResolver.resolve(constructor, beanFactory, providedArgs);

    BeanInstantiator beanInstantiator = instantiatorFactory.newInstantiator(constructor);
    return (T) beanInstantiator.instantiate(args);
  }

  public void setInstantiatorFactory(@Nullable BeanInstantiatorFactory instantiatorFactory) {
    if (instantiatorFactory == null) {
      instantiatorFactory = ReflectiveInstantiatorFactory.INSTANCE;
    }
    this.instantiatorFactory = instantiatorFactory;
  }

  @Nullable
  public BeanInstantiatorFactory getInstantiatorFactory() {
    return instantiatorFactory;
  }

  public void setBeanFactory(@Nullable BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Nullable
  public BeanFactory getBeanFactory() {
    return beanFactory;
  }

  public void setArgumentsResolver(ArgumentsResolver argumentsResolver) {
    Assert.notNull(argumentsResolver, "ArgumentsResolver must not be null");
    this.argumentsResolver = argumentsResolver;
  }

  public ArgumentsResolver getArgumentsResolver() {
    return argumentsResolver;
  }

}

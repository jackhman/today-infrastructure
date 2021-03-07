/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.context.loader;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.function.Supplier;

import cn.taketoday.context.Ordered;
import cn.taketoday.context.OrderedSupport;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.factory.ObjectSupplier;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ObjectUtils;

/**
 * for {@link ObjectSupplier} ExecutableParameterResolver
 *
 * @author TODAY 2021/3/6 12:06
 */
public class ObjectSupplierParameterResolver
        extends OrderedSupport implements ExecutableParameterResolver, Ordered {

  public ObjectSupplierParameterResolver() {
    this(Integer.MAX_VALUE);
  }

  public ObjectSupplierParameterResolver(int order) {
    super(order);
  }

  @Override
  public boolean supports(Parameter parameter) {
    final Class<?> type = parameter.getType();
    return type == ObjectSupplier.class || type == Supplier.class;
  }

  @Override
  public ObjectSupplier<?> resolve(Parameter parameter, BeanFactory beanFactory) {
    final Type[] generics = ClassUtils.getGenerics(parameter);
    if (ObjectUtils.isNotEmpty(generics)) {
      final Type generic = generics[0];
      if (generic instanceof Class) {
        final Class<?> target = (Class<?>) generic;
        return beanFactory.getBeanSupplier(target);
      }
    }

    throw new UnsupportedOperationException("Unsupported " + parameter);
  }

}

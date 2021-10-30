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

package cn.taketoday.beans.factory;

import java.util.function.Supplier;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.support.BeanInstantiator;

/**
 * @author TODAY 2021/10/19 20:58
 * @since 4.0
 */
public class DefaultInstantiationStrategy implements InstantiationStrategy {

  @Override
  public Object instantiate(BeanDefinition def, BeanFactory owner) throws BeansException {
    Supplier<?> instanceSupplier = def.getInstanceSupplier();
    if (instanceSupplier != null) {
      return instanceSupplier.get();
    }
    BeanInstantiator instantiator = BeanInstantiator.fromClass(def.getBeanClass());


    return def.newInstance(owner);
  }

  @Override
  public Object instantiate(BeanDefinition def, BeanFactory owner, Object... args) throws BeansException {
    return def.newInstance(owner, args);
  }

}

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

package cn.taketoday.aop.config;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.parsing.AbstractComponentDefinition;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * {@link cn.taketoday.beans.factory.parsing.ComponentDefinition}
 * implementation that holds a pointcut definition.
 *
 * @author Rob Harrop
 * @since 4.0
 */
public class PointcutComponentDefinition extends AbstractComponentDefinition {

  private final String pointcutBeanName;

  private final BeanDefinition pointcutDefinition;

  private final String description;

  public PointcutComponentDefinition(String pointcutBeanName, BeanDefinition pointcutDefinition, String expression) {
    Assert.notNull(pointcutBeanName, "Bean name must not be null");
    Assert.notNull(pointcutDefinition, "Pointcut definition must not be null");
    Assert.notNull(expression, "Expression must not be null");
    this.pointcutBeanName = pointcutBeanName;
    this.pointcutDefinition = pointcutDefinition;
    this.description = "Pointcut <name='" + pointcutBeanName + "', expression=[" + expression + "]>";
  }

  @Override
  public String getName() {
    return this.pointcutBeanName;
  }

  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public BeanDefinition[] getBeanDefinitions() {
    return new BeanDefinition[] { this.pointcutDefinition };
  }

  @Override
  @Nullable
  public Object getSource() {
    return this.pointcutDefinition.getSource();
  }

}

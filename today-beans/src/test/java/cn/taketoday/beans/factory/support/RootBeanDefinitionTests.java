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

package cn.taketoday.beans.factory.support;

import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import java.lang.reflect.Method;

import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/6/9 23:35
 */
class RootBeanDefinitionTests {

  @Test
  void setInstanceSetResolvedFactoryMethod() {
    InstanceSupplier<?> instanceSupplier = mock(InstanceSupplier.class);
    Method method = ReflectionUtils.findMethod(String.class, "toString");
    given(instanceSupplier.getFactoryMethod()).willReturn(method);
    RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
    beanDefinition.setInstanceSupplier(instanceSupplier);
    assertThat(beanDefinition.getResolvedFactoryMethod()).isEqualTo(method);
    verify(instanceSupplier).getFactoryMethod();
  }

  @Test
  void setInstanceDoesNotOverrideResolvedFactoryMethodWithNull() {
    InstanceSupplier<?> instanceSupplier = mock(InstanceSupplier.class);
    given(instanceSupplier.getFactoryMethod()).willReturn(null);
    Method method = ReflectionUtils.findMethod(String.class, "toString");
    RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
    beanDefinition.setResolvedFactoryMethod(method);
    beanDefinition.setInstanceSupplier(instanceSupplier);
    assertThat(beanDefinition.getResolvedFactoryMethod()).isEqualTo(method);
    verify(instanceSupplier).getFactoryMethod();
  }

}
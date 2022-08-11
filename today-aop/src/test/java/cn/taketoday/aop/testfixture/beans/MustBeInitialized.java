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

package cn.taketoday.aop.testfixture.beans;

import cn.taketoday.beans.factory.InitializingBean;

/**
 * Simple test of BeanFactory initialization
 *
 * @author Rod Johnson
 * @since 12.03.2003
 */
public class MustBeInitialized implements InitializingBean {

  private boolean inited;

  /**
   * @see InitializingBean#afterPropertiesSet()
   */
  @Override
  public void afterPropertiesSet() throws Exception {
    this.inited = true;
  }

  /**
   * Dummy business method that will fail unless the factory
   * managed the bean's lifecycle correctly
   */
  public void businessMethod() {
    if (!this.inited) {
      throw new RuntimeException("Factory didn't call afterPropertiesSet() on MustBeInitialized object");
    }
  }

}
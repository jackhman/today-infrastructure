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

package cn.taketoday.cache.interceptor;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.Pointcut;
import cn.taketoday.aop.support.AbstractBeanFactoryPointcutAdvisor;
import cn.taketoday.lang.Nullable;

/**
 * Advisor driven by a {@link CacheOperationSource}, used to include a
 * cache advice bean for methods that are cacheable.
 *
 * @author Costin Leau
 * @since 4.0
 */
@SuppressWarnings("serial")
public class BeanFactoryCacheOperationSourceAdvisor extends AbstractBeanFactoryPointcutAdvisor {

  @Nullable
  private CacheOperationSource cacheOperationSource;

  private final CacheOperationSourcePointcut pointcut = new CacheOperationSourcePointcut() {
    @Override
    @Nullable
    protected CacheOperationSource getCacheOperationSource() {
      return cacheOperationSource;
    }
  };

  /**
   * Set the cache operation attribute source which is used to find cache
   * attributes. This should usually be identical to the source reference
   * set on the cache interceptor itself.
   */
  public void setCacheOperationSource(CacheOperationSource cacheOperationSource) {
    this.cacheOperationSource = cacheOperationSource;
  }

  /**
   * Set the {@link ClassFilter} to use for this pointcut.
   * Default is {@link ClassFilter#TRUE}.
   */
  public void setClassFilter(ClassFilter classFilter) {
    this.pointcut.setClassFilter(classFilter);
  }

  @Override
  public Pointcut getPointcut() {
    return this.pointcut;
  }

}

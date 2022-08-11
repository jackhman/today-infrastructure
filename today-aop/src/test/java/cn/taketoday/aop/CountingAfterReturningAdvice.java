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

package cn.taketoday.aop;

import org.aopalliance.intercept.MethodInvocation;

/**
 * Simple before advice example that we can use for counting checks.
 *
 * @author Rod Johnson
 * @author TODAY 2021/4/11 17:55
 */
@SuppressWarnings("serial")
public class CountingAfterReturningAdvice extends MethodCounter implements AfterReturningAdvice {

  @Override
  public void afterReturning(Object returnValue, MethodInvocation invocation) throws Throwable {
    count(invocation.getMethod());
  }
}
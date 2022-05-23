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

package cn.taketoday.web.handler;

import cn.taketoday.core.PathMatcher;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.InterceptorChain;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.config.InterceptorRegistration;

/**
 * @author TODAY 2021/8/30 22:08
 * @since 4.0
 */
public final class RuntimeHandlerInterceptor
        extends InterceptorRegistration implements HandlerInterceptor {

  private PathMatcher pathMatcher;

  /**
   * Create an {@link InterceptorRegistration} instance.
   */
  public RuntimeHandlerInterceptor(HandlerInterceptor interceptor) {
    super(interceptor);
  }

  @Override
  public Object intercept(RequestContext request, InterceptorChain chain) throws Throwable {
    String requestPath = request.getRequestPath();
    if (matchInRuntime(requestPath, pathMatcher)) {
      return interceptor.intercept(request, chain);
    }
    return chain.proceed(request); // next in the chain
  }

}
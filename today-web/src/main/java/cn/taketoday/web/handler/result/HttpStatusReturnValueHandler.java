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
package cn.taketoday.web.handler.result;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.handler.result.HandlerMethodReturnValueHandler;

/**
 * @author TODAY 2020/12/23 20:12
 * @since 3.0
 */
public class HttpStatusReturnValueHandler implements HandlerMethodReturnValueHandler {

  @Override
  public boolean supportsHandlerMethod(final HandlerMethod handler) {
    return handler.isReturn(HttpStatus.class);
  }

  @Override
  public boolean supportsReturnValue(final Object returnValue) {
    return returnValue instanceof HttpStatus;
  }

  @Override
  public void handleReturnValue(
          RequestContext context, Object handler, final Object returnValue) {
    if (returnValue instanceof HttpStatus) {
      context.setStatus((HttpStatus) returnValue);
    }
  }

}
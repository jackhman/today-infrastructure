/**
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
package cn.taketoday.web.view;

import cn.taketoday.core.NonNull;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.HandlerMethod;
import cn.taketoday.web.http.ResponseEntity;

/**
 * Handle {@link ResponseEntity}
 *
 * @author TODAY 2020/12/7 22:46
 * @see ResponseEntity
 * @since 3.0
 */
public class ResponseEntityReturnValueHandler
        extends HandlerMethodReturnValueHandler implements ReturnValueHandler {

  private final ObjectReturnValueHandler returnValueHandler;

  public ResponseEntityReturnValueHandler(ObjectReturnValueHandler returnValueHandler) {
    this.returnValueHandler = returnValueHandler;
  }

  @Override
  protected boolean supportsHandlerMethod(final HandlerMethod handler) {
    return handler.isReturn(ResponseEntity.class);
  }

  @Override
  protected void handleInternal(
          RequestContext context, HandlerMethod handler, @NonNull Object returnValue) throws Throwable {
    final ResponseEntity<?> response = (ResponseEntity<?>) returnValue;
    context.setStatus(response.getStatusCode());
    // apply headers
    context.responseHeaders().addAll(response.getHeaders());

    returnValueHandler.handleObjectValue(context, response.getBody());
  }
}

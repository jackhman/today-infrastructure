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

package cn.taketoday.web.socket.annotation;

import javax.websocket.server.ServerEndpointConfig;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.socket.StandardWebSocketHandler;

/**
 * @author TODAY 2021/5/8 22:18
 * @since 3.0.1
 */
public class StandardAnnotationWebSocketDispatcher
        extends AnnotationWebSocketDispatcher implements StandardWebSocketHandler {

  private ServerEndpointConfig endpointConfig;

  public StandardAnnotationWebSocketDispatcher(AnnotationWebSocketHandler socketHandler) {
    super(socketHandler);
  }

  @Override
  public ServerEndpointConfig getEndpointConfig(RequestContext context) {
    if (endpointConfig != null) {
      return endpointConfig;
    }
    return StandardWebSocketHandler.super.getEndpointConfig(context);
  }

  public void setEndpointConfig(ServerEndpointConfig endpointConfig) {
    this.endpointConfig = endpointConfig;
  }

  public ServerEndpointConfig getEndpointConfig() {
    return endpointConfig;
  }

}

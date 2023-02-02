/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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
package cn.taketoday.session;

import cn.taketoday.core.Conventions;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;

/**
 * Contract for session id resolution strategies. Allows for session id
 * resolution through the request and for sending the session id or expiring
 * the session through the response.
 *
 * @author TODAY
 * @since 2019-10-03 10:56
 */
public interface SessionIdResolver {
  String WRITTEN_SESSION_ID_ATTR = Conventions.getQualifiedAttributeName(
          CookieSessionIdResolver.class, "WRITTEN_SESSION_ID_ATTR");

  /**
   * Resolving session id from RequestContext
   * <p>
   * session id including {@link #setSessionId applied session id}
   *
   * @param context request context
   * @return session id
   */
  @Nullable
  String getSessionId(RequestContext context);

  /**
   * Send the given session id to the client.
   *
   * @param context the current context
   * @param sessionId the session id
   */
  void setSessionId(RequestContext context, String sessionId);

  /**
   * Instruct the client to end the current session.
   *
   * @param exchange the current exchange
   */
  void expireSession(RequestContext exchange);
}

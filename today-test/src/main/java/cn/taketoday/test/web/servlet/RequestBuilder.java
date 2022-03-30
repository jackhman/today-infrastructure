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

package cn.taketoday.test.web.servlet;

import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders;
import jakarta.servlet.ServletContext;

/**
 * Builds a {@link MockHttpServletRequest}.
 *
 * <p>See static factory methods in
 * {@link MockMvcRequestBuilders MockMvcRequestBuilders}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface RequestBuilder {

  /**
   * Build the request.
   *
   * @param servletContext the {@link ServletContext} to use to create the request
   * @return the request
   */
  MockHttpServletRequest buildRequest(ServletContext servletContext);

}

/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web.multipart.support;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.multipart.Multipart;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/13 11:06
 */
public abstract class AbstractMultipart implements Multipart {

  @Nullable
  protected HttpHeaders headers;

  @Override
  public HttpHeaders getHeaders() {
    HttpHeaders headers = this.headers;
    if (headers == null) {
      headers = createHttpHeaders();
      this.headers = headers;
    }
    return headers;
  }

  protected HttpHeaders createHttpHeaders() {
    return HttpHeaders.create();
  }

}

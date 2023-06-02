/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.annotation.config.ssl;

import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslBundleRegistry;

/**
 * Interface to be implemented by types that register {@link SslBundle} instances with an
 * {@link SslBundleRegistry}.
 *
 * @author Scott Frederick
 * @since 4.0
 */
@FunctionalInterface
public interface SslBundleRegistrar {

  /**
   * Callback method for registering {@link SslBundle}s with an
   * {@link SslBundleRegistry}.
   *
   * @param registry the registry that accepts {@code SslBundle}s
   */
  void registerBundles(SslBundleRegistry registry);

}

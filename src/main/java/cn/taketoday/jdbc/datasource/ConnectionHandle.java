/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.jdbc.datasource;

import java.sql.Connection;

/**
 * Simple interface to be implemented by handles for a JDBC Connection.
 * Used by JpaDialect, for example.
 *
 * @author Juergen Hoeller
 * @see SimpleConnectionHandle
 * @see ConnectionHolder
 * @since 1.1
 */
@FunctionalInterface
public interface ConnectionHandle {

  /**
   * Fetch the JDBC Connection that this handle refers to.
   */
  Connection getConnection();

  /**
   * Release the JDBC Connection that this handle refers to.
   * <p>The default implementation is empty, assuming that the lifecycle
   * of the connection is managed externally.
   *
   * @param con the JDBC Connection to release
   */
  default void releaseConnection(Connection con) {
  }

}

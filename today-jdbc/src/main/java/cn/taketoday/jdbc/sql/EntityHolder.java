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

package cn.taketoday.jdbc.sql;

import cn.taketoday.beans.BeanProperty;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 22:43
 */
public class EntityHolder {

  public final Class<?> entityClass;
  public final BeanProperty idProperty;
  public final String tableName;
  public final BeanProperty[] beanProperties;

  public final String[] columnNames;

  EntityHolder(Class<?> entityClass, BeanProperty idProperty, String tableName, BeanProperty[] beanProperties, String[] columnNames) {
    this.entityClass = entityClass;
    this.idProperty = idProperty;
    this.tableName = tableName;
    this.beanProperties = beanProperties;
    this.columnNames = columnNames;
  }

}

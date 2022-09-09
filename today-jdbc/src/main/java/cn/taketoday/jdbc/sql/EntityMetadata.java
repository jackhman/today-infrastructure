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

import java.util.Arrays;
import java.util.Objects;

import cn.taketoday.beans.BeanProperty;
import cn.taketoday.core.style.ToStringBuilder;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/16 22:43
 */
public class EntityMetadata {
  public final String tableName;

  public final Class<?> entityClass;
  public final String idColumnName;
  public final EntityProperty idProperty;
  public final BeanProperty[] beanProperties;

  public final String[] columnNames;
  public final EntityProperty[] entityProperties;

//  TODO public final boolean autoGenerateKeys ;

  EntityMetadata(Class<?> entityClass, String idColumnName, EntityProperty idProperty, String tableName,
          BeanProperty[] beanProperties, String[] columnNames, EntityProperty[] entityProperties) {
    this.entityClass = entityClass;
    this.idColumnName = idColumnName;
    this.idProperty = idProperty;
    this.tableName = tableName;
    this.beanProperties = beanProperties;
    this.columnNames = columnNames;
    this.entityProperties = entityProperties;
  }

  @Override
  public String toString() {
    return ToStringBuilder.from(this)
            .append("tableName", tableName)
            .append("columnNames", columnNames)
            .append("entityClass", entityClass)
            .append("idProperty", idProperty)
            .append("beanProperties", beanProperties)
            .append("entityProperties", entityProperties)
            .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof EntityMetadata that))
      return false;
    return Objects.equals(tableName, that.tableName)
            && Objects.equals(idProperty, that.idProperty)
            && Arrays.equals(columnNames, that.columnNames)
            && Objects.equals(entityClass, that.entityClass)
            && Arrays.equals(beanProperties, that.beanProperties)
            && Arrays.equals(entityProperties, that.entityProperties);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(tableName, entityClass, idProperty);
    result = 31 * result + Arrays.hashCode(beanProperties);
    result = 31 * result + Arrays.hashCode(columnNames);
    result = 31 * result + Arrays.hashCode(entityProperties);
    return result;
  }
}

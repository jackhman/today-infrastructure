/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.jdbc.persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import cn.taketoday.jdbc.persistence.sql.OrderByClause;
import cn.taketoday.jdbc.persistence.sql.Restriction;
import cn.taketoday.jdbc.persistence.sql.Select;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LogMessage;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 1.0 2024/2/20 17:03
 */
final class NoConditionsQuery extends AbstractColumnsQueryHandler implements ConditionHandler {

  static final NoConditionsQuery instance = new NoConditionsQuery();

  @Override
  protected void renderInternal(EntityMetadata metadata, Select select) {
    // noop
  }

  @Override
  public void setParameter(EntityMetadata metadata, PreparedStatement statement) throws SQLException {
    // noop
  }

  @Override
  public String getDescription() {
    return "Query entities without conditions";
  }

  @Override
  public Object getDebugLogMessage() {
    return LogMessage.format(getDescription());
  }

  @Override
  public void renderWhereClause(EntityMetadata metadata, List<Restriction> restrictions) {
    // noop
  }

  @Nullable
  @Override
  public OrderByClause getOrderByClause(EntityMetadata metadata) {
    return null;
  }

}

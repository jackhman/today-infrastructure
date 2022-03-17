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
package cn.taketoday.jdbc.type;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Clinton Begin
 */
public class FloatTypeHandler extends BaseTypeHandler<Float> {

  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Float parameter)
          throws SQLException {
    ps.setFloat(i, parameter);
  }

  @Override
  public Float getResult(ResultSet rs, String columnName)
          throws SQLException {
    float result = rs.getFloat(columnName);
    return result == 0 && rs.wasNull() ? null : result;
  }

  @Override
  public Float getResult(ResultSet rs, int columnIndex)
          throws SQLException {
    float result = rs.getFloat(columnIndex);
    return result == 0 && rs.wasNull() ? null : result;
  }

  @Override
  public Float getResult(CallableStatement cs, int columnIndex)
          throws SQLException {
    float result = cs.getFloat(columnIndex);
    return result == 0 && cs.wasNull() ? null : result;
  }
}
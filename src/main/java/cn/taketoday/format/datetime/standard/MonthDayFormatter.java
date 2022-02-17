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

package cn.taketoday.format.datetime.standard;

import java.text.ParseException;
import java.time.MonthDay;
import java.util.Locale;

import cn.taketoday.format.Formatter;

/**
 * {@link Formatter} implementation for a JSR-310 {@link MonthDay},
 * following JSR-310's parsing rules for a MonthDay.
 *
 * @author Juergen Hoeller
 * @see MonthDay#parse
 * @since 4.0
 */
class MonthDayFormatter implements Formatter<MonthDay> {

  @Override
  public MonthDay parse(String text, Locale locale) throws ParseException {
    return MonthDay.parse(text);
  }

  @Override
  public String print(MonthDay object, Locale locale) {
    return object.toString();
  }

}

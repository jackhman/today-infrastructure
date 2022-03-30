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

package cn.taketoday.test.util.subpackage;

/**
 * Interface representing a <em>person</em> entity; intended for use in unit tests.
 *
 * <p>The introduction of an interface is necessary in order to test support for
 * JDK dynamic proxies.
 *
 * @author Sam Brannen
 * @since 4.3
 */
public interface Person {

	long getId();

	String getName();

	int getAge();

	String getEyeColor();

	boolean likesPets();

	Number getFavoriteNumber();

}
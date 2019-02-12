/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context.bean;

import java.lang.reflect.Field;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Property
 * 
 * @author Today <br>
 *         2018-06-23 11:28:01
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PropertyValue {

	/** property value */
	private Object value;
	/** field info */
	private Field field;

	public PropertyValue(Field field) {
		this.field = field;
	}

	@Override
	public String toString() {
		return new StringBuilder()//
				.append("{\"value\":\"").append(value)//
				.append("\",\"field\":\"").append(field)//
				.append("\"}")//
				.toString();
	}

}

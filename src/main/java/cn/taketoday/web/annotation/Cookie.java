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
package cn.taketoday.web.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.web.Constant;

/**
 * 
 * @author Today <br>
 *         2018-07-01 14:10:04 <br>
 *         2018-08-21 19:16 <b>change</b> add defaultValue()
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@RequestParam(type = Constant.ANNOTATION_COOKIE)
public @interface Cookie {

	/**
	 * 
	 * required ?
	 * 
	 * @return
	 */
	boolean required() default false;

	/**
	 * The name of cookie.
	 * 
	 * @return
	 */
	String value() default Constant.BLANK;

	/**
	 * When required == false, and parameter == null. use default value.
	 * 
	 * @return
	 */
	String defaultValue() default Constant.BLANK;

}

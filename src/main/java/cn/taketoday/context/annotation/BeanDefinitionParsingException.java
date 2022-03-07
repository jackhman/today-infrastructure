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

package cn.taketoday.context.annotation;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.parsing.Problem;

/**
 * Exception thrown when a bean definition reader encounters an error
 * during the parsing process.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 4.0
 */
@SuppressWarnings("serial")
public class BeanDefinitionParsingException extends BeanDefinitionStoreException {

  /**
   * Create a new BeanDefinitionParsingException.
   *
   * @param problem the configuration problem that was detected during the parsing process
   */
  public BeanDefinitionParsingException(Problem problem) {
    super(problem.getResourceDescription(), problem.toString(), problem.getRootCause());
  }

}

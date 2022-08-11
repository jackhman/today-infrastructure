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

package cn.taketoday.web.testfixture.beans;

import java.io.IOException;

public interface ITestBean extends AgeHolder {

  String getName();

  void setName(String name);

  ITestBean getSpouse();

  void setSpouse(ITestBean spouse);

  ITestBean[] getSpouses();

  String[] getStringArray();

  void setStringArray(String[] stringArray);

  Integer[][] getNestedIntegerArray();

  Integer[] getSomeIntegerArray();

  void setSomeIntegerArray(Integer[] someIntegerArray);

  void setNestedIntegerArray(Integer[][] nestedIntegerArray);

  int[] getSomeIntArray();

  void setSomeIntArray(int[] someIntArray);

  int[][] getNestedIntArray();

  void setNestedIntArray(int[][] someNestedArray);

  /**
   * Throws a given (non-null) exception.
   */
  void exceptional(Throwable t) throws Throwable;

  Object returnsThis();

  /**
   * Increment the age by one.
   *
   * @return the previous age
   */
  int haveBirthday();

  void unreliableFileOperation() throws IOException;

}
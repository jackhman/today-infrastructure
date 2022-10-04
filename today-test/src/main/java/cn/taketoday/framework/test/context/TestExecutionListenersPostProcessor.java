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

package cn.taketoday.framework.test.context;

import java.util.List;

import cn.taketoday.test.context.TestExecutionListener;

/**
 * Callback interface trigger from {@link InfraApplicationTestContextBootstrapper} that can be
 * used to post-process the list of default {@link TestExecutionListener} classes to be
 * used by a test. Can be used to add or remove existing listener classes.
 *
 * @author Phillip Webb
 * @see ApplicationTest
 * @since 4.0
 */
@FunctionalInterface
public interface TestExecutionListenersPostProcessor {

  /**
   * Post process the list of default {@link TestExecutionListener} classes to be used.
   *
   * @param listeners the source listeners
   * @return the actual listeners that should be used
   */
  List<TestExecutionListener> postProcessListeners(List<TestExecutionListener> listeners);

}

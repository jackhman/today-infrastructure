/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.testfixture;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import cn.taketoday.util.StringUtils;

/**
 * A test group used to limit when certain tests are run.
 *
 * @author Phillip Webb
 * @author Chris Beams
 * @author Sam Brannen
 */
public enum TestGroup {

  /**
   * Tests that take a considerable amount of time to run. Any test lasting longer than
   * 500ms should be considered a candidate in order to avoid making the overall test
   * suite too slow to run during the normal development cycle.
   */
  LONG_RUNNING;

  /**
   * Determine if this {@link TestGroup} is active.
   *
   * @since 4.0
   */
  public boolean isActive() {
    return loadTestGroups().contains(this);
  }

  private static final String TEST_GROUPS_SYSTEM_PROPERTY = "testGroups";

  /**
   * Load test groups dynamically instead of during static initialization in
   * order to avoid a {@link NoClassDefFoundError} being thrown while attempting
   * to load collaborator classes.
   */
  static Set<TestGroup> loadTestGroups() {
    try {
      return TestGroup.parse(System.getProperty(TEST_GROUPS_SYSTEM_PROPERTY));
    }
    catch (Exception ex) {
      throw new IllegalStateException("Failed to parse '" + TEST_GROUPS_SYSTEM_PROPERTY +
              "' system property: " + ex.getMessage(), ex);
    }
  }

  /**
   * Parse the specified comma separated string of groups.
   *
   * @param value the comma separated string of groups
   * @return a set of groups
   * @throws IllegalArgumentException if any specified group name is not a
   * valid {@link TestGroup}
   */
  static Set<TestGroup> parse(String value) throws IllegalArgumentException {
    if (StringUtils.isBlank(value)) {
      return Collections.emptySet();
    }
    String originalValue = value;
    value = value.trim();
    if ("ALL".equalsIgnoreCase(value)) {
      return EnumSet.allOf(TestGroup.class);
    }
    if (value.toUpperCase().startsWith("ALL-")) {
      Set<TestGroup> groups = EnumSet.allOf(TestGroup.class);
      groups.removeAll(parseGroups(originalValue, value.substring(4)));
      return groups;
    }
    return parseGroups(originalValue, value);
  }

  private static Set<TestGroup> parseGroups(String originalValue, String value) throws IllegalArgumentException {
    Set<TestGroup> groups = new HashSet<>();
    for (String group : value.split(",")) {
      try {
        groups.add(valueOf(group.trim().toUpperCase()));
      }
      catch (IllegalArgumentException ex) {
        throw new IllegalArgumentException(String.format(
                "Unable to find test group '%s' when parsing testGroups value: '%s'. " +
                        "Available groups include: [%s]", group.trim(), originalValue,
                StringUtils.arrayToCommaDelimitedString(TestGroup.values())));
      }
    }
    return groups;
  }

}

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

package cn.taketoday.test.classpath;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ModifiedClassPathExtension} overriding entries on the class path.
 *
 * @author Christoph Dreis
 */
@ClassPathOverrides("cn.taketoday:today-context:4.0.0-Draft.1")
class ModifiedClassPathExtensionOverridesTests {

  @Test
  void classesAreLoadedFromOverride() {
    assertThat(ApplicationContext.class.getProtectionDomain().getCodeSource().getLocation().toString())
            .endsWith("today-context-4.0.0-Draft.1.jar");
  }

  @Test
  void classesAreLoadedFromTransitiveDependencyOfOverride() {
    assertThat(StringUtils.class.getProtectionDomain().getCodeSource().getLocation().toString())
            .endsWith("today-core-4.0.0-Draft.1.jar");
  }

}

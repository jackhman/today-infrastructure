/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.beans.factory.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.aot.hint.RuntimeHints;
import cn.taketoday.aot.hint.RuntimeHintsRegistrar;
import cn.taketoday.aot.hint.predicate.RuntimeHintsPredicates;
import cn.taketoday.beans.factory.aot.AotServices;
import cn.taketoday.util.ClassUtils;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/23 20:09
 */
class JakartaAnnotationsRuntimeHintsTests {

  private final RuntimeHints hints = new RuntimeHints();

  @BeforeEach
  void setup() {
    AotServices.factories().load(RuntimeHintsRegistrar.class)
            .forEach(registrar -> registrar.registerHints(this.hints,
                    ClassUtils.getDefaultClassLoader()));
  }

  @Test
  void jakartaInjectAnnotationHasHints() {
    assertThat(RuntimeHintsPredicates.reflection().onType(Inject.class)).accepts(this.hints);
  }

  @Test
  void jakartaQualifierAnnotationHasHints() {
    assertThat(RuntimeHintsPredicates.reflection().onType(Qualifier.class)).accepts(this.hints);
  }

}
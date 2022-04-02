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

package cn.taketoday.context.properties.bind;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.properties.bind.Bindable.BindRestriction;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.AnnotationUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Bindable}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class BindableTests {

  @Test
  void ofClassWhenTypeIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> Bindable.of((Class<?>) null))
            .withMessageContaining("Type must not be null");
  }

  @Test
  void ofTypeWhenTypeIsNullShouldThrowException() {
    assertThatIllegalArgumentException().isThrownBy(() -> Bindable.of((ResolvableType) null))
            .withMessageContaining("Type must not be null");
  }

  @Test
  void ofClassShouldSetType() {
    assertThat(Bindable.of(String.class).getType()).isEqualTo(ResolvableType.fromClass(String.class));
  }

  @Test
  void ofTypeShouldSetType() {
    ResolvableType type = ResolvableType.fromClass(String.class);
    assertThat(Bindable.of(type).getType()).isEqualTo(type);
  }

  @Test
  void ofInstanceShouldSetTypeAndExistingValue() {
    String instance = "foo";
    ResolvableType type = ResolvableType.fromClass(String.class);
    assertThat(Bindable.ofInstance(instance).getType()).isEqualTo(type);
    assertThat(Bindable.ofInstance(instance).getValue().get()).isEqualTo("foo");
  }

  @Test
  void ofClassWithExistingValueShouldSetTypeAndExistingValue() {
    assertThat(Bindable.of(String.class).withExistingValue("foo").getValue().get()).isEqualTo("foo");
  }

  @Test
  void ofTypeWithExistingValueShouldSetTypeAndExistingValue() {
    assertThat(Bindable.of(ResolvableType.fromClass(String.class)).withExistingValue("foo").getValue().get())
            .isEqualTo("foo");
  }

  @Test
  void ofTypeWhenExistingValueIsNotInstanceOfTypeShouldThrowException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> Bindable.of(ResolvableType.fromClass(String.class)).withExistingValue(123))
            .withMessageContaining("ExistingValue must be an instance of " + String.class.getName());
  }

  @Test
  void ofTypeWhenPrimitiveWithExistingValueWrapperShouldNotThrowException() {
    Bindable<Integer> bindable = Bindable.<Integer>of(ResolvableType.fromClass(int.class)).withExistingValue(123);
    assertThat(bindable.getType().resolve()).isEqualTo(int.class);
    assertThat(bindable.getValue().get()).isEqualTo(123);
  }

  @Test
  void getBoxedTypeWhenNotBoxedShouldReturnType() {
    Bindable<String> bindable = Bindable.of(String.class);
    assertThat(bindable.getBoxedType()).isEqualTo(ResolvableType.fromClass(String.class));
  }

  @Test
  void getBoxedTypeWhenPrimitiveShouldReturnBoxedType() {
    Bindable<Integer> bindable = Bindable.of(int.class);
    assertThat(bindable.getType()).isEqualTo(ResolvableType.fromClass(int.class));
    assertThat(bindable.getBoxedType()).isEqualTo(ResolvableType.fromClass(Integer.class));
  }

  @Test
  void getBoxedTypeWhenPrimitiveArrayShouldReturnBoxedType() {
    Bindable<int[]> bindable = Bindable.of(int[].class);
    assertThat(bindable.getType().getComponentType()).isEqualTo(ResolvableType.fromClass(int.class));
    assertThat(bindable.getBoxedType().isArray()).isTrue();
    assertThat(bindable.getBoxedType().getComponentType()).isEqualTo(ResolvableType.fromClass(Integer.class));
  }

  @Test
  void getAnnotationsShouldReturnEmptyArray() {
    assertThat(Bindable.of(String.class).getAnnotations()).isEmpty();
  }

  @Test
  void withAnnotationsShouldSetAnnotations() {
    Annotation annotation = mock(Annotation.class);
    assertThat(Bindable.of(String.class).withAnnotations(annotation).getAnnotations()).containsExactly(annotation);
  }

  @Test
  void getAnnotationWhenMatchShouldReturnAnnotation() {
    Test annotation = AnnotationUtils.synthesizeAnnotation(Test.class);
    assertThat(Bindable.of(String.class).withAnnotations(annotation).getAnnotation(Test.class))
            .isSameAs(annotation);
  }

  @Test
  void getAnnotationWhenNoMatchShouldReturnNull() {
    Test annotation = AnnotationUtils.synthesizeAnnotation(Test.class);
    assertThat(Bindable.of(String.class).withAnnotations(annotation).getAnnotation(Bean.class)).isNull();
  }

  @Test
  void toStringShouldShowDetails() {
    Annotation annotation = AnnotationUtils.synthesizeAnnotation(TestAnnotation.class);
    Bindable<String> bindable = Bindable.of(String.class).withExistingValue("foo").withAnnotations(annotation);
    assertThat(bindable.toString())
            .contains("type = java.lang.String, value = 'provided', annotations = array<Annotation>["
                    + "@cn.taketoday.context.properties.bind.BindableTests$TestAnnotation()]");
  }

  @Test
  void equalsAndHashCode() {
    Annotation annotation = AnnotationUtils.synthesizeAnnotation(TestAnnotation.class);
    Bindable<String> bindable1 = Bindable.of(String.class).withExistingValue("foo").withAnnotations(annotation);
    Bindable<String> bindable2 = Bindable.of(String.class).withExistingValue("foo").withAnnotations(annotation);
    Bindable<String> bindable3 = Bindable.of(String.class).withExistingValue("fof").withAnnotations(annotation);
    assertThat(bindable1.hashCode()).isEqualTo(bindable2.hashCode());
    assertThat(bindable1).isEqualTo(bindable1).isEqualTo(bindable2);
    assertThat(bindable1).isEqualTo(bindable3);
  }

  @Test
    // gh-18218
  void withExistingValueDoesNotForgetAnnotations() {
    Annotation annotation = AnnotationUtils.synthesizeAnnotation(TestAnnotation.class);
    Bindable<?> bindable = Bindable.of(String.class).withAnnotations(annotation).withExistingValue("");
    assertThat(bindable.getAnnotations()).containsExactly(annotation);
  }

  @Test
    // gh-18218
  void withSuppliedValueDoesNotForgetAnnotations() {
    Annotation annotation = AnnotationUtils.synthesizeAnnotation(TestAnnotation.class);
    Bindable<?> bindable = Bindable.of(String.class).withAnnotations(annotation).withSuppliedValue(() -> "");
    assertThat(bindable.getAnnotations()).containsExactly(annotation);
  }

  @Test
  void hasBindRestrictionWhenDefaultReturnsFalse() {
    Bindable<String> bindable = Bindable.of(String.class);
    for (BindRestriction bindRestriction : BindRestriction.values()) {
      assertThat(bindable.hasBindRestriction(bindRestriction)).isFalse();
    }
  }

  @Test
  void withBindRestrictionAddsBindRestriction() {
    Bindable<String> bindable = Bindable.of(String.class);
    Bindable<String> restricted = bindable.withBindRestrictions(BindRestriction.NO_DIRECT_PROPERTY);
    assertThat(bindable.hasBindRestriction(BindRestriction.NO_DIRECT_PROPERTY)).isFalse();
    assertThat(restricted.hasBindRestriction(BindRestriction.NO_DIRECT_PROPERTY)).isTrue();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface TestAnnotation {

  }

}

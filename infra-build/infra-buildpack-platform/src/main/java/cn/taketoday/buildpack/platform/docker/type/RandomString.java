/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.buildpack.platform.docker.type;

import java.util.Random;
import java.util.stream.IntStream;

import cn.taketoday.lang.Assert;

/**
 * Utility class used to generate random strings.
 *
 * @author Phillip Webb
 */
final class RandomString {

  private static final Random random = new Random();

  private RandomString() {
  }

  static String generate(String prefix, int randomLength) {
    Assert.notNull(prefix, "Prefix must not be null");
    return prefix + generateRandom(randomLength);
  }

  static CharSequence generateRandom(int length) {
    IntStream chars = random.ints('a', 'z' + 1).limit(length);
    return chars.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append);
  }

}
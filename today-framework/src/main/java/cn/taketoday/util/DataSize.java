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
package cn.taketoday.util;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * A data size, such as '12MB'.
 *
 * <p>This class models data size in terms of bytes and is immutable and thread-safe.
 *
 * <p>The terms and units used in this class are based on
 * <a href="https://en.wikipedia.org/wiki/Binary_prefix">binary prefixes</a>
 * indicating multiplication by powers of 2. Consult the following table and
 * the Javadoc for {@link DataUnit} for details.
 *
 * <p>
 * <table border="1">
 * <tr><th>Term</th><th>Data Size</th><th>Size in Bytes</th></tr>
 * <tr><td>byte</td><td>1B</td><td>1</td></tr>
 * <tr><td>kilobyte</td><td>1KB</td><td>1,024</td></tr>
 * <tr><td>megabyte</td><td>1MB</td><td>1,048,576</td></tr>
 * <tr><td>gigabyte</td><td>1GB</td><td>1,073,741,824</td></tr>
 * <tr><td>terabyte</td><td>1TB</td><td>1,099,511,627,776</td></tr>
 * </table>
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @author Stephane Nicoll
 * @author TODAY
 * @see DataUnit
 * @since 2.1.3
 */
public final class DataSize implements Comparable<DataSize>, Serializable {

  /**
   * The pattern for parsing.
   */
  private static final Pattern PATTERN = Pattern.compile("^([+\\-]?\\d+)([a-zA-Z]{0,2})$");

  /**
   * Bytes per Kilobyte.
   */
  public static final long BYTES_PER_KB = 1024;

  /**
   * Bytes per Megabyte.
   */
  public static final long BYTES_PER_MB = BYTES_PER_KB * 1024;

  /**
   * Bytes per Gigabyte.
   */
  public static final long BYTES_PER_GB = BYTES_PER_MB * 1024;

  /**
   * Bytes per Terabyte.
   */
  public static final long BYTES_PER_TB = BYTES_PER_GB * 1024;

  private final long bytes;

  private DataSize(long bytes) {
    this.bytes = bytes;
  }

  /**
   * Obtain a {@link DataSize} representing the specified number of bytes.
   *
   * @param bytes the number of bytes, positive or negative
   * @return a {@link DataSize}
   */
  public static DataSize ofBytes(long bytes) {
    return new DataSize(bytes);
  }

  /**
   * Obtain a {@link DataSize} representing the specified number of kilobytes.
   *
   * @param kilobytes the number of kilobytes, positive or negative
   * @return a {@link DataSize}
   */
  public static DataSize ofKilobytes(long kilobytes) {
    return new DataSize(Math.multiplyExact(kilobytes, BYTES_PER_KB));
  }

  /**
   * Obtain a {@link DataSize} representing the specified number of megabytes.
   *
   * @param megabytes the number of megabytes, positive or negative
   * @return a {@link DataSize}
   */
  public static DataSize ofMegabytes(long megabytes) {
    return new DataSize(Math.multiplyExact(megabytes, BYTES_PER_MB));
  }

  /**
   * Obtain a {@link DataSize} representing the specified number of gigabytes.
   *
   * @param gigabytes the number of gigabytes, positive or negative
   * @return a {@link DataSize}
   */
  public static DataSize ofGigabytes(long gigabytes) {
    return new DataSize(Math.multiplyExact(gigabytes, BYTES_PER_GB));
  }

  /**
   * Obtain a {@link DataSize} representing the specified number of terabytes.
   *
   * @param terabytes the number of terabytes, positive or negative
   * @return a {@link DataSize}
   */
  public static DataSize ofTerabytes(long terabytes) {
    return new DataSize(Math.multiplyExact(terabytes, BYTES_PER_TB));
  }

  /**
   * Obtain a {@link DataSize} representing an amount in the specified {@link DataUnit}.
   *
   * @param amount the amount of the size, measured in terms of the unit,
   * positive or negative
   * @return a corresponding {@link DataSize}
   */
  public static DataSize of(long amount, DataUnit unit) {
    Assert.notNull(unit, "Unit must not be null");
    return new DataSize(Math.multiplyExact(amount, unit.size().toBytes()));
  }

  /**
   * Obtain a {@link DataSize} from a text string such as {@code 12MB} using
   * {@link DataUnit#BYTES} if no unit is specified.
   * <p>
   * Examples:
   * <pre>
   * "12KB" -- parses as "12 kilobytes"
   * "5MB"  -- parses as "5 megabytes"
   * "20"   -- parses as "20 bytes"
   * </pre>
   *
   * @param text the text to parse
   * @return the parsed {@link DataSize}
   * @see #parse(CharSequence, DataUnit)
   */
  public static DataSize parse(CharSequence text) {
    return parse(text, null);
  }

  /**
   * Obtain a {@link DataSize} from a text string such as {@code 12MB} using
   * the specified default {@link DataUnit} if no unit is specified.
   * <p>
   * The string starts with a number followed optionally by a unit matching one of the
   * supported {@linkplain DataUnit suffixes}.
   * <p>
   * Examples:
   * <pre>
   * "12KB" -- parses as "12 kilobytes"
   * "5MB"  -- parses as "5 megabytes"
   * "20"   -- parses as "20 kilobytes" (where the {@code defaultUnit} is {@link DataUnit#KILOBYTES})
   * </pre>
   *
   * @param text the text to parse
   * @return the parsed {@link DataSize}
   */
  public static DataSize parse(CharSequence text, @Nullable DataUnit defaultUnit) {
    Assert.notNull(text, "Text must not be null");
    try {
      Matcher matcher = PATTERN.matcher(StringUtils.trimAllWhitespace(text));
      Assert.state(matcher.matches(), "Does not match data size pattern");
      DataUnit unit = determineDataUnit(matcher.group(2), defaultUnit);
      long amount = Long.parseLong(matcher.group(1));
      return DataSize.of(amount, unit);
    }
    catch (Exception ex) {
      throw new IllegalArgumentException("'" + text + "' is not a valid data size", ex);
    }
  }

  private static DataUnit determineDataUnit(String suffix, @Nullable DataUnit defaultUnit) {
    DataUnit defaultUnitToUse = (defaultUnit != null ? defaultUnit : DataUnit.BYTES);
    return StringUtils.isNotEmpty(suffix) ? DataUnit.fromSuffix(suffix) : defaultUnitToUse;
  }

  /**
   * Checks if this size is negative, excluding zero.
   *
   * @return true if this size has a size less than zero bytes
   */
  public boolean isNegative() {
    return this.bytes < 0;
  }

  /**
   * Return the number of bytes in this instance.
   *
   * @return the number of bytes
   */
  public long toBytes() {
    return this.bytes;
  }

  /**
   * Return the number of kilobytes in this instance.
   *
   * @return the number of kilobytes
   */
  public long toKilobytes() {
    return this.bytes / BYTES_PER_KB;
  }

  /**
   * Return the number of megabytes in this instance.
   *
   * @return the number of megabytes
   */
  public long toMegabytes() {
    return this.bytes / BYTES_PER_MB;
  }

  /**
   * Return the number of gigabytes in this instance.
   *
   * @return the number of gigabytes
   */
  public long toGigabytes() {
    return this.bytes / BYTES_PER_GB;
  }

  /**
   * Return the number of terabytes in this instance.
   *
   * @return the number of terabytes
   */
  public long toTerabytes() {
    return this.bytes / BYTES_PER_TB;
  }

  @Override
  public int compareTo(DataSize other) {
    return Long.compare(this.bytes, other.bytes);
  }

  @Override
  public String toString() {
    return String.format("%dB", this.bytes);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    DataSize otherSize = (DataSize) other;
    return (this.bytes == otherSize.bytes);
  }

  @Override
  public int hashCode() {
    return Long.hashCode(this.bytes);
  }

}

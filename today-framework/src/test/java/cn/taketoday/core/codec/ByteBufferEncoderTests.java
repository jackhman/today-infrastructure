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

package cn.taketoday.core.codec;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sebastien Deleuze
 */
class ByteBufferEncoderTests extends AbstractEncoderTests<ByteBufferEncoder> {

  private final byte[] fooBytes = "foo".getBytes(StandardCharsets.UTF_8);

  private final byte[] barBytes = "bar".getBytes(StandardCharsets.UTF_8);

  ByteBufferEncoderTests() {
    super(new ByteBufferEncoder());
  }

  @Override
  @Test
  public void canEncode() {
    assertThat(this.encoder.canEncode(ResolvableType.fromClass(ByteBuffer.class),
            MimeTypeUtils.TEXT_PLAIN)).isTrue();
    assertThat(this.encoder.canEncode(ResolvableType.fromClass(Integer.class),
            MimeTypeUtils.TEXT_PLAIN)).isFalse();
    assertThat(this.encoder.canEncode(ResolvableType.fromClass(ByteBuffer.class),
            MimeTypeUtils.APPLICATION_JSON)).isTrue();

    // SPR-15464
    assertThat(this.encoder.canEncode(ResolvableType.NONE, null)).isFalse();
  }

  @Override
  @Test
  public void encode() {
    Flux<ByteBuffer> input = Flux.just(this.fooBytes, this.barBytes)
            .map(ByteBuffer::wrap);

    testEncodeAll(input, ByteBuffer.class, step -> step
            .consumeNextWith(expectBytes(this.fooBytes))
            .consumeNextWith(expectBytes(this.barBytes))
            .verifyComplete());
  }

}
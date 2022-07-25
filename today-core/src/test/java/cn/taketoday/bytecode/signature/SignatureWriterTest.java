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
package cn.taketoday.bytecode.signature;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import cn.taketoday.bytecode.AsmTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * SignatureWriter tests.
 *
 * @author Eric Bruneton
 */
public class SignatureWriterTest extends AsmTest {

  @ParameterizedTest
  @MethodSource({
          "cn.taketoday.bytecode.signature.SignaturesProviders#classSignatures",
          "cn.taketoday.bytecode.signature.SignaturesProviders#methodSignatures"
  })
  public void testReadAndWrite_classOrMethodSignature(final String signature) {
    SignatureWriter signatureWriter = new SignatureWriter();

    new SignatureReader(signature).accept(signatureWriter);

    assertEquals(signature, signatureWriter.toString());
  }

  @ParameterizedTest
  @MethodSource("cn.taketoday.bytecode.signature.SignaturesProviders#fieldSignatures")
  public void testReadAndWrite_fieldSignature(final String signature) {
    SignatureWriter signatureWriter = new SignatureWriter();

    new SignatureReader(signature).acceptType(signatureWriter);

    assertEquals(signature, signatureWriter.toString());
  }
}

/*
 * Copyright 2003 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.taketoday.core.bytecode.transform.impl;

import org.junit.jupiter.api.Test;

import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.transform.AbstractTransformTest;
import cn.taketoday.core.bytecode.transform.ClassTransformer;
import cn.taketoday.core.bytecode.transform.ClassTransformerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author baliuka
 */
public class TestInterceptFields extends AbstractTransformTest implements InterceptFieldCallback {

  static Object TEST_VALUE = "test1";

  String field;

  @Test
  public void test() throws Exception {
    Object o = new TestInterceptFields().transform().newInstance();
    InterceptFieldEnabled e = (InterceptFieldEnabled) o;
    e.setInterceptFieldCallback(this);
    field = "test";
    assertEquals(TEST_VALUE, field);

  }

  protected ClassTransformerFactory getTransformer() throws Exception {

    return new ClassTransformerFactory() {

      public ClassTransformer newTransformer() {

        return new InterceptFieldTransformer(

                new InterceptFieldFilter() {
                  public boolean acceptRead(Type owner, String name) {
                    return true;
                  }

                  public boolean acceptWrite(Type owner, String name) {
                    return true;
                  }

                }

        );

      }

    };

  }

  public boolean readBoolean(Object _this, String name, boolean oldValue) {

    return oldValue;
  }

  public byte readByte(Object _this, String name, byte oldValue) {

    return oldValue;
  }

  public char readChar(Object _this, String name, char oldValue) {

    return oldValue;
  }

  public double readDouble(Object _this, String name, double oldValue) {

    return oldValue;
  }

  public float readFloat(Object _this, String name, float oldValue) {

    return oldValue;
  }

  public int readInt(Object _this, String name, int oldValue) {

    return oldValue;
  }

  public long readLong(Object _this, String name, long oldValue) {

    return oldValue;
  }

  public Object readObject(Object _this, String name, Object oldValue) {

    return TEST_VALUE;
  }

  public short readShort(Object _this, String name, short oldValue) {

    return oldValue;
  }

  public boolean writeBoolean(Object _this, String name, boolean oldValue, boolean newValue) {

    return newValue;
  }

  public byte writeByte(Object _this, String name, byte oldValue, byte newValue) {

    return newValue;
  }

  public char writeChar(Object _this, String name, char oldValue, char newValue) {

    return newValue;
  }

  public double writeDouble(Object _this, String name, double oldValue, double newValue) {

    return newValue;
  }

  public float writeFloat(Object _this, String name, float oldValue, float newValue) {

    return newValue;
  }

  public int writeInt(Object _this, String name, int oldValue, int newValue) {

    return newValue;
  }

  public long writeLong(Object _this, String name, long oldValue, long newValue) {

    return newValue;
  }

  public Object writeObject(Object _this, String name, Object oldValue, Object newValue) {

    return newValue;
  }

  public short writeShort(Object _this, String name, short oldValue, short newValue) {

    return newValue;
  }

}

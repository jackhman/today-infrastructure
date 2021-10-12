/*
 * Copyright 2003,2004 The Apache Software Foundation
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
package cn.taketoday.core.bytecode.proxy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLazyLoader {

  private static class LazyBean {
    private String name;

    public LazyBean() { }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  @Test
  public void testLazyLoader() {
    LazyLoader loader = new LazyLoader() {

      @Override
      public LazyBean loadObject() {
        System.err.println("loading object");
        final LazyBean lazyBean = new LazyBean();
        lazyBean.setName("TEST");
        return lazyBean;
      }
    };
    LazyBean obj = (LazyBean) Enhancer.create(LazyBean.class, loader);

    System.err.println(obj.toString());
    System.err.println(obj.getClass());
    System.err.println(obj.getName());

    assertEquals("TEST", obj.getName());
  }

}

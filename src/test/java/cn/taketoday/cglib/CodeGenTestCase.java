/*
 * Copyright 2002,2003,2004 The Apache Software Foundation
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
package cn.taketoday.cglib;

import junit.framework.TestCase;

/**
 * @author Chris Nokleberg
 * <a href="mailto:chris@nokleberg.com">chris@nokleberg.com</a>
 * @version $Id: CodeGenTestCase.java,v 1.10 2012/07/27 16:02:49 baliuka Exp $
 */
public abstract class CodeGenTestCase extends TestCase {

  public CodeGenTestCase(String testName) {
    super(testName);
  }

  public abstract void perform(ClassLoader loader) throws Throwable;

  public static void printlnError(String x) {
//    System.err.println(x);
  }

  public static void println(String x) {
    //    System.out.println(x);
  }
}

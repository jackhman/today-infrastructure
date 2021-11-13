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
package cn.taketoday.core.bytecode.proxy;

import cn.taketoday.core.bytecode.commons.MethodSignature;
import cn.taketoday.core.bytecode.core.AbstractClassGenerator;
import cn.taketoday.core.bytecode.core.CodeGenerationException;
import cn.taketoday.core.bytecode.core.GeneratorStrategy;
import cn.taketoday.core.bytecode.core.NamingPolicy;
import cn.taketoday.core.reflect.MethodAccess;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Classes generated by {@link Enhancer} pass this object to the registered
 * {@link MethodInterceptor} objects when an intercepted method is invoked. It
 * can be used to either invoke the original method, or call the same method on
 * a different object of the same type.
 *
 * @author TODAY <br>
 * 2019-10-21 23:49
 */
public class MethodProxy {

//  private final int index1;
//  private final int index2;
//  private final MethodAccess method1;
//  private final MethodAccess method2;
//
//  private final MethodSignature sig1;
//  private final MethodSignature sig2;
//
//  private MethodProxy(MethodSignature sig1, MethodSignature sig2, int i1, int i2, MethodAccess f1, MethodAccess f2) {
//    this.sig1 = sig1;
//    this.sig2 = sig2;
//    this.method1 = f1;
//    this.method2 = f2;
//    this.index1 = i1;
//    this.index2 = i2;
//  }
//
//  /**
//   * For internal use by {@link Enhancer} only; see the
//   * {@link FastMethodAccessor} class for similar
//   * functionality.
//   */
//  public static MethodProxy create(Class<?> c1, Class<?> c2, String desc, String name1, String name2) {
//    final MethodSignature sig1 = new MethodSignature(name1, desc);
//    final MethodSignature sig2 = new MethodSignature(name2, desc);
//    final MethodAccess f1 = MethodAccess.from(c1);
//    final MethodAccess f2 = MethodAccess.from(c2);
//
//    return new MethodProxy(sig1, sig2, f1.getIndex(sig1), f2.getIndex(sig2), f1, f2);
//  }
//
//  /**
//   * Return the signature of the proxied method.
//   */
//  public MethodSignature getSignature() {
//    return sig1;
//  }
//
//  /**
//   * Return the name of the synthetic method created by CGLIB which is used by
//   * {@link #invokeSuper} to invoke the superclass (non-intercepted) method
//   * implementation. The parameter types are the same as the proxied method.
//   */
//  public String getSuperName() {
//    return sig2.getName();
//  }
//
//  /**
//   * Return the {@link MethodAccess} method index
//   * for the method used by {@link #invokeSuper}. This index uniquely identifies
//   * the method within the generated proxy, and therefore can be useful to
//   * reference external metadata.
//   *
//   * @see #getSuperName
//   */
//  public int getSuperIndex() {
//    return index2;
//  }
//
//  /**
//   * Return the <code>MethodProxy</code> used when intercepting the method
//   * matching the given signature.
//   *
//   * @param type the class generated by Enhancer
//   * @param sig the signature to match
//   * @return the MethodProxy instance, or null if no applicable matching method is
//   * found
//   * @throws IllegalArgumentException if the Class was not created by Enhancer or does not use a
//   * MethodInterceptor
//   */
//  public static MethodProxy find(Class<?> type, MethodSignature sig) {
//    try {
//      Method m = type.getDeclaredMethod(
//              MethodInterceptorGenerator.FIND_PROXY_NAME, MethodSignature.class);
//      return (MethodProxy) m.invoke(null, new Object[] { sig });
//    }
//    catch (NoSuchMethodException e) {
//      throw new IllegalArgumentException("Class " + type + " does not use a MethodInterceptor");
//    }
//    catch (IllegalAccessException e) {
//      throw new CodeGenerationException(e);
//    }
//    catch (InvocationTargetException e) {
//      throw new CodeGenerationException(e.getTargetException());
//    }
//  }
//
//  /**
//   * Invoke the original method, on a different object of the same type.
//   *
//   * @param obj the compatible object; recursion will result if you use the object
//   * passed as the first argument to the MethodInterceptor (usually not
//   * what you want)
//   * @param args the arguments passed to the intercepted method; you may substitute
//   * a different argument array as long as the types are compatible
//   * @throws Throwable the bare exceptions thrown by the called method are passed
//   * through without wrapping in an
//   * <code>InvocationTargetException</code>
//   * @see MethodInterceptor#intercept
//   */
//  public Object invoke(Object obj, Object[] args) throws Throwable {
//    try {
//      return method1.invoke(index1, obj, args);
//    }
//    catch (InvocationTargetException e) {
//      throw e.getTargetException();
//    }
//    catch (IllegalArgumentException e) {
//      if (index1 < 0) {
//        throw new IllegalArgumentException("Protected method: " + sig1);
//      }
//      throw e;
//    }
//  }
//
//  /**
//   * Invoke the original (super) method on the specified object.
//   *
//   * @param obj the enhanced object, must be the object passed as the first
//   * argument to the MethodInterceptor
//   * @param args the arguments passed to the intercepted method; you may substitute
//   * a different argument array as long as the types are compatible
//   * @throws Throwable the bare exceptions thrown by the called method are passed
//   * through without wrapping in an
//   * <code>InvocationTargetException</code>
//   * @see MethodInterceptor#intercept
//   */
//  public Object invokeSuper(final Object obj, final Object[] args) throws Throwable {
//    try {
//      return method2.invoke(index2, obj, args);
//    }
//    catch (InvocationTargetException e) {
//      throw e.getTargetException();
//    }
//  }


  private MethodSignature sig1;
  private MethodSignature sig2;

  private CreateInfo createInfo;

  private final Object initLock = new Object();
  private volatile FastClassInfo fastClassInfo;

  /**
   * For internal use by {@link Enhancer} only; see the {@link cn.taketoday.cglib.reflect.FastMethod} class
   * for similar functionality.
   */
  public static MethodProxy create(Class c1, Class c2, String desc, String name1, String name2) {
    MethodProxy proxy = new MethodProxy();
    proxy.sig1 = new MethodSignature(name1, desc);
    proxy.sig2 = new MethodSignature(name2, desc);
    proxy.createInfo = new CreateInfo(c1, c2);
    return proxy;
  }

  private void init() {
    /*
     * Using a volatile invariant allows us to initialize the FastClass and
     * method index pairs atomically.
     *
     * Double-checked locking is safe with volatile in Java 5.  Before 1.5 this
     * code could allow fastClassInfo to be instantiated more than once, which
     * appears to be benign.
     */
    if (fastClassInfo == null) {
      synchronized(initLock) {
        if (fastClassInfo == null) {
          CreateInfo ci = createInfo;

          FastClassInfo fci = new FastClassInfo();
          fci.f1 = helper(ci, ci.c1);
          fci.f2 = helper(ci, ci.c2);
          fci.i1 = fci.f1.getIndex(sig1);
          fci.i2 = fci.f2.getIndex(sig2);
          fastClassInfo = fci;
          createInfo = null;
        }
      }
    }
  }


  private static class FastClassInfo {

    MethodAccess f1;
    MethodAccess f2;

    int i1;
    int i2;
  }


  private static class CreateInfo {

    Class c1;

    Class c2;

    NamingPolicy namingPolicy;

    GeneratorStrategy strategy;

    boolean attemptLoad;

    public CreateInfo(Class c1, Class c2) {
      this.c1 = c1;
      this.c2 = c2;
      AbstractClassGenerator fromEnhancer = AbstractClassGenerator.getCurrent();
      if (fromEnhancer != null) {
        namingPolicy = fromEnhancer.getNamingPolicy();
        strategy = fromEnhancer.getStrategy();
        attemptLoad = fromEnhancer.isAttemptLoad();
      }
    }
  }

  private static MethodAccess helper(CreateInfo ci, Class type) {
    MethodAccess.Generator g = new MethodAccess.Generator(type);
    g.setNeighbor(type);
    g.setClassLoader(ci.c2.getClassLoader());
    g.setNamingPolicy(ci.namingPolicy);
    g.setStrategy(ci.strategy);
    g.setAttemptLoad(ci.attemptLoad);
    return g.create();
  }

  private MethodProxy() { }

  /**
   * Return the signature of the proxied method.
   */
  public MethodSignature getSignature() {
    return sig1;
  }

  /**
   * Return the name of the synthetic method created by CGLIB which is
   * used by {@link #invokeSuper} to invoke the superclass
   * (non-intercepted) method implementation. The parameter types are
   * the same as the proxied method.
   */
  public String getSuperName() {
    return sig2.getName();
  }

  /**
   * Return the {@link MethodAccess} method index for the method used by {@link #invokeSuper}. This index uniquely
   * identifies the method within the generated proxy, and therefore
   * can be useful to reference external metadata.
   *
   * @see #getSuperName
   */
  public int getSuperIndex() {
    init();
    return fastClassInfo.i2;
  }

  // For testing
  MethodAccess getFastClass() {
    init();
    return fastClassInfo.f1;
  }

  // For testing
  MethodAccess getSuperFastClass() {
    init();
    return fastClassInfo.f2;
  }

  /**
   * Return the <code>MethodProxy</code> used when intercepting the method
   * matching the given signature.
   *
   * @param type the class generated by Enhancer
   * @param sig the signature to match
   * @return the MethodProxy instance, or null if no applicable matching method is found
   * @throws IllegalArgumentException if the Class was not created by Enhancer or does not use a MethodInterceptor
   */
  public static MethodProxy find(Class type, MethodSignature sig) {
    try {
      Method m = type.getDeclaredMethod(
              MethodInterceptorGenerator.FIND_PROXY_NAME, MethodSignature.class);
      return (MethodProxy) m.invoke(null, new Object[] { sig });
    }
    catch (NoSuchMethodException ex) {
      throw new IllegalArgumentException("Class " + type + " does not use a MethodInterceptor");
    }
    catch (IllegalAccessException | InvocationTargetException ex) {
      throw new CodeGenerationException(ex);
    }
  }

  /**
   * Invoke the original method, on a different object of the same type.
   *
   * @param obj the compatible object; recursion will result if you use the object passed as the first
   * argument to the MethodInterceptor (usually not what you want)
   * @param args the arguments passed to the intercepted method; you may substitute a different
   * argument array as long as the types are compatible
   * @throws Throwable the bare exceptions thrown by the called method are passed through
   * without wrapping in an <code>InvocationTargetException</code>
   * @see MethodInterceptor#intercept
   */
  public Object invoke(Object obj, Object[] args) throws Throwable {
    try {
      init();
      FastClassInfo fci = fastClassInfo;
      return fci.f1.invoke(fci.i1, obj, args);
    }
    catch (InvocationTargetException ex) {
      throw ex.getTargetException();
    }
    catch (IllegalArgumentException ex) {
      if (fastClassInfo.i1 < 0)
        throw new IllegalArgumentException("Protected method: " + sig1);
      throw ex;
    }
  }

  /**
   * Invoke the original (super) method on the specified object.
   *
   * @param obj the enhanced object, must be the object passed as the first
   * argument to the MethodInterceptor
   * @param args the arguments passed to the intercepted method; you may substitute a different
   * argument array as long as the types are compatible
   * @throws Throwable the bare exceptions thrown by the called method are passed through
   * without wrapping in an <code>InvocationTargetException</code>
   * @see MethodInterceptor#intercept
   */
  public Object invokeSuper(Object obj, Object[] args) throws Throwable {
    try {
      init();
      FastClassInfo fci = fastClassInfo;
      return fci.f2.invoke(fci.i2, obj, args);
    }
    catch (InvocationTargetException e) {
      throw e.getTargetException();
    }
  }

}

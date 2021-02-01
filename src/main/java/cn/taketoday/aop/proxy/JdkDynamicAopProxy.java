/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.aop.proxy;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import cn.taketoday.aop.AopInvocationException;
import cn.taketoday.aop.TargetSource;
import cn.taketoday.aop.intercept.DefaultMethodInvocation;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.reflect.MethodMethodAccessor;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ObjectUtils;

/**
 * JDK-based {@link AopProxy} implementation for the AOP framework,
 * based on JDK {@link java.lang.reflect.Proxy dynamic proxies}.
 *
 * <p>Creates a dynamic proxy, implementing the interfaces exposed by
 * the AopProxy. Dynamic proxies <i>cannot</i> be used to proxy methods
 * defined in classes, rather than interfaces.
 *
 * <p>Objects of this type should be obtained through proxy factories,
 * configured by an {@link AdvisedSupport} class. This class is internal
 * to AOP framework and need not be used directly by client code.
 *
 * <p>Proxies created using this class will be thread-safe if the
 * underlying (target) class is thread-safe.
 *
 * <p>Proxies are serializable so long as all Advisors (including Advices
 * and Pointcuts) and the TargetSource are serializable.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Dave Syer
 * @author TODAY 2021/2/1 22:42
 * @see java.lang.reflect.Proxy
 * @see AdvisedSupport
 * @see ProxyFactory
 * @since 3.0
 */
public class JdkDynamicAopProxy implements AopProxy, InvocationHandler, Serializable {

  private static final long serialVersionUID = 1L;

  /*
   * NOTE: We could avoid the code duplication between this class and the CGLIB
   * proxies by refactoring "invoke" into a template method. However, this approach
   * adds at least 10% performance overhead versus a copy-paste solution, so we sacrifice
   * elegance for performance. (We have a good test suite to ensure that the different
   * proxies behave the same :-)
   * This way, we can also more easily take advantage of minor optimizations in each class.
   */

  /** We use a static Log to avoid serialization issues. */
  private static final Logger logger = LoggerFactory.getLogger(JdkDynamicAopProxy.class);

  /** Config used to configure this proxy. */
  private final AdvisedSupport advised;

  /**
   * Is the {@link #equals} method defined on the proxied interfaces?
   */
  private boolean equalsDefined;

  /**
   * Is the {@link #hashCode} method defined on the proxied interfaces?
   */
  private boolean hashCodeDefined;

  /**
   * Construct a new JdkDynamicAopProxy for the given AOP configuration.
   *
   * @param config
   *         the AOP configuration as AdvisedSupport object
   *
   * @throws AopConfigException
   *         if the config is invalid. We try to throw an informative
   *         exception in this case, rather than let a mysterious failure happen later.
   */
  public JdkDynamicAopProxy(AdvisedSupport config) {
    Assert.notNull(config, "AdvisedSupport must not be null");
    if (config.getAdvisors().length == 0 && config.getTargetSource() == AdvisedSupport.EMPTY_TARGET_SOURCE) {
      throw new AopConfigException("No advisors and no TargetSource specified");
    }
    this.advised = config;
  }

  @Override
  public Object getProxy() {
    return getProxy(ClassUtils.getClassLoader());
  }

  @Override
  public Object getProxy(ClassLoader classLoader) {
    if (logger.isTraceEnabled()) {
      logger.trace("Creating JDK dynamic proxy: " + this.advised.getTargetSource());
    }
    Class<?>[] proxiedInterfaces = AopProxyUtils.completeProxiedInterfaces(this.advised);
    findDefinedEqualsAndHashCodeMethods(proxiedInterfaces);
    return Proxy.newProxyInstance(classLoader, proxiedInterfaces, this);
  }

  /**
   * Finds any {@link #equals} or {@link #hashCode} method that may be defined
   * on the supplied set of interfaces.
   *
   * @param proxiedInterfaces
   *         the interfaces to introspect
   */
  private void findDefinedEqualsAndHashCodeMethods(Class<?>[] proxiedInterfaces) {
    for (Class<?> proxiedInterface : proxiedInterfaces) {
      Method[] methods = proxiedInterface.getDeclaredMethods();
      for (Method method : methods) {
        if (AopUtils.isEqualsMethod(method)) {
          this.equalsDefined = true;
        }
        if (AopUtils.isHashCodeMethod(method)) {
          this.hashCodeDefined = true;
        }
        if (this.equalsDefined && this.hashCodeDefined) {
          return;
        }
      }
    }
  }

  /**
   * Implementation of {@code InvocationHandler.invoke}.
   * <p>Callers will see exactly the exception thrown by the target,
   * unless a hook method throws an exception.
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    Object oldProxy = null;
    boolean setProxyContext = false;

    TargetSource targetSource = this.advised.targetSource;
    Object target = null;

    try {
      if (!this.equalsDefined && AopUtils.isEqualsMethod(method)) {
        // The target does not implement the equals(Object) method itself.
        return equals(args[0]);
      }
      else if (!this.hashCodeDefined && AopUtils.isHashCodeMethod(method)) {
        // The target does not implement the hashCode() method itself.
        return hashCode();
      }
      else if (!this.advised.opaque && method.getDeclaringClass().isInterface() &&
              method.getDeclaringClass().isAssignableFrom(Advised.class)) {
        // Service invocations on ProxyConfig with the proxy config...
        return AopUtils.invokeJoinpointUsingReflection(this.advised, method, args);
      }

      Object retVal;

      if (this.advised.exposeProxy) {
        // Make invocation available if necessary.
        oldProxy = AopContext.setCurrentProxy(proxy);
        setProxyContext = true;
      }

      // Get as late as possible to minimize the time we "own" the target,
      // in case it comes from a pool.
      target = targetSource.getTarget();
      Class<?> targetClass = (target != null ? target.getClass() : null);

      // Get the interception chain for this method.
      MethodInterceptor[] chain = this.advised.getInterceptors(method, targetClass);

      // Check whether we have any advice. If we don't, we can fallback on direct
      // reflective invocation of the target, and avoid creating a MethodInvocation.
      if (ObjectUtils.isEmpty(chain)) {
        // We can skip creating a MethodInvocation: just invoke the target directly
        // Note that the final invoker must be an InvokerInterceptor so we know it does
        // nothing but a reflective operation on the target, and no hot swapping or fancy proxying.
        Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);
        retVal = AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse);
      }
      else {
        MethodInvocation invocation =
                new DefaultMethodInvocation(target, method, new MethodMethodAccessor(method), args, chain);
        // Proceed to the joinpoint through the interceptor chain.
        retVal = invocation.proceed();
      }

      // Massage return value if necessary.
      Class<?> returnType = method.getReturnType();

      if (retVal == null && returnType != Void.TYPE && returnType.isPrimitive()) {
        throw new AopInvocationException(
                "Null return value from advice does not match primitive return type for: " + method);
      }
      return retVal;
    }
    finally {
      if (target != null && !targetSource.isStatic()) {
        // Must have come from TargetSource.
        targetSource.releaseTarget(target);
      }
      if (setProxyContext) {
        // Restore old proxy.
        AopContext.setCurrentProxy(oldProxy);
      }
    }
  }

  /**
   * Equality means interfaces, advisors and TargetSource are equal.
   * <p>The compared object may be a JdkDynamicAopProxy instance itself
   * or a dynamic proxy wrapping a JdkDynamicAopProxy instance.
   */
  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (other == null) {
      return false;
    }

    JdkDynamicAopProxy otherProxy;
    if (other instanceof JdkDynamicAopProxy) {
      otherProxy = (JdkDynamicAopProxy) other;
    }
    else if (Proxy.isProxyClass(other.getClass())) {
      InvocationHandler ih = Proxy.getInvocationHandler(other);
      if (!(ih instanceof JdkDynamicAopProxy)) {
        return false;
      }
      otherProxy = (JdkDynamicAopProxy) ih;
    }
    else {
      // Not a valid comparison...
      return false;
    }

    // If we get here, otherProxy is the other AopProxy.
    return AopProxyUtils.equalsInProxy(this.advised, otherProxy.advised);
  }

  /**
   * Proxy uses the hash code of the TargetSource.
   */
  @Override
  public int hashCode() {
    return JdkDynamicAopProxy.class.hashCode() * 13 + this.advised.getTargetSource().hashCode();
  }

}

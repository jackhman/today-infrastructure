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
package cn.taketoday.web.handler.method;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ControllerAdvice;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.handler.AbstractActionMappingMethodExceptionHandler;

/**
 * Handle {@link ExceptionHandler} annotated method
 * <p>
 * this method indicates that is a exception handler
 * </p>
 *
 * @author TODAY 2019-06-22 19:17
 * @since 2.3.7
 */
public class ExceptionHandlerAnnotationExceptionHandler
        extends AbstractActionMappingMethodExceptionHandler implements ApplicationContextAware, InitializingBean {

  private static final Logger log = LoggerFactory.getLogger(ExceptionHandlerAnnotationExceptionHandler.class);

  private final ConcurrentHashMap<Class<?>, ExceptionHandlerMethodResolver> exceptionHandlerCache =
          new ConcurrentHashMap<>(64);

  private final LinkedHashMap<ControllerAdviceBean, ExceptionHandlerMethodResolver> exceptionHandlerAdviceCache =
          new LinkedHashMap<>();

  private final ConcurrentHashMap<Method, ActionMappingAnnotationHandler> exceptionHandlerMapping =
          new ConcurrentHashMap<>(64);

  @Nullable
  private ApplicationContext applicationContext;

  private AnnotationHandlerFactory handlerFactory;

  @Nullable
  @Override
  protected Object handleInternal(RequestContext context,
          @Nullable ActionMappingAnnotationHandler annotationHandler, Throwable target) {
    // catch all handlers
    ActionMappingAnnotationHandler exHandler = lookupExceptionHandler(annotationHandler, target);
    if (exHandler == null) {
      return null; // next
    }

    logCatchThrowable(target);
    try {
      if (log.isDebugEnabled()) {
        log.debug("Using @ExceptionHandler {}", exHandler);
      }
      return handleException(context, exHandler);
    }
    catch (Throwable handlerEx) {
      logResultedInException(target, handlerEx);
      // next handler
      return null;
    }
  }

  /**
   * Handle Exception use {@link ActionMappingAnnotationHandler}
   *
   * @param context current request
   * @param exHandler ThrowableHandlerMethod
   * @return handler return value
   * @throws Throwable occurred in exHandler
   */
  protected Object handleException(RequestContext context, ActionMappingAnnotationHandler exHandler)
          throws Throwable {
    exHandler.handleReturnValue(context, exHandler, exHandler.invokeHandler(context));
    return NONE_RETURN_VALUE;
  }

  /**
   * Find an {@code @ExceptionHandler} method for the given exception. The default
   * implementation searches methods in the class hierarchy of the controller first
   * and if not found, it continues searching for additional {@code @ExceptionHandler}
   * methods assuming some {@linkplain ControllerAdvice @ControllerAdvice}
   * Spring-managed beans were detected.
   *
   * @param exception the raised exception
   * @return a method to handle the exception, or {@code null} if none
   */
  @Nullable
  protected ActionMappingAnnotationHandler lookupExceptionHandler(
          @Nullable ActionMappingAnnotationHandler annotationHandler, Throwable exception) {

    Class<?> handlerType = null;

    if (annotationHandler != null) {
      // Local exception handler methods on the controller class itself.
      // To be invoked through the proxy, even in case of an interface-based proxy.
      handlerType = annotationHandler.getBeanType();
      ExceptionHandlerMethodResolver resolver = exceptionHandlerCache.computeIfAbsent(
              handlerType, ExceptionHandlerMethodResolver::new);
      Method method = resolver.resolveMethod(exception);
      if (method != null) {
        return exceptionHandlerMapping.computeIfAbsent(method,
                key -> getHandler(annotationHandler::getHandlerObject, key, annotationHandler.getBeanType()));
      }
      // For advice applicability check below (involving base packages, assignable types
      // and annotation presence), use target class instead of interface-based proxy.
      if (Proxy.isProxyClass(handlerType)) {
        handlerType = AopUtils.getTargetClass(annotationHandler.getHandlerObject());
      }
    }

    for (Map.Entry<ControllerAdviceBean, ExceptionHandlerMethodResolver> entry : exceptionHandlerAdviceCache.entrySet()) {
      ControllerAdviceBean advice = entry.getKey();
      if (advice.isApplicableToBeanType(handlerType)) {
        ExceptionHandlerMethodResolver resolver = entry.getValue();
        Method method = resolver.resolveMethod(exception);
        if (method != null) {
          return exceptionHandlerMapping.computeIfAbsent(method,
                  key -> getHandler(advice::resolveBean, key, advice.getBeanType()));
        }
      }
    }

    return null;
  }

  private ActionMappingAnnotationHandler getHandler(
          Supplier<Object> handlerBean, Method method, Class<?> errorHandlerType) {
    return handlerFactory.create(handlerBean, method, errorHandlerType);
  }

  //

  @Override
  public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  @Nullable
  public ApplicationContext getApplicationContext() {
    return this.applicationContext;
  }

  public void setHandlerFactory(AnnotationHandlerFactory handlerFactory) {
    this.handlerFactory = handlerFactory;
  }

  @Override
  public void afterPropertiesSet() {
    ApplicationContext context = getApplicationContext();
    if (handlerFactory == null) {
      handlerFactory = new AnnotationHandlerFactory(context);
      handlerFactory.initDefaults();
    }
    if (context != null) {
      initExceptionHandlerAdviceCache(context);
    }
  }

  private void initExceptionHandlerAdviceCache(ApplicationContext applicationContext) {
    List<ControllerAdviceBean> adviceBeans = ControllerAdviceBean.findAnnotatedBeans(applicationContext);
    for (ControllerAdviceBean adviceBean : adviceBeans) {
      Class<?> beanType = adviceBean.getBeanType();
      if (beanType == null) {
        throw new IllegalStateException("Unresolvable type for ControllerAdviceBean: " + adviceBean);
      }
      ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(beanType);
      if (resolver.hasExceptionMappings()) {
        this.exceptionHandlerAdviceCache.put(adviceBean, resolver);
      }
    }

    if (logger.isDebugEnabled()) {
      int handlerSize = this.exceptionHandlerAdviceCache.size();
      if (handlerSize == 0) {
        logger.debug("ControllerAdvice beans: none");
      }
      else {
        logger.debug("ControllerAdvice beans: {} @ExceptionHandler", handlerSize);
      }
    }
  }

}
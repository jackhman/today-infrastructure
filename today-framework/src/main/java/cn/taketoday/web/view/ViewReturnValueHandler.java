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

package cn.taketoday.web.view;

import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Locale;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Experimental;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextUtils;
import cn.taketoday.web.handler.HandlerExceptionHandler;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.BindingContext;
import cn.taketoday.web.handler.result.HandlerMethodReturnValueHandler;

/**
 * view-name or {@link View} ReturnValueHandler
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see View
 * @see RedirectModel
 * @see RedirectModelManager
 * @since 4.0 2022/2/9 20:34
 */
public class ViewReturnValueHandler implements HandlerMethodReturnValueHandler {

  private final ViewResolver viewResolver;

  @Nullable
  private RedirectModelManager modelManager;

  private boolean putAllOutputRedirectModel = true;

  public ViewReturnValueHandler(ViewResolver viewResolver) {
    Assert.notNull(viewResolver, "viewResolver is required");
    this.viewResolver = viewResolver;
  }

  @Override
  public boolean supportsReturnValue(@Nullable Object returnValue) {
    return returnValue instanceof String || returnValue instanceof View;
  }

  @Override
  public boolean supportsHandlerMethod(HandlerMethod handler) {
    if (handler.isReturn(String.class)) {
      return !handler.isResponseBody();
    }
    return handler.isReturnTypeAssignableTo(View.class);
  }

  @Experimental
  public static boolean supportsLambda(@Nullable Object handler) {
    if (handler != null) {
      Class<?> handlerClass = handler.getClass();
      Method method = ReflectionUtils.findMethod(handlerClass, "writeReplace");
      if (method != null) {
        ReflectionUtils.makeAccessible(method);

        Object returnValue = ReflectionUtils.invokeMethod(method, handler);
        if (returnValue instanceof SerializedLambda lambda) {
          Class<?> implClass = ClassUtils.load(lambda.getImplClass().replace('/', '.'));
          if (implClass != null) {
            Method declaredMethod = ReflectionUtils.findMethod(implClass, lambda.getImplMethodName(), RequestContext.class);
            if (declaredMethod != null) {
              return HandlerMethod.isResponseBody(declaredMethod);
            }
          }
        }
      }
    }
    return false;
  }

  /**
   * @param context Current HTTP request context
   * @param handler Target HTTP handler
   * @param returnValue Handler execution result
   * Or {@link HandlerExceptionHandler} return value
   * @throws ViewRenderingException Could not resolve view with given name
   */
  @Override
  public void handleReturnValue(
          RequestContext context, Object handler, @Nullable Object returnValue) throws Exception {
    if (returnValue instanceof String viewName) {
      renderView(context, handler, viewName);
    }
    else if (returnValue instanceof View view) {
      renderView(context, view);
    }
  }

  public void renderView(RequestContext context, Object handler, String viewName) throws Exception {
    Locale locale = RequestContextUtils.getLocale(context);
    View view = viewResolver.resolveViewName(viewName, locale);
    if (view == null) {
      throw new ViewRenderingException(
              "Could not resolve view with name '" + viewName + "' in handler '" + handler + "'");
    }
    renderView(context, view);
  }

  public void renderView(RequestContext context, View view) throws Exception {
    LinkedHashMap<String, Object> model = new LinkedHashMap<>();

    // put all input RedirectModel
    RedirectModel inputRedirectModel = RequestContextUtils.getInputRedirectModel(context, modelManager);
    if (inputRedirectModel != null) {
      model.putAll(inputRedirectModel.asMap());
    }

    if (putAllOutputRedirectModel) {
      // put all output RedirectModel
      RedirectModel outputRedirectModel = RequestContextUtils.getOutputRedirectModel(context);
      if (outputRedirectModel != null) {
        model.putAll(outputRedirectModel.asMap());
      }
    }

    BindingContext bindingContext = context.getBindingContext();
    if (bindingContext != null) {
      // injected arguments Model
      model.putAll(bindingContext.getModel());
    }

    // do rendering
    view.render(model, context);
  }

  /**
   * set {@link #putAllOutputRedirectModel} to determine if all 'output'
   * RedirectModel should be put into model
   *
   * @param putAllOutputRedirectModel If true, all 'output' RedirectModel
   * will be put to current view
   * @see RequestContextUtils#getOutputRedirectModel(RequestContext)
   */
  public void setPutAllOutputRedirectModel(boolean putAllOutputRedirectModel) {
    this.putAllOutputRedirectModel = putAllOutputRedirectModel;
  }

  /**
   * set RedirectModelManager to resolve 'input' RedirectModel
   *
   * @param modelManager RedirectModelManager to manage 'input' or 'output' RedirectModel
   */
  public void setModelManager(@Nullable RedirectModelManager modelManager) {
    this.modelManager = modelManager;
  }

  @Nullable
  public RedirectModelManager getModelManager() {
    return modelManager;
  }

  public ViewResolver getViewResolver() {
    return viewResolver;
  }

}

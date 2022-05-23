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
package cn.taketoday.web.registry.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.SmartInitializingSingleton;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.HandlerMapping;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.annotation.ActionMapping;
import cn.taketoday.web.annotation.Controller;
import cn.taketoday.web.annotation.Interceptor;
import cn.taketoday.web.annotation.PathVariable;
import cn.taketoday.web.handler.method.ActionMappingAnnotationHandler;
import cn.taketoday.web.handler.method.AnnotationHandlerFactory;
import cn.taketoday.web.handler.method.HandlerMethod;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.registry.AbstractUrlHandlerMapping;
import cn.taketoday.web.util.WebUtils;
import cn.taketoday.web.util.pattern.PathPattern;
import cn.taketoday.web.util.pattern.PathPatternParser;

/**
 * Store {@link HandlerMethod}
 *
 * @author TODAY <br>
 * 2018-07-1 20:47:06
 */
public class HandlerMethodMapping
        extends AbstractUrlHandlerMapping implements HandlerMapping, SmartInitializingSingleton {

  private ConfigurableBeanFactory beanFactory;

  /** @since 3.0 */
  private AnnotationHandlerFactory annotationHandlerFactory;

  private BeanDefinitionRegistry registry;

  private boolean detectHandlerMethodsInAncestorContexts = false;

  public HandlerMethodMapping() {
    setOrder(HIGHEST_PRECEDENCE);
  }

  // MappedHandlerRegistry
  // --------------------------

  @Override
  public void afterSingletonsInstantiated() {
    log.info("Initializing Annotation Controllers");
    initActions();
  }

  @Override
  protected void initApplicationContext(ApplicationContext context) {
    super.initApplicationContext(context);
    setBeanFactory(context.unwrapFactory(ConfigurableBeanFactory.class));
    this.registry = context.unwrapFactory(BeanDefinitionRegistry.class);
  }

  /**
   * Scan beans in the ApplicationContext, detect and register ActionMappings.
   *
   * @see #getCandidateBeanNames()
   */
  protected void initActions() {
    for (String beanName : getCandidateBeanNames()) {
      // ActionMapping on the class is ok
      MergedAnnotation<Controller> rootController = beanFactory.findAnnotationOnBean(beanName, Controller.class);
      MergedAnnotation<ActionMapping> actionMapping = beanFactory.findAnnotationOnBean(beanName, ActionMapping.class);
      MergedAnnotation<ActionMapping> controllerMapping = null;
      if (actionMapping.isPresent()) {
        controllerMapping = actionMapping;
      }
      // build
      if (rootController.isPresent() || actionMapping.isPresent()) {
        Class<?> type = beanFactory.getType(beanName);
        buildHandlerMethod(beanName, type, controllerMapping);
      }
    }
  }

  /**
   * Determine the names of candidate beans in the application context.
   *
   * @see #setDetectHandlerMethodsInAncestorContexts
   * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors
   * @since 4.0
   */
  protected Set<String> getCandidateBeanNames() {
    return this.detectHandlerMethodsInAncestorContexts
           ? BeanFactoryUtils.beanNamesForTypeIncludingAncestors(obtainApplicationContext(), Object.class)
           : obtainApplicationContext().getBeanNamesForType(Object.class);
  }

  /**
   * Start config
   */
  public void startConfiguration() {
    // @since 2.3.3
    initActions();
  }

  private void buildHandlerMethod(
          String beanName, Class<?> beanClass,
          @Nullable MergedAnnotation<ActionMapping> controllerMapping) {

    ReflectionUtils.doWithMethods(beanClass, method -> {
      buildHandlerMethod(beanName, method, beanClass, controllerMapping);
    });
  }

  /**
   * Set Action Mapping
   *
   * @param beanName bean name
   * @param method Action or Handler
   * @param beanClass Controller
   * @param controllerMapping find mapping on class
   */
  protected void buildHandlerMethod(
          String beanName, Method method, Class<?> beanClass,
          @Nullable MergedAnnotation<ActionMapping> controllerMapping) {
    MergedAnnotation<ActionMapping> annotation = MergedAnnotations.from(method).get(ActionMapping.class);
    if (annotation.isPresent()) {
      // build HandlerMethod
      ActionMappingAnnotationHandler handler = createHandler(beanName, beanClass, method);
      // do mapping url
      mappingHandlerMethod(handler, controllerMapping, annotation);
    }
  }

  /**
   * Create {@link ActionMappingAnnotationHandler}.
   *
   * @param beanName bean name
   * @param beanClass Controller class
   * @param method Action or Handler
   * @return A new {@link ActionMappingAnnotationHandler}
   */
  protected ActionMappingAnnotationHandler createHandler(String beanName, Class<?> beanClass, Method method) {
    List<HandlerInterceptor> interceptors = getInterceptors(beanClass, method);
    return annotationHandlerFactory.create(beanName, method, beanClass, interceptors);
  }

  /**
   * Mapping given HandlerMapping to {@link HandlerMethodMapping}
   *
   * @param handler current {@link ActionMappingAnnotationHandler}
   * methods on class
   */
  protected void mappingHandlerMethod(
          ActionMappingAnnotationHandler handler,
          @Nullable MergedAnnotation<ActionMapping> controllerMapping,
          MergedAnnotation<ActionMapping> handlerMethodMapping) {
    boolean emptyNamespaces = true;
    boolean addClassRequestMethods = false;
    Set<String> namespaces = Collections.emptySet();
    Set<HttpMethod> classRequestMethods = Collections.emptySet();
    if (controllerMapping != null) {
      namespaces = new LinkedHashSet<>(4, 1.0f); // name space
      classRequestMethods = new LinkedHashSet<>(8, 1.0f); // method
      for (String value : controllerMapping.getStringArray(MergedAnnotation.VALUE)) {
        namespaces.add(StringUtils.formatURL(value));
      }
      Collections.addAll(classRequestMethods, controllerMapping.getEnumArray("method", HttpMethod.class));
      emptyNamespaces = namespaces.isEmpty();
      addClassRequestMethods = !classRequestMethods.isEmpty();
    }

    boolean exclude = handlerMethodMapping.getBoolean("exclude"); // exclude name space on class ?
    Set<HttpMethod> requestMethods = // http request method on method(action/handler)
            CollectionUtils.newHashSet(handlerMethodMapping.getEnumArray("method", HttpMethod.class));

    if (addClassRequestMethods)
      requestMethods.addAll(classRequestMethods);

    for (String urlOnMethod : handlerMethodMapping.getStringArray("value")) { // url on method
      String checkedUrl = StringUtils.formatURL(urlOnMethod);
      // splice urls and request methods
      // ---------------------------------
      for (HttpMethod requestMethod : requestMethods) {
        if (exclude || emptyNamespaces) {
          mappingHandlerMethod(checkedUrl, requestMethod, handler);
        }
        else {
          for (String namespace : namespaces) {
            mappingHandlerMethod(namespace.concat(checkedUrl), requestMethod, handler);
          }
        }
      }
    }
  }

  /**
   * Mapping to {@link HandlerMethodMapping}
   *
   * @param handler {@link ActionMappingAnnotationHandler}
   * @param path Request path
   * @param requestMethod HTTP request method
   * @see HttpMethod
   */
  private void mappingHandlerMethod(String path, HttpMethod requestMethod, ActionMappingAnnotationHandler handler) {
    // GET/blog/users/1 GET/blog/#{key}/1
    PathPattern pathPattern = getRequestPathPattern(path);

    ActionMappingAnnotationHandler transformed = transformHandler(pathPattern, handler);
    super.registerHandler(requestMethod.name().concat(pathPattern.getPatternString()), transformed);
  }

  protected final PathPattern getRequestPathPattern(String path) {
    if (getApplicationContext() instanceof WebApplicationContext webApp) {
      String contextPath = webApp.getContextPath();
      if (StringUtils.hasText(contextPath)) {
        path = contextPath.concat(path);
      }
    }
    path = resolveEmbeddedVariables(path);
    path = WebUtils.getSanitizedPath(path);
    PathPatternParser patternParser = getPatternParser();
    return patternParser.parse(path);
  }

  /**
   * Transform {@link ActionMappingAnnotationHandler} if path contains {@link PathVariable}
   *
   * @param pathPattern path pattern
   * @param handler Target {@link ActionMappingAnnotationHandler}
   * @return Transformed {@link ActionMappingAnnotationHandler}
   */
  protected ActionMappingAnnotationHandler transformHandler(
          PathPattern pathPattern, ActionMappingAnnotationHandler handler) {
    if (containsPathVariable(pathPattern.getPatternString())) {
      mappingPathVariable(pathPattern, handler);
      return handler;
    }
    return handler;
  }

  /**
   * contains {@link PathVariable} char: '{' and '}'
   *
   * @param path handler key
   * @return If contains '{' and '}'
   */
  public static boolean containsPathVariable(String path) {
    return path.indexOf('{') > -1 && path.indexOf('}') > -1;
  }

  /**
   * Mapping path variable.
   */
  protected void mappingPathVariable(PathPattern pathPattern, ActionMappingAnnotationHandler handler) {
    HashMap<String, ResolvableMethodParameter> parameterMapping = new HashMap<>();

    ResolvableMethodParameter[] parameters = handler.getResolvableParameters();
    for (ResolvableMethodParameter parameter : parameters) {
      parameterMapping.put(parameter.getName(), parameter);
    }

    for (String variable : pathPattern.getVariableNames()) {
      ResolvableMethodParameter parameter = parameterMapping.get(variable);
      if (parameter == null) {
        throw new ConfigurationException(
                "There isn't a variable named: [" + variable +
                        "] in the parameter list at method: [" + handler.getMethod() + "]");
      }
    }
  }

  /**
   * Get list of intercepters.
   *
   * @param controllerClass controller class
   * @param action method
   * @return List of {@link HandlerInterceptor} objects
   */
  protected List<HandlerInterceptor> getInterceptors(Class<?> controllerClass, Method action) {
    ArrayList<HandlerInterceptor> ret = new ArrayList<>();
    Set<Interceptor> controllerInterceptors = AnnotatedElementUtils.getAllMergedAnnotations(controllerClass, Interceptor.class);
    // get interceptor on class
    if (CollectionUtils.isNotEmpty(controllerInterceptors)) {
      for (Interceptor controllerInterceptor : controllerInterceptors) {
        Collections.addAll(ret, getInterceptors(controllerInterceptor.value()));
      }
    }
    // HandlerInterceptor on a method
    Set<Interceptor> actionInterceptors = AnnotatedElementUtils.getAllMergedAnnotations(action, Interceptor.class);
    if (CollectionUtils.isNotEmpty(actionInterceptors)) {
      ApplicationContext beanFactory = obtainApplicationContext();
      for (Interceptor actionInterceptor : actionInterceptors) {
        Collections.addAll(ret, getInterceptors(actionInterceptor.value()));
        // exclude interceptors
        for (Class<? extends HandlerInterceptor> interceptor : actionInterceptor.exclude()) {
          ret.remove(beanFactory.getBean(interceptor));
        }
      }
    }
    return ret;
  }

  /***
   * Get {@link HandlerInterceptor} objects
   *
   * @param interceptors
   *            {@link HandlerInterceptor} class
   * @return Array of {@link HandlerInterceptor} objects
   */
  public HandlerInterceptor[] getInterceptors(Class<? extends HandlerInterceptor>[] interceptors) {
    if (ObjectUtils.isEmpty(interceptors)) {
      return HandlerInterceptor.EMPTY_ARRAY;
    }
    int i = 0;
    HandlerInterceptor[] ret = new HandlerInterceptor[interceptors.length];

    GenericApplicationContext registrar = obtainApplicationContext().unwrap(GenericApplicationContext.class);
    for (Class<? extends HandlerInterceptor> interceptor : interceptors) {
      if (!registry.containsBeanDefinition(interceptor, true)) {
        try {
          registrar.registerBean(interceptor);
        }
        catch (BeanDefinitionStoreException e) {
          throw new ConfigurationException("Interceptor: [" + interceptor.getName() + "] register error", e);
        }
      }
      HandlerInterceptor instance = this.beanFactory.getBean(interceptor);
      Assert.state(instance != null, "Can't get target interceptor bean");
      ret[i++] = instance;
    }
    return ret;
  }

  /**
   * Rebuild Controllers
   */
  public void rebuildControllers() {
    log.info("Rebuilding Controllers");
    startConfiguration();
  }

  public void setBeanFactory(ConfigurableBeanFactory beanFactory) {
    Assert.notNull(beanFactory, "ConfigurableBeanFactory cannot be null");
    this.beanFactory = beanFactory;
  }

  public void setAnnotationHandlerFactory(AnnotationHandlerFactory annotationHandlerFactory) {
    this.annotationHandlerFactory = annotationHandlerFactory;
  }

  /**
   * Whether to detect handler methods in beans in ancestor ApplicationContexts.
   * <p>Default is "false": Only beans in the current ApplicationContext are
   * considered, i.e. only in the context that this HandlerMapping itself
   * is defined in (typically the current DispatcherServlet's context).
   * <p>Switch this flag on to detect handler beans in ancestor contexts
   * (typically the root WebApplicationContext) as well.
   *
   * @see #getCandidateBeanNames()
   * @since 4.0
   */
  public void setDetectHandlerMethodsInAncestorContexts(boolean detectHandlerMethodsInAncestorContexts) {
    this.detectHandlerMethodsInAncestorContexts = detectHandlerMethodsInAncestorContexts;
  }
}

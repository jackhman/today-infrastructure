/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web.handler.method;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.context.MessageSource;
import cn.taketoday.core.BridgeMethodResolver;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.SynthesizingMethodParameter;
import cn.taketoday.core.conversion.ConversionException;
import cn.taketoday.core.i18n.LocaleContextHolder;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.MapCache;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.annotation.ResponseBody;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.cors.CorsConfiguration;
import cn.taketoday.web.handler.AsyncHandler;
import cn.taketoday.web.handler.DefaultResponseStatus;
import cn.taketoday.web.handler.HandlerWrapper;

/**
 * Encapsulates information about a handler method consisting of a
 * {@linkplain #getMethod() method} and a {@linkplain #getBean() bean}.
 * Provides convenient access to method parameters, the method return value,
 * method annotations, etc.
 *
 * <p>The class may be created with a bean instance or with a bean name
 * (e.g. lazy-init bean, prototype bean). Use {@link #withBean(Object)}}
 * to obtain a {@code HandlerMethod} instance with a bean instance resolved
 * through the associated {@link BeanFactory}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-06-25 20:03:11
 */
public class HandlerMethod implements AsyncHandler {

  /** Logger that is available to subclasses. */
  protected static final Logger log = LoggerFactory.getLogger(HandlerMethod.class);

  static MapCache<AnnotationKey, Boolean, HandlerMethod> methodAnnotationCache = new MapCache<>(128) {
    @Override
    protected Boolean createValue(AnnotationKey key, HandlerMethod handlerMethod) {
      return AnnotatedElementUtils.hasAnnotation(key.method, key.annotationType);
    }
  };

  private final Object bean;

  private final Class<?> beanType;

  /** action **/
  private final Method method;

  private final Method bridgedMethod;

  /** parameter list **/
  private final MethodParameter[] parameters;

  private final String description;

  /** @since 2.3.7 */
  private final Class<?> returnType;

  /**
   * @since 4.0
   */
  @Nullable
  private MethodParameter returnTypeParameter;

  @Nullable
  private final MessageSource messageSource;

  /** @since 4.0 */
  private final boolean responseBody;

  @Nullable
  private HttpStatusCode responseStatus;

  @Nullable
  private String responseStatusReason;

  /** @since 4.0 */
  @Nullable
  private volatile ArrayList<Annotation[][]> interfaceParameterAnnotations;

  /**
   * cors config cache
   *
   * @since 4.0
   */
  @Nullable
  CorsConfiguration corsConfig;

  /**
   * Create an instance from a bean instance and a method.
   */
  public HandlerMethod(Object bean, Method method) {
    Assert.notNull(bean, "Bean is required");
    Assert.notNull(method, "Method is required");
    this.bean = bean;
    this.messageSource = null;
    this.beanType = ClassUtils.getUserClass(bean);
    this.method = method;
    this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
    this.returnType = bridgedMethod.getReturnType();
    ReflectionUtils.makeAccessible(bridgedMethod);
    this.parameters = initMethodParameters();
    this.description = initDescription(beanType, method);
    this.responseBody = computeResponseBody();
    evaluateResponseStatus();
  }

  /**
   * Create an instance from a bean instance, method name, and parameter types.
   *
   * @throws NoSuchMethodException when the method cannot be found
   */
  public HandlerMethod(Object bean, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
    Assert.notNull(bean, "Bean is required");
    Assert.notNull(methodName, "Method name is required");
    this.bean = bean;
    this.messageSource = null;
    this.beanType = ClassUtils.getUserClass(bean);
    this.method = bean.getClass().getMethod(methodName, parameterTypes);
    this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
    this.returnType = bridgedMethod.getReturnType();
    ReflectionUtils.makeAccessible(bridgedMethod);
    this.parameters = initMethodParameters();
    this.description = initDescription(beanType, method);
    this.responseBody = computeResponseBody();
    evaluateResponseStatus();
  }

  /**
   * Create an instance from a bean name, a method, and a {@code BeanFactory}.
   */
  public HandlerMethod(String beanName, BeanFactory beanFactory, Method method) {
    this(beanName, beanFactory, null, method);
  }

  /**
   * Variant of {@link #HandlerMethod(String, BeanFactory, Method)} that
   * also accepts a {@link MessageSource}.
   */
  public HandlerMethod(String beanName, BeanFactory beanFactory, @Nullable MessageSource messageSource, Method method) {
    Assert.notNull(method, "Method is required");
    Assert.hasText(beanName, "Bean name is required");
    Assert.notNull(beanFactory, "BeanFactory is required");

    this.bean = beanFactory.isSingleton(beanName) ? beanFactory.getBean(beanName) : beanName;
    this.method = method;
    this.messageSource = messageSource;
    Class<?> beanType = beanFactory.getType(beanName);
    if (beanType == null) {
      throw new IllegalStateException("Cannot resolve bean type for bean with name '" + beanName + "'");
    }
    this.beanType = ClassUtils.getUserClass(beanType);
    this.bridgedMethod = BridgeMethodResolver.findBridgedMethod(method);
    this.returnType = bridgedMethod.getReturnType();
    ReflectionUtils.makeAccessible(bridgedMethod);
    this.parameters = initMethodParameters();
    this.description = initDescription(this.beanType, method);
    this.responseBody = computeResponseBody();
    evaluateResponseStatus();
  }

  /**
   * Copy constructor for use in subclasses.
   */
  protected HandlerMethod(HandlerMethod handlerMethod) {
    Assert.notNull(handlerMethod, "HandlerMethod is required");
    this.bean = handlerMethod.bean;
    this.messageSource = handlerMethod.messageSource;
    this.method = handlerMethod.method;
    this.beanType = handlerMethod.beanType;
    this.returnType = handlerMethod.returnType;
    this.bridgedMethod = handlerMethod.bridgedMethod;
    this.parameters = handlerMethod.parameters;
    this.responseStatus = handlerMethod.responseStatus;
    this.responseStatusReason = handlerMethod.responseStatusReason;
    this.description = handlerMethod.description;
    this.responseBody = handlerMethod.responseBody;
    this.corsConfig = handlerMethod.corsConfig;
  }

  /**
   * Re-create HandlerMethod with the resolved handler.
   */
  HandlerMethod(HandlerMethod handlerMethod, Object handler) {
    this.bean = handler;
    this.messageSource = handlerMethod.messageSource;
    this.beanType = handlerMethod.beanType;
    this.method = handlerMethod.method;
    this.returnType = handlerMethod.returnType;
    this.bridgedMethod = handlerMethod.bridgedMethod;
    this.parameters = handlerMethod.parameters;
    this.responseStatus = handlerMethod.responseStatus;
    this.responseStatusReason = handlerMethod.responseStatusReason;
    this.description = handlerMethod.description;
    this.responseBody = handlerMethod.responseBody;
    this.corsConfig = handlerMethod.corsConfig;
  }

  private MethodParameter[] initMethodParameters() {
    int count = bridgedMethod.getParameterCount();
    MethodParameter[] result = new MethodParameter[count];
    for (int i = 0; i < count; i++) {
      result[i] = new HandlerMethodParameter(i);
    }
    return result;
  }

  private void evaluateResponseStatus() {
    ResponseStatus annotation = getMethodAnnotation(ResponseStatus.class);
    if (annotation == null) {
      annotation = AnnotatedElementUtils.findMergedAnnotation(getBeanType(), ResponseStatus.class);
    }
    if (annotation != null) {
      String reason = annotation.reason();
      String resolvedReason = StringUtils.hasText(reason) && messageSource != null
                              ? messageSource.getMessage(reason, null, reason, LocaleContextHolder.getLocale())
                              : reason;

      this.responseStatus = annotation.code();
      this.responseStatusReason = resolvedReason;
      if (StringUtils.hasText(resolvedReason) && getMethod().getReturnType() != void.class) {
        log.warn("Return value of [{}] will be ignored since @ResponseStatus 'reason' attribute is set.", getMethod());
      }
    }
  }

  private static String initDescription(Class<?> beanType, Method method) {
    StringJoiner joiner = new StringJoiner(", ", "(", ")");
    for (Class<?> paramType : method.getParameterTypes()) {
      joiner.add(paramType.getSimpleName());
    }
    return beanType.getName() + "#" + method.getName() + joiner;
  }

  // ---- useful methods

  /**
   * Return the bean for this handler method.
   */
  public Object getBean() {
    return this.bean;
  }

  /**
   * Return the method for this handler method.
   */
  public Method getMethod() {
    return this.method;
  }

  /**
   * This method returns the type of the handler for this handler method.
   * <p>Note that if the bean type is a CGLIB-generated class, the original
   * user-defined class is returned.
   */
  public Class<?> getBeanType() {
    return this.beanType;
  }

  /**
   * If the bean method is a bridge method, this method returns the bridged
   * (user-defined) method. Otherwise, it returns the same method as {@link #getMethod()}.
   */
  protected final Method getBridgedMethod() {
    return this.bridgedMethod;
  }

  /**
   * Return the method parameters for this handler method.
   */
  public MethodParameter[] getMethodParameters() {
    return this.parameters;
  }

  /**
   * Returns the number of formal parameters (whether explicitly
   * declared or implicitly declared or neither) for the executable
   * represented by this object.
   *
   * @return The number of formal parameters for the executable this
   * object represents
   * @since 4.0
   */
  public int getParameterCount() {
    return parameters.length;
  }

  /**
   * Return the specified response status, if any.
   *
   * @see ResponseStatus#code()
   */
  @Nullable
  protected HttpStatusCode getResponseStatus() {
    return this.responseStatus;
  }

  /**
   * Return the associated response status reason, if any.
   *
   * @see ResponseStatus#reason()
   */
  @Nullable
  protected String getResponseStatusReason() {
    return this.responseStatusReason;
  }

  /**
   * Return the HandlerMethod return type.
   */
  public MethodParameter getReturnType() {
    MethodParameter returnType = returnTypeParameter;
    if (returnType == null) {
      returnType = new HandlerMethodParameter(-1);
      this.returnTypeParameter = returnType;
    }
    return returnType;
  }

  /**
   * Return the actual return value type.
   */
  public MethodParameter getReturnValueType(@Nullable Object returnValue) {
    return new ReturnValueMethodParameter(returnValue);
  }

  /**
   * Return the actual return type.
   */
  public Class<?> getRawReturnType() {
    return returnType;
  }

  public boolean isReturnTypeAssignableTo(Class<?> superClass) {
    return superClass.isAssignableFrom(returnType);
  }

  public boolean isReturn(Class<?> returnType) {
    return returnType == this.returnType;
  }

  // handleRequest
  // -----------------------------------------

  /**
   * ResponseBody present?
   */
  public boolean isResponseBody() {
    return this.responseBody;
  }

  protected boolean computeResponseBody() {
    MergedAnnotation<ResponseBody> annotation = MergedAnnotations.from(method).get(ResponseBody.class);
    if (annotation.isPresent()) {
      return annotation.getBoolean(MergedAnnotation.VALUE);
    }
    annotation = MergedAnnotations.from(method.getDeclaringClass()).get(ResponseBody.class);
    if (annotation.isPresent()) {
      return annotation.getBoolean(MergedAnnotation.VALUE);
    }
    return false;
  }

  /**
   * Return {@code true} if the method return type is void, {@code false} otherwise.
   */
  public boolean isVoid() {
    return Void.TYPE.equals(returnType);
  }

  /**
   * Return a single annotation on the underlying method traversing its super methods
   * if no annotation can be found on the given method itself.
   * <p>Also supports <em>merged</em> composed annotations with attribute
   * overrides
   *
   * @param annotationType the type of annotation to introspect the method for
   * @return the annotation, or {@code null} if none found
   * @see AnnotatedElementUtils#findMergedAnnotation
   */
  @Nullable
  public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
    return AnnotatedElementUtils.findMergedAnnotation(this.method, annotationType);
  }

  /**
   * Return whether the parameter is declared with the given annotation type.
   *
   * @param annotationType the annotation type to look for
   * @see AnnotatedElementUtils#hasAnnotation
   * @since 4.0
   */
  public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
    return methodAnnotationCache.get(new AnnotationKey(method, annotationType), this);
  }

  @Override
  public ConcurrentResultHandlerMethod wrapConcurrentResult(Object result) {
    return new ConcurrentResultHandlerMethod(new ConcurrentResultMethodParameter(result), this);
  }

  /**
   * create with a new bean
   *
   * @since 4.0
   */
  public HandlerMethod withBean(Object handler) {
    return new HandlerMethod(this, handler);
  }

  /**
   * Return a short representation of this handler method for log message purposes.
   *
   * @since 4.0
   */
  public String getShortLogMessage() {
    return getBeanType().getName() + "#" + this.method.getName() +
            "[" + this.method.getParameterCount() + " args]";
  }

  private ArrayList<Annotation[][]> getInterfaceParameterAnnotations() {
    ArrayList<Annotation[][]> parameterAnnotations = this.interfaceParameterAnnotations;
    if (parameterAnnotations == null) {
      parameterAnnotations = new ArrayList<>();
      for (Class<?> ifc : ClassUtils.getAllInterfacesForClassAsSet(this.method.getDeclaringClass())) {
        for (Method candidate : ifc.getMethods()) {
          if (isOverrideFor(candidate)) {
            parameterAnnotations.add(candidate.getParameterAnnotations());
          }
        }
      }
      this.interfaceParameterAnnotations = parameterAnnotations;
    }
    return parameterAnnotations;
  }

  private boolean isOverrideFor(Method candidate) {
    if (!candidate.getName().equals(this.method.getName()) ||
            candidate.getParameterCount() != this.method.getParameterCount()) {
      return false;
    }
    Class<?>[] paramTypes = this.method.getParameterTypes();
    if (Arrays.equals(candidate.getParameterTypes(), paramTypes)) {
      return true;
    }
    for (int i = 0; i < paramTypes.length; i++) {
      if (paramTypes[i] !=
              ResolvableType.forParameter(candidate, i, this.method.getDeclaringClass()).resolve()) {
        return false;
      }
    }
    return true;
  }

  // Object

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof HandlerMethod otherMethod)) {
      return false;
    }
    return (this.bean.equals(otherMethod.bean) && this.method.equals(otherMethod.method));
  }

  @Override
  public int hashCode() {
    return (this.bean.hashCode() * 31 + this.method.hashCode());
  }

  @Override
  public String toString() {
    return description;
  }

  // Support methods for use in "InvocableHandlerMethod" sub-class variants..

  @Nullable
  protected static Object findProvidedArgument(MethodParameter parameter, @Nullable Object... providedArgs) {
    if (ObjectUtils.isNotEmpty(providedArgs)) {
      for (Object providedArg : providedArgs) {
        if (parameter.getParameterType().isInstance(providedArg)) {
          return providedArg;
        }
      }
    }
    return null;
  }

  protected static String formatArgumentError(ResolvableMethodParameter param, String message) {
    return "Could not resolve parameter [" + param.getParameterIndex() + "] in " +
            param.getMethod().toGenericString() + (StringUtils.hasText(message) ? ": " + message : "");
  }

  /**
   * Assert that the target bean class is an instance of the class where the given
   * method is declared. In some cases the actual controller instance at request-
   * processing time may be a JDK dynamic proxy (lazy initialization, prototype
   * beans, and others). {@code @Controller}'s that require proxying should prefer
   * class-based proxy mechanisms.
   */
  protected void assertTargetBean(Method method, Object targetBean, Object[] args) {
    Class<?> methodDeclaringClass = method.getDeclaringClass();
    Class<?> targetBeanClass = targetBean.getClass();
    if (!methodDeclaringClass.isAssignableFrom(targetBeanClass)) {
      String text = "The mapped handler method class '" + methodDeclaringClass.getName() +
              "' is not an instance of the actual controller bean class '" +
              targetBeanClass.getName() + "'. If the controller requires proxying " +
              "(e.g. due to @Transactional), please use class-based proxying.";
      throw new IllegalStateException(formatInvokeError(text, args));
    }
  }

  protected String formatInvokeError(String text, Object[] args) {
    String formattedArgs = IntStream.range(0, args.length)
            .mapToObj(i -> (args[i] != null ?
                            "[" + i + "] [type=" + args[i].getClass().getName() + "] [value=" + args[i] + "]" :
                            "[" + i + "] [null]"))
            .collect(Collectors.joining(",\n", " ", " "));
    return text + "\n" +
            "Controller [" + getBeanType().getName() + "]\n" +
            "Method [" + getBridgedMethod().toGenericString() + "] " +
            "with argument values:\n" + formattedArgs;
  }

  // ResponseStatus

  public static int getStatusValue(Throwable ex) {
    return getResponseStatus(ex).value().value();
  }

  public static ResponseStatus getResponseStatus(Throwable ex) {
    return getResponseStatus(ex.getClass());
  }

  public static ResponseStatus getResponseStatus(Class<? extends Throwable> exceptionClass) {
    if (ConversionException.class.isAssignableFrom(exceptionClass)) {
      return new DefaultResponseStatus(HttpStatus.BAD_REQUEST);
    }
    ResponseStatus status = AnnotationUtils.getAnnotation(exceptionClass, ResponseStatus.class);
    if (status != null) {
      return new DefaultResponseStatus(status);
    }
    return new DefaultResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  // HandlerMethod

  @Nullable
  public static HandlerMethod unwrap(@Nullable Object handler) {
    if (handler instanceof HandlerMethod) {
      return (HandlerMethod) handler;
    }
    else if (handler instanceof ActionMappingAnnotationHandler annotationHandler) {
      return annotationHandler.getMethod();
    }
    else if (handler instanceof HandlerWrapper wrapper
            && wrapper.getRawHandler() instanceof HandlerMethod target) {
      return target;
    }
    return null;
  }

  protected static class ConcurrentResultHandlerMethod extends HandlerMethod {
    private final HandlerMethod target;

    private final MethodParameter returnType;

    public ConcurrentResultHandlerMethod(ConcurrentResultMethodParameter returnType, HandlerMethod target) {
      super(target);
      this.target = target;
      this.returnType = returnType;
    }

    /**
     * Bridge to actual controller type-level annotations.
     */
    @Override
    public Class<?> getBeanType() {
      return target.getBeanType();
    }

    /**
     * Bridge to actual return value or generic type within the declared
     * async return type, e.g. Foo instead of {@code DeferredResult<Foo>}.
     */
    @Override
    public MethodParameter getReturnValueType(@Nullable Object returnValue) {
      return this.returnType;
    }

    @Override
    public MethodParameter getReturnType() {
      return returnType;
    }

    /**
     * Bridge to controller method-level annotations.
     */
    @Override
    public <A extends Annotation> A getMethodAnnotation(Class<A> annotationType) {
      return target.getMethodAnnotation(annotationType);
    }

    /**
     * Bridge to controller method-level annotations.
     */
    @Override
    public <A extends Annotation> boolean hasMethodAnnotation(Class<A> annotationType) {
      return target.hasMethodAnnotation(annotationType);
    }

    @Override
    public boolean isReturn(Class<?> returnType) {
      return this.returnType.getParameterType() == returnType;
    }

    @Override
    public boolean isReturnTypeAssignableTo(Class<?> superClass) {
      return superClass.isAssignableFrom(returnType.getParameterType());
    }

  }

  /**
   * MethodParameter subclass based on the actual return value type or if
   * that's null falling back on the generic type within the declared async
   * return type, e.g. Foo instead of {@code DeferredResult<Foo>}.
   */
  private class ConcurrentResultMethodParameter extends HandlerMethodParameter {

    @Nullable
    private final Object returnValue;

    private final ResolvableType returnType;

    public ConcurrentResultMethodParameter(Object returnValue) {
      super(-1);
      this.returnValue = returnValue;
      this.returnType = returnValue instanceof ReactiveTypeHandler.CollectedValuesList list
                        ? list.getReturnType()
                        : ResolvableType.forType(super.getGenericParameterType()).getGeneric();
    }

    public ConcurrentResultMethodParameter(ConcurrentResultMethodParameter original) {
      super(original);
      this.returnValue = original.returnValue;
      this.returnType = original.returnType;
    }

    @Override
    public Class<?> getParameterType() {
      if (this.returnValue != null) {
        return this.returnValue.getClass();
      }
      if (!ResolvableType.NONE.equals(this.returnType)) {
        return this.returnType.toClass();
      }
      return super.getParameterType();
    }

    @Override
    public Type getGenericParameterType() {
      return this.returnType.getType();
    }

    @Override
    public <T extends Annotation> boolean hasMethodAnnotation(Class<T> annotationType) {
      // Ensure @ResponseBody-style handling for values collected from a reactive type
      // even if actual return type is ResponseEntity<Flux<T>>
      return (super.hasMethodAnnotation(annotationType)
              || (annotationType == ResponseBody.class &&
              this.returnValue instanceof ReactiveTypeHandler.CollectedValuesList));
    }

    @Override
    public ConcurrentResultMethodParameter clone() {
      return new ConcurrentResultMethodParameter(this);
    }
  }

  /**
   * A MethodParameter with HandlerMethod-specific behavior.
   */
  protected class HandlerMethodParameter extends SynthesizingMethodParameter {

    @Nullable
    private volatile Annotation[] combinedAnnotations;

    public HandlerMethodParameter(int index) {
      super(method, index);
    }

    protected HandlerMethodParameter(HandlerMethodParameter original) {
      super(original);
    }

    @Override
    public Method getMethod() {
      return HandlerMethod.this.bridgedMethod;
    }

    @Override
    public Class<?> getContainingClass() {
      return HandlerMethod.this.getBeanType();
    }

    @Override
    public <T extends Annotation> T getMethodAnnotation(Class<T> annotationType) {
      return HandlerMethod.this.getMethodAnnotation(annotationType);
    }

    @Override
    public <T extends Annotation> boolean hasMethodAnnotation(Class<T> annotationType) {
      return HandlerMethod.this.hasMethodAnnotation(annotationType);
    }

    @Override
    public Annotation[] getParameterAnnotations() {
      Annotation[] anns = this.combinedAnnotations;
      if (anns == null) {
        anns = super.getParameterAnnotations();
        int index = getParameterIndex();
        if (index >= 0) {
          for (Annotation[][] ifcAnns : getInterfaceParameterAnnotations()) {
            if (index < ifcAnns.length) {
              Annotation[] paramAnns = ifcAnns[index];
              if (paramAnns.length > 0) {
                ArrayList<Annotation> merged = new ArrayList<>(anns.length + paramAnns.length);
                CollectionUtils.addAll(merged, anns);
                for (Annotation paramAnn : paramAnns) {
                  boolean existingType = false;
                  for (Annotation ann : anns) {
                    if (ann.annotationType() == paramAnn.annotationType()) {
                      existingType = true;
                      break;
                    }
                  }
                  if (!existingType) {
                    merged.add(adaptAnnotation(paramAnn));
                  }
                }
                anns = merged.toArray(Constant.EMPTY_ANNOTATIONS);
              }
            }
          }
        }
        this.combinedAnnotations = anns;
      }
      return anns;
    }

    @Override
    public HandlerMethodParameter clone() {
      return new HandlerMethodParameter(this);
    }

    @Override
    public String toString() {
      return getParameterType().getSimpleName() + " " + getParameterName();
    }

  }

  /**
   * A MethodParameter for a HandlerMethod return type based on an actual return value.
   */
  private class ReturnValueMethodParameter extends HandlerMethodParameter {

    @Nullable
    private final Class<?> returnValueType;

    public ReturnValueMethodParameter(@Nullable Object returnValue) {
      super(-1);
      this.returnValueType = returnValue != null ? returnValue.getClass() : null;
    }

    protected ReturnValueMethodParameter(ReturnValueMethodParameter original) {
      super(original);
      this.returnValueType = original.returnValueType;
    }

    @Override
    public Class<?> getParameterType() {
      return returnValueType != null ? returnValueType : super.getParameterType();
    }

    @Override
    public ReturnValueMethodParameter clone() {
      return new ReturnValueMethodParameter(this);
    }

  }

  static final class AnnotationKey {

    private final int hash;

    public final Method method;

    public final Class<? extends Annotation> annotationType;

    AnnotationKey(Method method, Class<? extends Annotation> annotationType) {
      this.method = method;
      this.annotationType = annotationType;
      this.hash = Objects.hash(method, annotationType);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof AnnotationKey annotationKey))
        return false;
      return hash == annotationKey.hash
              && Objects.equals(method, annotationKey.method)
              && Objects.equals(annotationType, annotationKey.annotationType);
    }

    @Override
    public int hashCode() {
      return this.hash;
    }

  }
}

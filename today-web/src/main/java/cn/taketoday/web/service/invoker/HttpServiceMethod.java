/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.service.invoker;

import org.reactivestreams.Publisher;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.core.ReactiveAdapter;
import cn.taketoday.core.ReactiveAdapterRegistry;
import cn.taketoday.core.StringValueResolver;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.SynthesizingMethodParameter;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.service.annotation.HttpExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Implements the invocation of an {@link HttpExchange @HttpExchange}-annotated,
 * {@link HttpServiceProxyFactory#createClient(Class) HTTP service proxy} method
 * by delegating to an {@link HttpClientAdapter} to perform actual requests.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class HttpServiceMethod {

  private final Method method;

  private final MethodParameter[] parameters;

  private final List<HttpServiceArgumentResolver> argumentResolvers;

  private final HttpRequestValuesInitializer requestValuesInitializer;

  private final ResponseFunction responseFunction;

  HttpServiceMethod(
          Method method, Class<?> containingClass, List<HttpServiceArgumentResolver> argumentResolvers,
          HttpClientAdapter client, @Nullable StringValueResolver embeddedValueResolver,
          ReactiveAdapterRegistry reactiveRegistry, Duration blockTimeout) {

    this.method = method;
    this.argumentResolvers = argumentResolvers;
    this.parameters = initMethodParameters(method);
    this.responseFunction = ResponseFunction.create(client, method, reactiveRegistry, blockTimeout);
    this.requestValuesInitializer = HttpRequestValuesInitializer.create(method, containingClass, embeddedValueResolver);
  }

  private static MethodParameter[] initMethodParameters(Method method) {
    int count = method.getParameterCount();
    if (count == 0) {
      return new MethodParameter[0];
    }
    ParameterNameDiscoverer nameDiscoverer = ParameterNameDiscoverer.getSharedInstance();
    MethodParameter[] parameters = new MethodParameter[count];
    for (int i = 0; i < count; i++) {
      var parameter = new SynthesizingMethodParameter(method, i);
      parameter.initParameterNameDiscovery(nameDiscoverer);
      parameters[i] = parameter;
    }
    return parameters;
  }

  public Method getMethod() {
    return this.method;
  }

  @Nullable
  public Object invoke(Object[] arguments) {
    var requestValues = requestValuesInitializer.initializeRequestValuesBuilder();
    applyArguments(requestValues, arguments);
    return responseFunction.execute(requestValues.build());
  }

  private void applyArguments(HttpRequestValues.Builder requestValues, Object[] arguments) {
    MethodParameter[] parameters = this.parameters;
    Assert.isTrue(arguments.length == parameters.length, "Method argument mismatch");
    for (int i = 0; i < arguments.length; i++) {
      Object value = arguments[i];
      boolean resolved = false;
      for (HttpServiceArgumentResolver resolver : this.argumentResolvers) {
        if (resolver.resolve(value, parameters[i], requestValues)) {
          resolved = true;
          break;
        }
      }
      Assert.state(resolved, formatArgumentError(parameters[i], "No suitable resolver"));
    }
  }

  private static String formatArgumentError(MethodParameter param, String message) {
    return "Could not resolve parameter [" + param.getParameterIndex() + "] in " +
            param.getExecutable().toGenericString() + (StringUtils.hasText(message) ? ": " + message : "");
  }

  /**
   * Factory for {@link HttpRequestValues} with values extracted from the type
   * and method-level {@link HttpExchange @HttpRequest} annotations.
   */
  private record HttpRequestValuesInitializer(
          @Nullable HttpMethod httpMethod, @Nullable String url,
          @Nullable MediaType contentType, @Nullable List<MediaType> acceptMediaTypes) {

    public HttpRequestValues.Builder initializeRequestValuesBuilder() {
      HttpRequestValues.Builder requestValues = HttpRequestValues.builder();
      if (this.httpMethod != null) {
        requestValues.setHttpMethod(this.httpMethod);
      }
      if (this.url != null) {
        requestValues.setUriTemplate(this.url);
      }
      if (this.contentType != null) {
        requestValues.setContentType(this.contentType);
      }
      if (this.acceptMediaTypes != null) {
        requestValues.setAccept(this.acceptMediaTypes);
      }
      return requestValues;
    }

    /**
     * Introspect the method and create the request factory for it.
     */
    public static HttpRequestValuesInitializer create(Method method,
            Class<?> containingClass, @Nullable StringValueResolver embeddedValueResolver) {

      HttpExchange annot1 = AnnotatedElementUtils.findMergedAnnotation(containingClass, HttpExchange.class);
      HttpExchange annot2 = AnnotatedElementUtils.findMergedAnnotation(method, HttpExchange.class);

      Assert.notNull(annot2, "Expected HttpRequest annotation");

      HttpMethod httpMethod = initHttpMethod(annot1, annot2);
      String url = initUrl(annot1, annot2, embeddedValueResolver);
      MediaType contentType = initContentType(annot1, annot2);
      List<MediaType> acceptableMediaTypes = initAccept(annot1, annot2);

      return new HttpRequestValuesInitializer(httpMethod, url, contentType, acceptableMediaTypes);
    }

    @Nullable
    private static HttpMethod initHttpMethod(@Nullable HttpExchange typeAnnot, HttpExchange annot) {

      String value1 = (typeAnnot != null ? typeAnnot.method() : null);
      String value2 = annot.method();

      if (StringUtils.hasText(value2)) {
        return HttpMethod.valueOf(value2);
      }

      if (StringUtils.hasText(value1)) {
        return HttpMethod.valueOf(value1);
      }

      return null;
    }

    @Nullable
    private static String initUrl(@Nullable HttpExchange typeAnnot,
            HttpExchange annot, @Nullable StringValueResolver embeddedValueResolver) {

      String url1 = (typeAnnot != null ? typeAnnot.url() : null);
      String url2 = annot.url();

      if (embeddedValueResolver != null) {
        url1 = (url1 != null ? embeddedValueResolver.resolveStringValue(url1) : null);
        url2 = embeddedValueResolver.resolveStringValue(url2);
      }

      boolean hasUrl1 = StringUtils.hasText(url1);
      boolean hasUrl2 = StringUtils.hasText(url2);

      if (hasUrl1 && hasUrl2) {
        return (url1 + (!url1.endsWith("/") && !url2.startsWith("/") ? "/" : "") + url2);
      }

      if (!hasUrl1 && !hasUrl2) {
        return null;
      }

      return (hasUrl2 ? url2 : url1);
    }

    @Nullable
    private static MediaType initContentType(@Nullable HttpExchange typeAnnot, HttpExchange annot) {

      String value1 = (typeAnnot != null ? typeAnnot.contentType() : null);
      String value2 = annot.contentType();

      if (StringUtils.hasText(value2)) {
        return MediaType.parseMediaType(value2);
      }

      if (StringUtils.hasText(value1)) {
        return MediaType.parseMediaType(value1);
      }

      return null;
    }

    @Nullable
    private static List<MediaType> initAccept(@Nullable HttpExchange typeAnnot, HttpExchange annot) {

      String[] value1 = (typeAnnot != null ? typeAnnot.accept() : null);
      String[] value2 = annot.accept();

      if (!ObjectUtils.isEmpty(value2)) {
        return MediaType.parseMediaTypes(Arrays.asList(value2));
      }

      if (!ObjectUtils.isEmpty(value1)) {
        return MediaType.parseMediaTypes(Arrays.asList(value1));
      }

      return null;
    }

  }

  /**
   * Function to execute a request, obtain a response, and adapt to the expected
   * return type, blocking if necessary.
   */
  private record ResponseFunction(
          Function<HttpRequestValues, Publisher<?>> responseFunction,
          @Nullable ReactiveAdapter returnTypeAdapter,
          boolean blockForOptional, Duration blockTimeout) {

    @Nullable
    public Object execute(HttpRequestValues requestValues) {
      Publisher<?> responsePublisher = this.responseFunction.apply(requestValues);
      if (returnTypeAdapter != null) {
        return returnTypeAdapter.fromPublisher(responsePublisher);
      }

      return blockForOptional ?
             ((Mono<?>) responsePublisher).blockOptional(blockTimeout) :
             ((Mono<?>) responsePublisher).block(blockTimeout);
    }

    /**
     * Create the {@code ResponseFunction} that matches the method's return type.
     */
    public static ResponseFunction create(HttpClientAdapter client,
            Method method, ReactiveAdapterRegistry reactiveRegistry, Duration blockTimeout) {

      MethodParameter returnParam = new MethodParameter(method, -1);
      Class<?> returnType = returnParam.getParameterType();
      ReactiveAdapter reactiveAdapter = reactiveRegistry.getAdapter(returnType);

      MethodParameter actualParam = (reactiveAdapter != null ? returnParam.nested() : returnParam.nestedIfOptional());
      Class<?> actualType = actualParam.getNestedParameterType();

      Function<HttpRequestValues, Publisher<?>> responseFunction;
      if (actualType.equals(void.class) || actualType.equals(Void.class)) {
        responseFunction = client::requestToVoid;
      }
      else if (reactiveAdapter != null && reactiveAdapter.isNoValue()) {
        responseFunction = client::requestToVoid;
      }
      else if (actualType.equals(HttpHeaders.class)) {
        responseFunction = client::requestToHeaders;
      }
      else if (actualType.equals(ResponseEntity.class)) {
        MethodParameter bodyParam = actualParam.nested();
        Class<?> bodyType = bodyParam.getNestedParameterType();
        if (bodyType.equals(Void.class)) {
          responseFunction = client::requestToBodilessEntity;
        }
        else {
          ReactiveAdapter bodyAdapter = reactiveRegistry.getAdapter(bodyType);
          responseFunction = initResponseEntityFunction(client, bodyParam, bodyAdapter);
        }
      }
      else {
        responseFunction = initBodyFunction(client, actualParam, reactiveAdapter);
      }

      boolean blockForOptional = returnType.equals(Optional.class);
      return new ResponseFunction(responseFunction, reactiveAdapter, blockForOptional, blockTimeout);
    }

    @SuppressWarnings("ConstantConditions")
    private static Function<HttpRequestValues, Publisher<?>> initResponseEntityFunction(
            HttpClientAdapter client, MethodParameter methodParam, @Nullable ReactiveAdapter reactiveAdapter) {

      if (reactiveAdapter == null) {
        return request -> client.requestToEntity(
                request, ParameterizedTypeReference.forType(methodParam.getNestedGenericParameterType()));
      }

      Assert.isTrue(reactiveAdapter.isMultiValue(),
              "ResponseEntity body must be a concrete value or a multi-value Publisher");

      ParameterizedTypeReference<?> bodyType =
              ParameterizedTypeReference.forType(methodParam.nested().getNestedGenericParameterType());

      // Shortcut for Flux
      if (reactiveAdapter.getReactiveType().equals(Flux.class)) {
        return request -> client.requestToEntityFlux(request, bodyType);
      }

      return request -> client.requestToEntityFlux(request, bodyType)
              .map(entity -> {
                Object body = reactiveAdapter.fromPublisher(entity.getBody());
                return new ResponseEntity<>(body, entity.getHeaders(), entity.getStatusCode());
              });
    }

    private static Function<HttpRequestValues, Publisher<?>> initBodyFunction(
            HttpClientAdapter client, MethodParameter methodParam, @Nullable ReactiveAdapter reactiveAdapter) {

      ParameterizedTypeReference<?> bodyType =
              ParameterizedTypeReference.forType(methodParam.getNestedGenericParameterType());

      return reactiveAdapter != null && reactiveAdapter.isMultiValue()
             ? request -> client.requestToBodyFlux(request, bodyType)
             : request -> client.requestToBody(request, bodyType);
    }

  }

}

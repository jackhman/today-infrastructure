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

package cn.taketoday.web.bind.resolver;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import cn.taketoday.core.GenericTypeResolver;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.TypeReference;
import cn.taketoday.core.io.InputStreamResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceRegion;
import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpRange;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ProblemDetail;
import cn.taketoday.http.converter.GenericHttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.http.converter.HttpMessageNotWritableException;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.LogFormatUtils;
import cn.taketoday.util.MimeTypeUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.HttpMediaTypeNotAcceptableException;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.ReturnValueHandler;
import cn.taketoday.web.ServletDetector;
import cn.taketoday.web.accept.ContentNegotiationManager;
import cn.taketoday.web.handler.method.ActionMappingAnnotationHandler;
import cn.taketoday.web.servlet.ServletUtils;
import cn.taketoday.web.util.UriUtils;
import cn.taketoday.web.util.pattern.PathPattern;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Extends {@link AbstractMessageConverterMethodArgumentResolver} with the ability to handle method
 * return values by writing to the response with {@link HttpMessageConverter HttpMessageConverters}.
 * <p>
 * write {@link ActionMappingAnnotationHandler} return value
 * </p>
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see ActionMappingAnnotationHandler
 * @since 4.0 2022/1/23 12:30
 */
public abstract class AbstractMessageConverterMethodProcessor
        extends AbstractMessageConverterMethodArgumentResolver implements ReturnValueHandler {
  private static final Logger log = LoggerFactory.getLogger(AbstractMessageConverterMethodProcessor.class);

  /* Extensions associated with the built-in message converters */
  private static final Set<String> SAFE_EXTENSIONS = Set.of(
          "txt", "text", "yml", "properties", "csv",
          "json", "xml", "atom", "rss",
          "png", "jpe", "jpeg", "jpg", "gif", "wbmp", "bmp"
  );

  private static final Set<String> SAFE_MEDIA_BASE_TYPES = Set.of("audio", "image", "video");
  private static final List<MediaType> ALL_APPLICATION_MEDIA_TYPES = List.of(MediaType.ALL, new MediaType("application"));
  private static final Type RESOURCE_REGION_LIST_TYPE =
          new TypeReference<List<ResourceRegion>>() { }.getType();

  private final ContentNegotiationManager contentNegotiationManager;

  private final List<MediaType> problemMediaTypes =
          Arrays.asList(MediaType.APPLICATION_PROBLEM_JSON, MediaType.APPLICATION_PROBLEM_XML);

  private final HashSet<String> safeExtensions = new HashSet<>();

  /**
   * Constructor with list of converters only.
   */
  protected AbstractMessageConverterMethodProcessor(List<HttpMessageConverter<?>> converters) {
    this(converters, null, null);
  }

  /**
   * Constructor with list of converters and ContentNegotiationManager.
   */
  protected AbstractMessageConverterMethodProcessor(List<HttpMessageConverter<?>> converters,
          @Nullable ContentNegotiationManager contentNegotiationManager) {
    this(converters, contentNegotiationManager, null);
  }

  /**
   * Constructor with list of converters and ContentNegotiationManager as well
   * as request/response body advice instances.
   */
  protected AbstractMessageConverterMethodProcessor(List<HttpMessageConverter<?>> converters,
          @Nullable ContentNegotiationManager manager, @Nullable List<Object> requestResponseBodyAdvice) {
    super(converters, requestResponseBodyAdvice);
    this.contentNegotiationManager = manager != null ? manager : new ContentNegotiationManager();
    this.safeExtensions.addAll(contentNegotiationManager.getAllFileExtensions());
    this.safeExtensions.addAll(SAFE_EXTENSIONS);
  }

  // ReturnValueHandler

  /**
   * Writes the given return type to the given output message.
   *
   * @param value the value to write to the output message
   * @param returnType the type of the value
   * @param context the output message to write to and Used to inspect the {@code Accept} header.
   * @throws java.io.IOException thrown in case of I/O errors
   * @throws cn.taketoday.web.HttpMediaTypeNotAcceptableException thrown when the conditions indicated
   * by the {@code Accept} header on the request cannot be met by the message converters
   * @throws cn.taketoday.http.converter.HttpMessageNotWritableException thrown if a given message cannot
   * be written by a converter, or if the content-type chosen by the server
   * has no compatible converter.
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected <T> void writeWithMessageConverters(@Nullable T value, @Nullable MethodParameter returnType, RequestContext context)
          throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {

    Object body;
    Type targetType;
    Class<?> valueType;

    if (value instanceof CharSequence) {
      body = value.toString();
      valueType = String.class;
      targetType = String.class;
    }
    else {
      body = value;
      valueType = getReturnValueType(body, returnType);
      if (returnType == null) {
        targetType = ResolvableType.forInstance(body).getType();
      }
      else {
        targetType = GenericTypeResolver.resolveType(getGenericType(returnType), returnType.getContainingClass());
      }
    }

    HttpHeaders responseHeaders = context.responseHeaders();
    if (isResourceType(value, returnType)) {
      responseHeaders.set(HttpHeaders.ACCEPT_RANGES, "bytes");
      if (value != null) {
        String headerRange = context.requestHeaders().getFirst(HttpHeaders.RANGE);
        if (headerRange != null && context.getStatus() == 200) {
          Resource resource = (Resource) value;
          try {
            List<HttpRange> httpRanges = HttpRange.parseRanges(headerRange);
            context.setStatus(HttpStatus.PARTIAL_CONTENT.value());
            body = HttpRange.toResourceRegions(httpRanges, resource);
            valueType = body.getClass();
            targetType = RESOURCE_REGION_LIST_TYPE;
          }
          catch (IllegalArgumentException ex) {
            responseHeaders.set(HttpHeaders.CONTENT_RANGE, "bytes */" + resource.contentLength());
            context.setStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());
          }
        }
      }
    }

    MediaType selectedMediaType = null;
    MediaType contentType = responseHeaders.getContentType();
    boolean isContentTypePreset = contentType != null && contentType.isConcrete();
    if (isContentTypePreset) {
      if (log.isDebugEnabled()) {
        log.debug("Found 'Content-Type:{}' in response", contentType);
      }
      selectedMediaType = contentType;
    }
    else {
      List<MediaType> acceptableTypes;
      try {
        acceptableTypes = getAcceptableMediaTypes(context);
      }
      catch (HttpMediaTypeNotAcceptableException ex) {
        int series = context.getStatus() / 100;
        if (body == null || series == 4 || series == 5) {
          if (log.isDebugEnabled()) {
            log.debug("Ignoring error response content (if any). {}", ex.toString());
          }
          return;
        }
        throw ex;
      }

      var producibleTypes = getProducibleMediaTypes(context, valueType, targetType);
      if (body != null && producibleTypes.isEmpty()) {
        throw new HttpMessageNotWritableException(
                "No converter found for return value of type: " + valueType);
      }

      ArrayList<MediaType> compatibleMediaTypes = new ArrayList<>();

      determineCompatibleMediaTypes(acceptableTypes, producibleTypes, compatibleMediaTypes);

      // For ProblemDetail, fall back on RFC 7807 format
      if (compatibleMediaTypes.isEmpty() && ProblemDetail.class.isAssignableFrom(valueType)) {
        determineCompatibleMediaTypes(this.problemMediaTypes, producibleTypes, compatibleMediaTypes);
      }

      if (compatibleMediaTypes.isEmpty()) {
        if (log.isDebugEnabled()) {
          log.debug("No match for {}, supported: {}", acceptableTypes, producibleTypes);
        }
        if (body != null) {
          throw new HttpMediaTypeNotAcceptableException(producibleTypes);
        }
        return;
      }

      MimeTypeUtils.sortBySpecificity(compatibleMediaTypes);

      for (MediaType mediaType : compatibleMediaTypes) {
        if (mediaType.isConcrete()) {
          selectedMediaType = mediaType;
          break;
        }
        else if (mediaType.isPresentIn(ALL_APPLICATION_MEDIA_TYPES)) {
          selectedMediaType = MediaType.APPLICATION_OCTET_STREAM;
          break;
        }
      }

      if (log.isDebugEnabled()) {
        log.debug("Using '{}', given {} and supported {}", selectedMediaType, acceptableTypes, producibleTypes);
      }
    }

    if (selectedMediaType != null) {
      RequestResponseBodyAdviceChain advice = getAdvice();
      selectedMediaType = selectedMediaType.removeQualityValue();
      for (HttpMessageConverter<?> converter : messageConverters) {
        var generic = converter instanceof GenericHttpMessageConverter
                      ? (GenericHttpMessageConverter) converter : null;
        if (generic != null ? generic.canWrite(targetType, valueType, selectedMediaType)
                            : converter.canWrite(valueType, selectedMediaType)) {

          body = advice.beforeBodyWrite(body, returnType, selectedMediaType, converter, context);
          if (body != null) {
            if (log.isDebugEnabled()) {
              Object theBody = body;
              LogFormatUtils.traceDebug(log,
                      traceOn -> "Writing [" + LogFormatUtils.formatValue(theBody, !traceOn) + "]");
            }
            addContentDispositionHeader(context);
            if (generic != null) {
              generic.write(body, targetType, selectedMediaType, context.asHttpOutputMessage());
            }
            else {
              ((HttpMessageConverter) converter).write(
                      body, selectedMediaType, context.asHttpOutputMessage());
            }
          }
          else if (log.isDebugEnabled()) {
            log.debug("Nothing to write: null body");
          }
          return;
        }
      }
    }

    if (body != null) {
      HandlerMatchingMetadata matchingMetadata = context.getMatchingMetadata();
      if (matchingMetadata != null) {
        MediaType[] producibleMediaTypes = matchingMetadata.getProducibleMediaTypes();
        if (isContentTypePreset || ObjectUtils.isNotEmpty(producibleMediaTypes)) {
          throw new HttpMessageNotWritableException(
                  "No converter for [" + valueType + "] with preset Content-Type '" + contentType + "'");
        }
      }
      throw new HttpMediaTypeNotAcceptableException(getSupportedMediaTypes(body.getClass()));
    }
  }

  /**
   * Return the type of the value to be written to the response. Typically this is
   * a simple check via getClass on the value but if the value is null, then the
   * return type needs to be examined possibly including generic type determination
   * (e.g. {@code ResponseEntity<T>}).
   */
  protected Class<?> getReturnValueType(@Nullable Object value, @Nullable MethodParameter returnType) {
    if (value != null) {
      return value.getClass();
    }
    if (returnType != null) {
      return returnType.getParameterType();
    }
    throw new IllegalStateException("return-value and return-type must not be null at same time");
  }

  /**
   * Return whether the returned value or the declared return type extends {@link Resource}.
   */
  protected boolean isResourceType(@Nullable Object value, @Nullable MethodParameter returnType) {
    Class<?> clazz = getReturnValueType(value, returnType);
    return clazz != InputStreamResource.class && Resource.class.isAssignableFrom(clazz);
  }

  /**
   * Return the generic type of the {@code returnType} (or of the nested type
   * if it is an {@link HttpEntity}).
   */
  private Type getGenericType(MethodParameter returnType) {
    if (HttpEntity.class.isAssignableFrom(returnType.getParameterType())) {
      return ResolvableType.forType(returnType.getGenericParameterType()).getGeneric().getType();
    }
    else {
      return returnType.getGenericParameterType();
    }
  }

  /**
   * Returns the media types that can be produced.
   *
   * @see #getProducibleMediaTypes(RequestContext, Class, Type)
   */
  @SuppressWarnings("unused")
  protected List<MediaType> getProducibleMediaTypes(RequestContext request, Class<?> valueClass) {
    return getProducibleMediaTypes(request, valueClass, null);
  }

  /**
   * Returns the media types that can be produced. The resulting media types are:
   * <ul>
   * <li>The producible media types specified in the request mappings, or
   * <li>Media types of configured converters that can write the specific return value, or
   * <li>{@link MediaType#ALL}
   * </ul>
   */
  protected List<MediaType> getProducibleMediaTypes(
          RequestContext request, Class<?> valueClass, @Nullable Type targetType) {

    HandlerMatchingMetadata matchingMetadata = request.getMatchingMetadata();
    if (matchingMetadata != null) {
      MediaType[] mediaTypes = matchingMetadata.getProducibleMediaTypes();
      if (ObjectUtils.isNotEmpty(mediaTypes)) {
        return Arrays.asList(mediaTypes);
      }
    }
    LinkedHashSet<MediaType> result = new LinkedHashSet<>();
    for (HttpMessageConverter<?> converter : messageConverters) {
      if (converter instanceof GenericHttpMessageConverter<?> generic && targetType != null) {
        if (generic.canWrite(targetType, valueClass, null)) {
          result.addAll(converter.getSupportedMediaTypes(valueClass));
        }
      }
      else if (converter.canWrite(valueClass, null)) {
        result.addAll(converter.getSupportedMediaTypes(valueClass));
      }
    }
    return result.isEmpty() ? Collections.singletonList(MediaType.ALL) : new ArrayList<>(result);
  }

  private List<MediaType> getAcceptableMediaTypes(RequestContext request)
          throws HttpMediaTypeNotAcceptableException {
    return this.contentNegotiationManager.resolveMediaTypes(request);
  }

  private void determineCompatibleMediaTypes(
          List<MediaType> acceptableTypes, List<MediaType> producibleTypes, List<MediaType> mediaTypesToUse) {

    for (MediaType requestedType : acceptableTypes) {
      for (MediaType producibleType : producibleTypes) {
        if (requestedType.isCompatibleWith(producibleType)) {
          mediaTypesToUse.add(getMostSpecificMediaType(requestedType, producibleType));
        }
      }
    }
  }

  /**
   * Return the more specific of the acceptable and the producible media types
   * with the q-value of the former.
   */
  private MediaType getMostSpecificMediaType(MediaType acceptType, MediaType produceType) {
    MediaType produceTypeToUse = produceType.copyQualityValue(acceptType);
    if (acceptType.isLessSpecific(produceTypeToUse)) {
      return produceTypeToUse;
    }
    else {
      return acceptType;
    }
  }

  /**
   * Check if the path has a file extension and whether the extension is either
   * on the list of {@link #SAFE_EXTENSIONS safe extensions} or explicitly
   * {@link cn.taketoday.web.accept.ContentNegotiationManager#getAllFileExtensions() registered}.
   * If not, and the status is in the 2xx range, a 'Content-Disposition'
   * header with a safe attachment file name ("f.txt") is added to prevent
   * RFD exploits.
   */
  private void addContentDispositionHeader(RequestContext request) {
    HttpHeaders headers = request.responseHeaders();
    if (headers.containsKey(HttpHeaders.CONTENT_DISPOSITION)) {
      return;
    }

    try {
      int status = request.getStatus();
      if (status < 200 || (status > 299 && status < 400)) {
        return;
      }
    }
    catch (Throwable ex) {
      // ignore
    }
    String requestUri = request.getRequestURI();

    int index = requestUri.lastIndexOf('/') + 1;
    String filename = requestUri.substring(index);
    String pathParams = "";

    index = filename.indexOf(';');
    if (index != -1) {
      pathParams = filename.substring(index);
      filename = filename.substring(0, index);
    }

    filename = UriUtils.decode(filename, StandardCharsets.UTF_8);
    String ext = StringUtils.getFilenameExtension(filename);

    pathParams = UriUtils.decode(pathParams, StandardCharsets.UTF_8);
    String extInPathParams = StringUtils.getFilenameExtension(pathParams);

    if (notSafeExtension(request, ext) || notSafeExtension(request, extInPathParams)) {
      headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=f.txt");
    }
  }

  private boolean notSafeExtension(RequestContext request, @Nullable String extension) {
    if (StringUtils.isBlank(extension)) {
      return false;
    }
    extension = extension.toLowerCase(Locale.ENGLISH);
    if (safeExtensions.contains(extension)) {
      return false;
    }

    HandlerMatchingMetadata matchingMetadata = request.getMatchingMetadata();
    if (matchingMetadata != null) {
      PathPattern bestMatchingPattern = matchingMetadata.getBestMatchingPattern();
      if (bestMatchingPattern != null && bestMatchingPattern.getPatternString().endsWith("." + extension)) {
        return false;
      }
      if (extension.equals("html")) {
        MediaType[] mediaTypes = matchingMetadata.getProducibleMediaTypes();
        if (ObjectUtils.isNotEmpty(mediaTypes) && ObjectUtils.containsElement(mediaTypes, MediaType.TEXT_HTML)) {
          return false;
        }
      }
    }

    MediaType mediaType = resolveMediaType(request, extension);
    return mediaType == null || !safeMediaType(mediaType);
  }

  @Nullable
  private MediaType resolveMediaType(RequestContext request, String extension) {
    MediaType result = null;
    if (ServletDetector.runningInServlet(request)) {
      String rawMimeType = ServletDelegate.getMimeType(request, extension);
      if (StringUtils.hasText(rawMimeType)) {
        result = MediaType.parseMediaType(rawMimeType);
      }
    }
    if (result == null || MediaType.APPLICATION_OCTET_STREAM.equals(result)) {
      result = MediaType.fromFileName("file." + extension);
    }
    return result;
  }

  private boolean safeMediaType(MediaType mediaType) {
    return SAFE_MEDIA_BASE_TYPES.contains(mediaType.getType())
            || mediaType.getSubtype().endsWith("+xml");
  }

  static class ServletDelegate {

    static String getMimeType(RequestContext request, String extension) {
      HttpServletRequest servletRequest = ServletUtils.getServletRequest(request);
      return servletRequest.getServletContext().getMimeType("file." + extension);
    }

  }

}

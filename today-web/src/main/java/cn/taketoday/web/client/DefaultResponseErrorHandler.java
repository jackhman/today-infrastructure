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

package cn.taketoday.web.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import cn.taketoday.core.ResolvableType;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.http.client.ClientHttpResponseDecorator;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.FileCopyUtils;
import cn.taketoday.util.LogFormatUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * Default implementation of the {@link ResponseErrorHandler} interface.
 *
 * <p>This error handler checks for the status code on the
 * {@link ClientHttpResponse}. Any code in the 4xx or 5xx series is considered
 * to be an error. This behavior can be changed by overriding
 * {@link #hasError(HttpStatusCode)}. Unknown status codes will be ignored by
 * {@link #hasError(ClientHttpResponse)}.
 *
 * <p>See {@link #handleError(ClientHttpResponse)} for more details on specific
 * exception types.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @see RestTemplate#setErrorHandler
 * @since 4.0
 */
public class DefaultResponseErrorHandler implements ResponseErrorHandler {

  @Nullable
  private List<HttpMessageConverter<?>> messageConverters;

  /**
   * For internal use from the RestTemplate, to pass the message converters
   * to use to decode error content.
   */
  void setMessageConverters(List<HttpMessageConverter<?>> converters) {
    this.messageConverters = Collections.unmodifiableList(converters);
  }

  /**
   * Delegates to {@link #hasError(HttpStatusCode)} with the response status code.
   *
   * @see ClientHttpResponse#getStatusCode()
   * @see #hasError(HttpStatusCode)
   */
  @Override
  public boolean hasError(ClientHttpResponse response) throws IOException {
    HttpStatusCode statusCode = response.getStatusCode();
    return hasError(statusCode);
  }

  /**
   * Template method called from {@link #hasError(ClientHttpResponse)}.
   * <p>The default implementation checks {@link HttpStatusCode#isError()}.
   * Can be overridden in subclasses.
   *
   * @param statusCode the HTTP status code
   * @return {@code true} if the response indicates an error; {@code false} otherwise
   * @see HttpStatusCode#isError()
   */
  protected boolean hasError(HttpStatusCode statusCode) {
    return statusCode.isError();
  }

  /**
   * Handle the error in the given response with the given resolved status code.
   * <p>The default implementation throws:
   * <ul>
   * <li>{@link HttpClientErrorException} if the status code is in the 4xx
   * series, or one of its sub-classes such as
   * {@link HttpClientErrorException.BadRequest} and others.
   * <li>{@link HttpServerErrorException} if the status code is in the 5xx
   * series, or one of its sub-classes such as
   * {@link HttpServerErrorException.InternalServerError} and others.
   * <li>{@link UnknownHttpStatusCodeException} for error status codes not in the
   * {@link HttpStatus} enum range.
   * </ul>
   *
   * @throws UnknownHttpStatusCodeException in case of an unresolvable status code
   * @see #handleError(ClientHttpResponse, HttpStatusCode)
   */
  @Override
  public void handleError(ClientHttpResponse response) throws IOException {
    HttpStatusCode statusCode = response.getStatusCode();
    handleError(response, statusCode);
  }

  /**
   * Return error message with details from the response body. For example:
   * <pre>
   * 404 Not Found: [{'id': 123, 'message': 'my message'}]
   * </pre>
   */
  private String getErrorMessage(
      int rawStatusCode, String statusText, @Nullable byte[] responseBody, @Nullable Charset charset) {

    String preface = "%d %s: ".formatted(rawStatusCode, statusText);

    if (ObjectUtils.isEmpty(responseBody)) {
      return preface + "[no body]";
    }

    if (charset == null) {
      charset = StandardCharsets.UTF_8;
    }

    String bodyText = new String(responseBody, charset);
    bodyText = LogFormatUtils.formatValue(bodyText, -1, true);

    return preface + bodyText;
  }

  /**
   * Handle the error based on the resolved status code.
   *
   * <p>The default implementation delegates to
   * {@link HttpClientErrorException#create} for errors in the 4xx range, to
   * {@link HttpServerErrorException#create} for errors in the 5xx range,
   * or otherwise raises {@link UnknownHttpStatusCodeException}.
   *
   * @see HttpClientErrorException#create
   * @see HttpServerErrorException#create
   */
  protected void handleError(ClientHttpResponse response, HttpStatusCode statusCode) throws IOException {
    String statusText = response.getStatusText();
    HttpHeaders headers = response.getHeaders();
    byte[] body = getResponseBody(response);
    Charset charset = getCharset(response);
    String message = getErrorMessage(statusCode.value(), statusText, body, charset);

    RestClientResponseException ex;
    if (statusCode.is4xxClientError()) {
      ex = HttpClientErrorException.create(message, statusCode, statusText, headers, body, charset);
    }
    else if (statusCode.is5xxServerError()) {
      ex = HttpServerErrorException.create(message, statusCode, statusText, headers, body, charset);
    }
    else {
      ex = new UnknownHttpStatusCodeException(message, statusCode.value(), statusText, headers, body, charset);
    }

    if (CollectionUtils.isNotEmpty(this.messageConverters)) {
      ex.setBodyConvertFunction(initBodyConvertFunction(response, body));
    }

    throw ex;
  }

  /**
   * Return a function for decoding the error content. This can be passed to
   * {@link RestClientResponseException#setBodyConvertFunction(Function)}.
   */
  protected Function<ResolvableType, ?> initBodyConvertFunction(ClientHttpResponse response, byte[] body) {
    Assert.state(CollectionUtils.isNotEmpty(messageConverters), "Expected message converters");
    return resolvableType -> {
      try {
        HttpMessageConverterExtractor<?> extractor =
            new HttpMessageConverterExtractor<>(resolvableType.getType(), this.messageConverters);

        return extractor.extractData(new ClientHttpResponseDecorator(response) {
          @Override
          public InputStream getBody() {
            return new ByteArrayInputStream(body);
          }
        });
      }
      catch (IOException ex) {
        throw new RestClientException(
                "Error while extracting response for type [%s]".formatted(resolvableType), ex);
      }
    };
  }

  /**
   * Read the body of the given response (for inclusion in a status exception).
   *
   * @param response the response to inspect
   * @return the response body as a byte array,
   * or an empty byte array if the body could not be read
   */
  protected byte[] getResponseBody(ClientHttpResponse response) {
    try {
      return FileCopyUtils.copyToByteArray(response.getBody());
    }
    catch (IOException ex) {
      // ignore
    }
    return Constant.EMPTY_BYTES;
  }

  /**
   * Determine the charset of the response (for inclusion in a status exception).
   *
   * @param response the response to inspect
   * @return the associated charset, or {@code null} if none
   */
  @Nullable
  protected Charset getCharset(ClientHttpResponse response) {
    HttpHeaders headers = response.getHeaders();
    MediaType contentType = headers.getContentType();
    return contentType != null ? contentType.getCharset() : null;
  }

}

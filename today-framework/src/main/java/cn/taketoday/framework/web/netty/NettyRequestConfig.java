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

package cn.taketoday.framework.web.netty;

import java.nio.charset.Charset;
import java.util.function.Supplier;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.function.SingletonSupplier;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpDataFactory;

/**
 * To help build a {@link NettyRequestContext}
 *
 * <p>
 * User can use this class to customize {@link NettyRequestContext}
 * </p>
 *
 * @author TODAY 2021/3/30 17:46
 */
public class NettyRequestConfig {

  /**
   * Should Netty validate HTTP response Header values to ensure they aren't malicious.
   */
  private boolean validateHeaders = false;
  private boolean singleFieldHeaders = true;

  private HttpVersion httpVersion = HttpVersion.HTTP_1_1;

  private Supplier<HttpHeaders> trailingHeaders;

  private ServerCookieEncoder cookieEncoder = ServerCookieEncoder.STRICT;
  private ServerCookieDecoder cookieDecoder = ServerCookieDecoder.STRICT;

  /**
   * response body initial size
   *
   * @see io.netty.buffer.Unpooled#buffer(int)
   */
  private int bodyInitialSize = 64;

  @Nullable
  private Supplier<ByteBuf> responseBody;

  /**
   * {@code contextPath} just like {@code HttpServletRequest.getContextPath()}
   *
   * @see jakarta.servlet.http.HttpServletRequest#getContextPath()
   */
  private String contextPath = "";
  private Charset postRequestDecoderCharset = Constant.DEFAULT_CHARSET;

  private HttpDataFactory httpDataFactory;

  public NettyRequestConfig() {
    this(SingletonSupplier.valueOf(EmptyHttpHeaders.INSTANCE));
  }

  public NettyRequestConfig(Supplier<HttpHeaders> trailingHeaders) {
    this(new DefaultHttpDataFactory(true), trailingHeaders);
  }

  public NettyRequestConfig(HttpDataFactory httpDataFactory, Supplier<HttpHeaders> trailingHeaders) {
    setTrailingHeaders(trailingHeaders);
    setHttpDataFactory(httpDataFactory);
  }

  /**
   * Should Netty validate Header values to ensure they aren't malicious.
   */
  public void setValidateHeaders(boolean validateHeaders) {
    this.validateHeaders = validateHeaders;
  }

  public void setSingleFieldHeaders(boolean singleFieldHeaders) {
    this.singleFieldHeaders = singleFieldHeaders;
  }

  public boolean isSingleFieldHeaders() {
    return singleFieldHeaders;
  }

  public boolean isValidateHeaders() {
    return validateHeaders;
  }

  public void setTrailingHeaders(Supplier<HttpHeaders> trailingHeaders) {
    Assert.notNull(trailingHeaders, "trailingHeaders Supplier cannot be null");
    this.trailingHeaders = trailingHeaders;
  }

  public Supplier<HttpHeaders> getTrailingHeaders() {
    return trailingHeaders;
  }

  public void setHttpVersion(HttpVersion httpVersion) {
    this.httpVersion = httpVersion;
  }

  public HttpVersion getHttpVersion() {
    return httpVersion;
  }

  public void setCookieDecoder(@Nullable ServerCookieDecoder cookieDecoder) {
    this.cookieDecoder = cookieDecoder == null ? ServerCookieDecoder.STRICT : cookieDecoder;
  }

  public void setCookieEncoder(@Nullable ServerCookieEncoder cookieEncoder) {
    this.cookieEncoder = cookieEncoder == null ? ServerCookieEncoder.STRICT : cookieEncoder;
  }

  public ServerCookieDecoder getCookieDecoder() {
    return cookieDecoder;
  }

  public ServerCookieEncoder getCookieEncoder() {
    return cookieEncoder;
  }

  public void setResponseBody(@Nullable Supplier<ByteBuf> responseBody) {
    this.responseBody = responseBody;
  }

  @Nullable
  public Supplier<ByteBuf> getResponseBody() {
    return responseBody;
  }

  /**
   * @return response body initial capacity
   * @see io.netty.buffer.Unpooled#buffer(int)
   */
  public int getBodyInitialSize() {
    return bodyInitialSize;
  }

  /**
   * @param bodyInitialSize response body initial capacity
   * @see io.netty.buffer.Unpooled#buffer(int)
   */
  public void setBodyInitialSize(int bodyInitialSize) {
    this.bodyInitialSize = bodyInitialSize;
  }

  public void setContextPath(@Nullable String contextPath) {
    this.contextPath = contextPath == null ? "" : contextPath;
  }

  /**
   * {@code contextPath} just like {@code HttpServletRequest.getContextPath()}
   *
   * @see jakarta.servlet.http.HttpServletRequest#getContextPath()
   */
  public String getContextPath() {
    return contextPath;
  }

  public void setPostRequestDecoderCharset(@Nullable Charset charset) {
    this.postRequestDecoderCharset =
            charset == null ? Constant.DEFAULT_CHARSET : charset;
  }

  public Charset getPostRequestDecoderCharset() {
    return postRequestDecoderCharset;
  }

  public HttpDataFactory getHttpDataFactory() {
    return httpDataFactory;
  }

  public void setHttpDataFactory(HttpDataFactory httpDataFactory) {
    Assert.notNull(httpDataFactory, "HttpDataFactory is required");
    this.httpDataFactory = httpDataFactory;
  }
}
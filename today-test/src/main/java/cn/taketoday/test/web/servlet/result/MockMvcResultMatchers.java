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

package cn.taketoday.test.web.servlet.result;

import org.hamcrest.Matcher;

import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import cn.taketoday.lang.Nullable;
import cn.taketoday.test.web.servlet.ResultMatcher;
import cn.taketoday.util.AntPathMatcher;
import cn.taketoday.web.util.UriComponentsBuilder;

import static cn.taketoday.test.util.AssertionErrors.assertEquals;
import static cn.taketoday.test.util.AssertionErrors.assertTrue;

/**
 * Static factory methods for {@link ResultMatcher}-based result actions.
 *
 * <h3>Eclipse Users</h3>
 * <p>Consider adding this class as a Java editor favorite. To navigate to
 * this setting, open the Preferences and type "favorites".
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 4.0
 */
public abstract class MockMvcResultMatchers {

  private static final AntPathMatcher pathMatcher = new AntPathMatcher();

  /**
   * Access to request-related assertions.
   */
  public static RequestResultMatchers request() {
    return new RequestResultMatchers();
  }

  /**
   * Access to assertions for the handler that handled the request.
   */
  public static HandlerResultMatchers handler() {
    return new HandlerResultMatchers();
  }

  /**
   * Access to model-related assertions.
   */
  public static ModelResultMatchers model() {
    return new ModelResultMatchers();
  }

  /**
   * Access to assertions on the selected view.
   */
  public static ViewResultMatchers view() {
    return new ViewResultMatchers();
  }

  /**
   * Access to flash attribute assertions.
   */
  public static FlashAttributeResultMatchers flash() {
    return new FlashAttributeResultMatchers();
  }

  /**
   * Asserts the request was forwarded to the given URL.
   * <p>This method accepts only exact matches.
   *
   * @param expectedUrl the exact URL expected
   */
  public static ResultMatcher forwardedUrl(@Nullable String expectedUrl) {
    return result -> assertEquals("Forwarded URL", expectedUrl, result.getResponse().getForwardedUrl());
  }

  /**
   * Asserts the request was forwarded to the given URL template.
   * <p>This method accepts exact matches against the expanded and encoded URL template.
   *
   * @param urlTemplate a URL template; the expanded URL will be encoded
   * @param uriVars zero or more URI variables to populate the template
   * @see UriComponentsBuilder#fromUriString(String)
   */
  public static ResultMatcher forwardedUrlTemplate(String urlTemplate, Object... uriVars) {
    String uri = UriComponentsBuilder.fromUriString(urlTemplate).buildAndExpand(uriVars).encode().toUriString();
    return forwardedUrl(uri);
  }

  /**
   * Asserts the request was forwarded to the given URL.
   * <p>This method accepts {@link cn.taketoday.util.AntPathMatcher}
   * patterns.
   *
   * @param urlPattern an Ant-style path pattern to match against
   * @see cn.taketoday.util.AntPathMatcher
   * @since 4.0
   */
  public static ResultMatcher forwardedUrlPattern(String urlPattern) {
    return result -> {
      assertTrue("'" + urlPattern + "' is not an Ant-style path pattern",
              pathMatcher.isPattern(urlPattern));
      String url = result.getResponse().getForwardedUrl();
      assertTrue("Forwarded URL '" + url + "' does not match the expected URL pattern '" + urlPattern + "'",
              (url != null && pathMatcher.match(urlPattern, url)));
    };
  }

  /**
   * Asserts the request was redirected to the given URL.
   * <p>This method accepts only exact matches.
   *
   * @param expectedUrl the exact URL expected
   */
  public static ResultMatcher redirectedUrl(String expectedUrl) {
    return result -> assertEquals("Redirected URL", expectedUrl, result.getResponse().getRedirectedUrl());
  }

  /**
   * Asserts the request was redirected to the given URL template.
   * <p>This method accepts exact matches against the expanded and encoded URL template.
   *
   * @param urlTemplate a URL template; the expanded URL will be encoded
   * @param uriVars zero or more URI variables to populate the template
   * @see UriComponentsBuilder#fromUriString(String)
   */
  public static ResultMatcher redirectedUrlTemplate(String urlTemplate, Object... uriVars) {
    String uri = UriComponentsBuilder.fromUriString(urlTemplate).buildAndExpand(uriVars).encode().toUriString();
    return redirectedUrl(uri);
  }

  /**
   * Asserts the request was redirected to the given URL.
   * <p>This method accepts {@link cn.taketoday.util.AntPathMatcher}
   * patterns.
   *
   * @param urlPattern an Ant-style path pattern to match against
   * @see cn.taketoday.util.AntPathMatcher
   * @since 4.0
   */
  public static ResultMatcher redirectedUrlPattern(String urlPattern) {
    return result -> {
      assertTrue("'" + urlPattern + "' is not an Ant-style path pattern",
              pathMatcher.isPattern(urlPattern));
      String url = result.getResponse().getRedirectedUrl();
      assertTrue("Redirected URL '" + url + "' does not match the expected URL pattern '" + urlPattern + "'",
              (url != null && pathMatcher.match(urlPattern, url)));
    };
  }

  /**
   * Access to response status assertions.
   */
  public static StatusResultMatchers status() {
    return new StatusResultMatchers();
  }

  /**
   * Access to response header assertions.
   */
  public static HeaderResultMatchers header() {
    return new HeaderResultMatchers();
  }

  /**
   * Access to response body assertions.
   */
  public static ContentResultMatchers content() {
    return new ContentResultMatchers();
  }

  /**
   * Access to response body assertions using a
   * <a href="https://github.com/jayway/JsonPath">JsonPath</a> expression
   * to inspect a specific subset of the body.
   * <p>The JSON path expression can be a parameterized string using
   * formatting specifiers as defined in
   * {@link String#format(String, Object...)}.
   *
   * @param expression the JSON path expression, optionally parameterized with arguments
   * @param args arguments to parameterize the JSON path expression with
   * @see #jsonPath(String, Matcher)
   * @see #jsonPath(String, Matcher, Class)
   */
  public static JsonPathResultMatchers jsonPath(String expression, Object... args) {
    return new JsonPathResultMatchers(expression, args);
  }

  /**
   * Evaluate the given <a href="https://github.com/jayway/JsonPath">JsonPath</a>
   * expression against the response body and assert the resulting value with
   * the given Hamcrest {@link Matcher}.
   *
   * @param expression the JSON path expression
   * @param matcher a matcher for the value expected at the JSON path
   * @see #jsonPath(String, Object...)
   * @see #jsonPath(String, Matcher, Class)
   */
  public static <T> ResultMatcher jsonPath(String expression, Matcher<? super T> matcher) {
    return new JsonPathResultMatchers(expression).value(matcher);
  }

  /**
   * Evaluate the given <a href="https://github.com/jayway/JsonPath">JsonPath</a>
   * expression against the response body and assert the resulting value with
   * the given Hamcrest {@link Matcher}, coercing the resulting value into the
   * given target type before applying the matcher.
   * <p>This can be useful for matching numbers reliably &mdash; for example,
   * to coerce an integer into a double.
   *
   * @param expression the JSON path expression
   * @param matcher a matcher for the value expected at the JSON path
   * @param targetType the target type to coerce the matching value into
   * @see #jsonPath(String, Object...)
   * @see #jsonPath(String, Matcher)
   * @since 5.2
   */
  public static <T> ResultMatcher jsonPath(String expression, Matcher<? super T> matcher, Class<T> targetType) {
    return new JsonPathResultMatchers(expression).value(matcher, targetType);
  }

  /**
   * Access to response body assertions using an XPath expression to
   * inspect a specific subset of the body.
   * <p>The XPath expression can be a parameterized string using formatting
   * specifiers as defined in {@link String#format(String, Object...)}.
   *
   * @param expression the XPath expression, optionally parameterized with arguments
   * @param args arguments to parameterize the XPath expression with
   */
  public static XpathResultMatchers xpath(String expression, Object... args) throws XPathExpressionException {
    return new XpathResultMatchers(expression, null, args);
  }

  /**
   * Access to response body assertions using an XPath expression to
   * inspect a specific subset of the body.
   * <p>The XPath expression can be a parameterized string using formatting
   * specifiers as defined in {@link String#format(String, Object...)}.
   *
   * @param expression the XPath expression, optionally parameterized with arguments
   * @param namespaces the namespaces referenced in the XPath expression
   * @param args arguments to parameterize the XPath expression with
   */
  public static XpathResultMatchers xpath(String expression, Map<String, String> namespaces, Object... args)
          throws XPathExpressionException {

    return new XpathResultMatchers(expression, namespaces, args);
  }

  /**
   * Access to response cookie assertions.
   */
  public static CookieResultMatchers cookie() {
    return new CookieResultMatchers();
  }

}
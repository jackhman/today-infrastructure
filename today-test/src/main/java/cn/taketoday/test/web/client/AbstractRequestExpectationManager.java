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

package cn.taketoday.test.web.client;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.client.ClientHttpRequest;
import cn.taketoday.http.client.ClientHttpResponse;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Base class for {@code RequestExpectationManager} implementations responsible
 * for storing expectations and actual requests, and checking for unsatisfied
 * expectations at the end.
 *
 * <p>Subclasses are responsible for validating each request by matching it to
 * to expectations following the order of declaration or not.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class AbstractRequestExpectationManager implements RequestExpectationManager {

  private final List<RequestExpectation> expectations = new ArrayList<>();

  private final List<ClientHttpRequest> requests = new ArrayList<>();

  private final Map<ClientHttpRequest, Throwable> requestFailures = new LinkedHashMap<>();

  /**
   * Return a read-only list of the expectations.
   */
  protected List<RequestExpectation> getExpectations() {
    return Collections.unmodifiableList(this.expectations);
  }

  /**
   * Return a read-only list of requests executed so far.
   */
  protected List<ClientHttpRequest> getRequests() {
    return Collections.unmodifiableList(this.requests);
  }

  @Override
  public ResponseActions expectRequest(ExpectedCount count, RequestMatcher matcher) {
    Assert.state(this.requests.isEmpty(), "Cannot add more expectations after actual requests are made");
    RequestExpectation expectation = new DefaultRequestExpectation(count, matcher);
    this.expectations.add(expectation);
    return expectation;
  }

  @Override
  public ClientHttpResponse validateRequest(ClientHttpRequest request) throws IOException {
    RequestExpectation expectation;
    synchronized(this.requests) {
      if (this.requests.isEmpty()) {
        afterExpectationsDeclared();
      }
      try {
        expectation = matchRequest(request);
      }
      catch (Throwable ex) {
        this.requestFailures.put(request, ex);
        throw ex;
      }
      finally {
        this.requests.add(request);
      }
    }
    return expectation.createResponse(request);
  }

  /**
   * Invoked at the time of the first actual request, which effectively means
   * the expectations declaration phase is over.
   */
  protected void afterExpectationsDeclared() {
  }

  /**
   * As of 5.0.3 subclasses should implement this method instead of
   * {@link #validateRequestInternal(ClientHttpRequest)} in order to match the
   * request to an expectation, leaving the call to create the response as a separate step
   * (to be invoked by this class).
   *
   * @param request the current request
   * @return the matched expectation with its request count updated via
   * {@link RequestExpectation#incrementAndValidate()}.
   * @since 4.0
   */
  protected RequestExpectation matchRequest(ClientHttpRequest request) throws IOException {
    throw new UnsupportedOperationException(
            "It looks like neither the deprecated \"validateRequestInternal\"" +
                    "nor its replacement (this method) are implemented.");
  }

  @Override
  public void verify() {
    int count = verifyInternal();
    if (count > 0) {
      String message = "Further request(s) expected leaving " + count + " unsatisfied expectation(s).\n";
      throw new AssertionError(message + getRequestDetails());
    }
  }

  @Override
  public void verify(Duration timeout) {
    Instant endTime = Instant.now().plus(timeout);
    do {
      if (verifyInternal() == 0) {
        return;
      }
    }
    while (Instant.now().isBefore(endTime));
    verify();
  }

  private int verifyInternal() {
    if (this.expectations.isEmpty()) {
      return 0;
    }
    if (!this.requestFailures.isEmpty()) {
      throw new AssertionError("Some requests did not execute successfully.\n" +
              this.requestFailures.entrySet().stream()
                      .map(entry -> "Failed request:\n" + entry.getKey() + "\n" + entry.getValue())
                      .collect(Collectors.joining("\n", "\n", "")));
    }
    int count = 0;
    for (RequestExpectation expectation : this.expectations) {
      if (!expectation.isSatisfied()) {
        count++;
      }
    }
    return count;
  }

  /**
   * Return details of executed requests.
   */
  protected String getRequestDetails() {
    StringBuilder sb = new StringBuilder();
    sb.append(this.requests.size()).append(" request(s) executed");
    if (!this.requests.isEmpty()) {
      sb.append(":\n");
      for (ClientHttpRequest request : this.requests) {
        sb.append(request.toString()).append('\n');
      }
    }
    else {
      sb.append(".\n");
    }
    return sb.toString();
  }

  /**
   * Return an {@code AssertionError} that a sub-class can raise for an
   * unexpected request.
   */
  protected AssertionError createUnexpectedRequestError(ClientHttpRequest request) {
    HttpMethod method = request.getMethod();
    URI uri = request.getURI();
    String message = "No further requests expected: HTTP " + method + " " + uri + "\n";
    return new AssertionError(message + getRequestDetails());
  }

  @Override
  public void reset() {
    this.expectations.clear();
    this.requests.clear();
    this.requestFailures.clear();
  }

  /**
   * Helper class to manage a group of remaining expectations.
   */
  protected static class RequestExpectationGroup {

    private final Set<RequestExpectation> expectations = new LinkedHashSet<>();

    public void addAllExpectations(Collection<RequestExpectation> expectations) {
      this.expectations.addAll(expectations);
    }

    public Set<RequestExpectation> getExpectations() {
      return this.expectations;
    }

    /**
     * Return a matching expectation, or {@code null} if none match.
     */
    @Nullable
    public RequestExpectation findExpectation(ClientHttpRequest request) throws IOException {
      for (RequestExpectation expectation : this.expectations) {
        try {
          expectation.match(request);
          return expectation;
        }
        catch (AssertionError error) {
          // We're looking to find a match or return null..
        }
      }
      return null;
    }

    /**
     * Invoke this for an expectation that has been matched.
     * <p>The count of the given expectation is incremented, then it is
     * either stored if remainingCount &gt; 0 or removed otherwise.
     */
    public void update(RequestExpectation expectation) {
      expectation.incrementAndValidate();
      updateInternal(expectation);
    }

    private void updateInternal(RequestExpectation expectation) {
      if (expectation.hasRemainingCount()) {
        this.expectations.add(expectation);
      }
      else {
        this.expectations.remove(expectation);
      }
    }

    /**
     * Reset all expectations for this group.
     */
    public void reset() {
      this.expectations.clear();
    }
  }

}

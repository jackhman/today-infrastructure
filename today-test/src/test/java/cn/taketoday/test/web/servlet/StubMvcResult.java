/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.web.servlet;

import cn.taketoday.mock.web.MockHttpServletRequest;
import cn.taketoday.mock.web.MockHttpServletResponse;
import cn.taketoday.web.HandlerInterceptor;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.view.ModelAndView;
import cn.taketoday.web.RedirectModel;

/**
 * A stub implementation of the {@link MvcResult} contract.
 *
 * @author Rossen Stoyanchev
 */
public class StubMvcResult implements MvcResult {

  private MockHttpServletRequest request;

  private Object handler;

  private HandlerInterceptor[] interceptors;

  private Exception resolvedException;

  private ModelAndView mav;

  private RedirectModel flashMap;

  private MockHttpServletResponse response;
  final RequestContext requestContext;

  public StubMvcResult(MockHttpServletRequest request,
          Object handler,
          HandlerInterceptor[] interceptors,
          Exception resolvedException,
          ModelAndView mav,
          RedirectModel flashMap,
          MockHttpServletResponse response) {
    this.request = request;
    this.handler = handler;
    this.interceptors = interceptors;
    this.resolvedException = resolvedException;
    this.mav = mav;
    this.flashMap = flashMap;
    this.response = response;

    this.requestContext = new ServletRequestContext(null, request, response);
  }

  @Override
  public MockHttpServletRequest getRequest() {
    return request;
  }

  @Override
  public Object getHandler() {
    return handler;
  }

  @Override
  public HandlerInterceptor[] getInterceptors() {
    return interceptors;
  }

  @Override
  public Exception getResolvedException() {
    return resolvedException;
  }

  @Override
  public ModelAndView getModelAndView() {
    return mav;
  }

  @Override
  public RedirectModel getFlashMap() {
    return flashMap;
  }

  @Override
  public MockHttpServletResponse getResponse() {
    return response;
  }

  @Override
  public RequestContext getRequestContext() {
    return requestContext;
  }

  public ModelAndView getMav() {
    return mav;
  }

  public void setMav(ModelAndView mav) {
    this.mav = mav;
  }

  public void setRequest(MockHttpServletRequest request) {
    this.request = request;
  }

  public void setHandler(Object handler) {
    this.handler = handler;
  }

  public void setInterceptors(HandlerInterceptor[] interceptors) {
    this.interceptors = interceptors;
  }

  public void setResolvedException(Exception resolvedException) {
    this.resolvedException = resolvedException;
  }

  public void setFlashMap(RedirectModel flashMap) {
    this.flashMap = flashMap;
  }

  public void setResponse(MockHttpServletResponse response) {
    this.response = response;
  }

  @Override
  public Object getAsyncResult() {
    return null;
  }

  @Override
  public Object getAsyncResult(long timeToWait) {
    return null;
  }

}

/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web.servlet;

import static cn.taketoday.web.servlet.DispatcherServlet.prepareContext;

import java.io.IOException;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.mapping.HandlerMethod;
import cn.taketoday.web.mapping.ViewMapping;
import cn.taketoday.web.resolver.ExceptionResolver;
import cn.taketoday.web.utils.ResultUtils;
import cn.taketoday.web.view.ViewResolver;
import lombok.extern.slf4j.Slf4j;

/**
 * @author TODAY <br>
 *         2018-06-25 19:48:28
 * @version 2.0.0
 */
@Slf4j
@SuppressWarnings("serial")
public class ViewDispatcher extends GenericServlet {

    /** exception Resolver */
    @Autowired(Constant.EXCEPTION_RESOLVER)
    private ExceptionResolver exceptionResolver;

    @Autowired(Constant.VIEW_RESOLVER)
    protected ViewResolver viewResolver;

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {

        final HttpServletRequest request = (HttpServletRequest) req;

        final ViewMapping mapping = ViewMapping.get(request.getRequestURI());

        if (mapping == null) {
            ((HttpServletResponse) res).sendError(404);
            log.debug("NOT FOUND -> [{}]", request.getRequestURI());
            return;
        }

        final RequestContext requestContext = prepareContext(request, res);
        try {

            if (mapping.getStatus() != 0) {
                requestContext.status(mapping.getStatus());
            }

            final String contentType = mapping.getContentType();
            if (StringUtils.isNotEmpty(contentType)) {
                requestContext.contentType(contentType);
            }

            if (mapping.hasAction()) {

                final HandlerMethod handlerMethod = mapping.getHandlerMethod();

                final Object result = handlerMethod.getMethod()//
                        .invoke(mapping.getBean(), handlerMethod.resolveParameters(requestContext));

                if (handlerMethod.is(void.class)) {
                    ResultUtils.resolveView(mapping.getAssetsPath(), viewResolver, requestContext);
                }
                else {
                    handlerMethod.resolveResult(requestContext, result);
                }
            }
            else {
                ResultUtils.resolveView(mapping.getAssetsPath(), viewResolver, requestContext);
            }
        }
        catch (Throwable e) {
            ResultUtils.resolveException(requestContext, exceptionResolver, mapping, e);
        }
    }

    public String getServletName() {
        return "ViewDispatcher";
    }

    @Override
    public String getServletInfo() {
        return "ViewDispatcher, Copyright © TODAY & 2017 - 2019 All Rights Reserved";
    }

}

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

package cn.taketoday.framework.test.web.reactive.server;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.framework.test.context.InfraTest;
import cn.taketoday.framework.web.embedded.tomcat.TomcatReactiveWebServerFactory;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.server.reactive.ContextPathCompositeHandler;
import cn.taketoday.http.server.reactive.HttpHandler;
import cn.taketoday.http.server.reactive.ServerHttpRequest;
import cn.taketoday.http.server.reactive.ServerHttpResponse;
import cn.taketoday.test.context.TestPropertySource;
import cn.taketoday.test.web.reactive.server.WebTestClient;

import java.util.Collections;
import java.util.Map;

import reactor.core.publisher.Mono;

/**
 * Tests for {@link WebTestClientContextCustomizer} with a custom base path for a reactive
 * web application.
 *
 * @author Madhura Bhave
 */
@InfraTest(webEnvironment = InfraTest.WebEnvironment.RANDOM_PORT,
           properties = "app.main.application-type=reactive_web")
@TestPropertySource(properties = "webflux.base-path=/test")
@Disabled
class WebTestClientContextCustomizerWithCustomBasePathTests {

  @Autowired
  private WebTestClient webTestClient;

  @Test
  void test() {
    this.webTestClient.get().uri("/hello")
            .exchange()
            .expectBody(String.class).isEqualTo("hello world");
  }

  @Configuration(proxyBeanMethods = false)
  static class TestConfig {

    @Bean
    TomcatReactiveWebServerFactory webServerFactory() {
      return new TomcatReactiveWebServerFactory(0);
    }

    @Bean
    HttpHandler httpHandler() {
      TestHandler httpHandler = new TestHandler();
      Map<String, HttpHandler> handlersMap = Collections.singletonMap("/test", httpHandler);
      return new ContextPathCompositeHandler(handlersMap);
    }

  }

  static class TestHandler implements HttpHandler {

    private static final DefaultDataBufferFactory factory = new DefaultDataBufferFactory();

    @Override
    public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
      response.setStatusCode(HttpStatus.OK);
      return response.writeWith(Mono.just(factory.wrap("hello world".getBytes())));
    }

  }

}

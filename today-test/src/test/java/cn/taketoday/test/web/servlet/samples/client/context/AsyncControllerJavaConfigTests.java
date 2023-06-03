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

package cn.taketoday.test.web.servlet.samples.client.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.http.MediaType;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextHierarchy;
import cn.taketoday.test.context.junit.jupiter.InfraExtension;
import cn.taketoday.test.context.web.WebAppConfiguration;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.test.web.servlet.client.MockMvcWebTestClient;
import cn.taketoday.web.annotation.GetMapping;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.config.AsyncSupportConfigurer;
import cn.taketoday.web.config.EnableWebMvc;
import cn.taketoday.web.config.WebMvcConfigurer;
import cn.taketoday.web.context.async.CallableProcessingInterceptor;
import cn.taketoday.web.servlet.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link cn.taketoday.test.web.servlet.samples.context.AsyncControllerJavaConfigTests}.
 *
 * @author Rossen Stoyanchev
 */
@ExtendWith(InfraExtension.class)
@WebAppConfiguration
@ContextHierarchy(@ContextConfiguration(classes = AsyncControllerJavaConfigTests.WebConfig.class))
public class AsyncControllerJavaConfigTests {

  @Autowired
  private WebApplicationContext wac;

  @Autowired
  private CallableProcessingInterceptor callableInterceptor;

  private WebTestClient testClient;

  @BeforeEach
  public void setup() {
    this.testClient = MockMvcWebTestClient.bindToApplicationContext(this.wac).build();
  }

  @Test
  public void callableInterceptor() throws Exception {
    testClient.get().uri("/callable")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
//            .consumeWith(System.out::println)
            .json("{\"key\":\"value\"}");

    verify(this.callableInterceptor).beforeConcurrentHandling(any(), any());
    verify(this.callableInterceptor).preProcess(any(), any());
    verify(this.callableInterceptor).postProcess(any(), any(), any());
    verify(this.callableInterceptor).afterCompletion(any(), any());
    verifyNoMoreInteractions(this.callableInterceptor);
  }

  @Configuration
  @EnableWebMvc
  static class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
      configurer.registerCallableInterceptors(callableInterceptor());
    }

    @Bean
    public CallableProcessingInterceptor callableInterceptor() {
      return mock();
    }

    @Bean
    public AsyncController asyncController() {
      return new AsyncController();
    }

  }

  @RestController
  static class AsyncController {

    @GetMapping("/callable")
    public Callable<Map<String, String>> getCallable() {
      return () -> Collections.singletonMap("key", "value");
    }
  }

}
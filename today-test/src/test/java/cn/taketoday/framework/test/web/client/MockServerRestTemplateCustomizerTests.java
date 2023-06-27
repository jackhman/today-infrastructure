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

package cn.taketoday.framework.test.web.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.http.client.BufferingClientHttpRequestFactory;
import cn.taketoday.http.client.ClientHttpRequestFactory;
import cn.taketoday.test.web.client.RequestExpectationManager;
import cn.taketoday.test.web.client.SimpleRequestExpectationManager;
import cn.taketoday.test.web.client.UnorderedRequestExpectationManager;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.client.config.RestTemplateBuilder;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static cn.taketoday.test.web.client.match.MockRestRequestMatchers.requestTo;
import static cn.taketoday.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link MockServerRestTemplateCustomizer}.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
class MockServerRestTemplateCustomizerTests {

  private MockServerRestTemplateCustomizer customizer;

  @BeforeEach
  void setup() {
    this.customizer = new MockServerRestTemplateCustomizer();
  }

  @Test
  void createShouldUseSimpleRequestExpectationManager() {
    MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
    customizer.customize(new RestTemplate());
    assertThat(customizer.getServer()).extracting("expectationManager")
            .isInstanceOf(SimpleRequestExpectationManager.class);
  }

  @Test
  void createWhenExpectationManagerClassIsNullShouldThrowException() {
    Class<? extends RequestExpectationManager> expectationManager = null;
    assertThatIllegalArgumentException().isThrownBy(() -> new MockServerRestTemplateCustomizer(expectationManager))
            .withMessageContaining("ExpectationManager must not be null");
  }

  @Test
  void createWhenExpectationManagerSupplierIsNullShouldThrowException() {
    Supplier<? extends RequestExpectationManager> expectationManagerSupplier = null;
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new MockServerRestTemplateCustomizer(expectationManagerSupplier))
            .withMessageContaining("ExpectationManagerSupplier must not be null");
  }

  @Test
  void createShouldUseExpectationManagerClass() {
    MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer(
            UnorderedRequestExpectationManager.class);
    customizer.customize(new RestTemplate());
    assertThat(customizer.getServer()).extracting("expectationManager")
            .isInstanceOf(UnorderedRequestExpectationManager.class);
  }

  @Test
  void createShouldUseSupplier() {
    MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer(
            UnorderedRequestExpectationManager::new);
    customizer.customize(new RestTemplate());
    assertThat(customizer.getServer()).extracting("expectationManager")
            .isInstanceOf(UnorderedRequestExpectationManager.class);
  }

  @Test
  void detectRootUriShouldDefaultToTrue() {
    MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer(
            UnorderedRequestExpectationManager.class);
    customizer.customize(new RestTemplateBuilder().rootUri("https://example.com").build());
    assertThat(customizer.getServer()).extracting("expectationManager")
            .isInstanceOf(RootUriRequestExpectationManager.class);
  }

  @Test
  void setDetectRootUriShouldDisableRootUriDetection() {
    this.customizer.setDetectRootUri(false);
    this.customizer.customize(new RestTemplateBuilder().rootUri("https://example.com").build());
    assertThat(this.customizer.getServer()).extracting("expectationManager")
            .isInstanceOf(SimpleRequestExpectationManager.class);
  }

  @Test
  void bufferContentShouldDefaultToFalse() {
    MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
    RestTemplate restTemplate = new RestTemplate();
    customizer.customize(restTemplate);
    assertThat(restTemplate.getRequestFactory()).isInstanceOf(ClientHttpRequestFactory.class);
  }

  @Test
  void setBufferContentShouldEnableContentBuffering() {
    MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
    RestTemplate restTemplate = new RestTemplate();
    customizer.setBufferContent(true);
    customizer.customize(restTemplate);
    assertThat(restTemplate.getRequestFactory()).isInstanceOf(BufferingClientHttpRequestFactory.class);
  }

  @Test
  void customizeShouldBindServer() {
    RestTemplate template = new RestTemplateBuilder(this.customizer).build();
    this.customizer.getServer().expect(requestTo("/test")).andRespond(withSuccess());
    template.getForEntity("/test", String.class);
    this.customizer.getServer().verify();
  }

  @Test
  void getServerWhenNoServersAreBoundShouldThrowException() {
    assertThatIllegalStateException().isThrownBy(this.customizer::getServer)
            .withMessageContaining("Unable to return a single MockRestServiceServer since "
                    + "MockServerRestTemplateCustomizer has not been bound to a RestTemplate");
  }

  @Test
  void getServerWhenMultipleServersAreBoundShouldThrowException() {
    this.customizer.customize(new RestTemplate());
    this.customizer.customize(new RestTemplate());
    assertThatIllegalStateException().isThrownBy(this.customizer::getServer)
            .withMessageContaining("Unable to return a single MockRestServiceServer since "
                    + "MockServerRestTemplateCustomizer has been bound to more than one RestTemplate");
  }

  @Test
  void getServerWhenSingleServerIsBoundShouldReturnServer() {
    RestTemplate template = new RestTemplate();
    this.customizer.customize(template);
    assertThat(this.customizer.getServer()).isEqualTo(this.customizer.getServer(template));
  }

  @Test
  void getServerWhenRestTemplateIsFoundShouldReturnServer() {
    RestTemplate template1 = new RestTemplate();
    RestTemplate template2 = new RestTemplate();
    this.customizer.customize(template1);
    this.customizer.customize(template2);
    assertThat(this.customizer.getServer(template1)).isNotNull();
    assertThat(this.customizer.getServer(template2)).isNotNull().isNotSameAs(this.customizer.getServer(template1));
  }

  @Test
  void getServerWhenRestTemplateIsNotFoundShouldReturnNull() {
    RestTemplate template1 = new RestTemplate();
    RestTemplate template2 = new RestTemplate();
    this.customizer.customize(template1);
    assertThat(this.customizer.getServer(template1)).isNotNull();
    assertThat(this.customizer.getServer(template2)).isNull();
  }

  @Test
  void getServersShouldReturnServers() {
    RestTemplate template1 = new RestTemplate();
    RestTemplate template2 = new RestTemplate();
    this.customizer.customize(template1);
    this.customizer.customize(template2);
    assertThat(this.customizer.getServers()).containsOnlyKeys(template1, template2);
  }

  @Test
  void getExpectationManagersShouldReturnExpectationManagers() {
    RestTemplate template1 = new RestTemplate();
    RestTemplate template2 = new RestTemplate();
    this.customizer.customize(template1);
    this.customizer.customize(template2);
    RequestExpectationManager manager1 = this.customizer.getExpectationManagers().get(template1);
    RequestExpectationManager manager2 = this.customizer.getExpectationManagers().get(template2);
    assertThat(this.customizer.getServer(template1)).extracting("expectationManager").isEqualTo(manager1);
    assertThat(this.customizer.getServer(template2)).extracting("expectationManager").isEqualTo(manager2);
  }

}

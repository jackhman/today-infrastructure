/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.annotation.config.web.embedded;

import java.time.Duration;

import cn.taketoday.core.Ordered;
import cn.taketoday.core.env.Environment;
import cn.taketoday.framework.cloud.CloudPlatform;
import cn.taketoday.framework.web.embedded.netty.ReactorNettyReactiveWebServerFactory;
import cn.taketoday.framework.web.server.ServerProperties;
import cn.taketoday.framework.web.server.WebServerFactoryCustomizer;
import cn.taketoday.util.PropertyMapper;
import io.netty.channel.ChannelOption;

/**
 * Customization for Netty-specific features.
 *
 * @author Brian Clozel
 * @author Chentao Qu
 * @author Artsiom Yudovin
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ReactorNettyWebServerFactoryCustomizer
        implements WebServerFactoryCustomizer<ReactorNettyReactiveWebServerFactory>, Ordered {

  private final Environment environment;

  private final ServerProperties serverProperties;

  public ReactorNettyWebServerFactoryCustomizer(Environment environment, ServerProperties serverProperties) {
    this.environment = environment;
    this.serverProperties = serverProperties;
  }

  @Override
  public int getOrder() {
    return 0;
  }

  @Override
  public void customize(ReactorNettyReactiveWebServerFactory factory) {
    factory.setUseForwardHeaders(getOrDeduceUseForwardHeaders());
    PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
    ServerProperties.ReactorNetty nettyProperties = serverProperties.getReactorNetty();

    map.from(nettyProperties::getIdleTimeout).whenNonNull().to(idleTimeout -> customizeIdleTimeout(factory, idleTimeout));
    map.from(nettyProperties::getConnectionTimeout).whenNonNull().to(connectionTimeout -> customizeConnectionTimeout(factory, connectionTimeout));
    map.from(nettyProperties::getMaxKeepAliveRequests).to(maxKeepAliveRequests -> customizeMaxKeepAliveRequests(factory, maxKeepAliveRequests));

    if (serverProperties.getHttp2().isEnabled()) {
      map.from(serverProperties.getMaxHttpRequestHeaderSize())
              .whenNonNull()
              .to(size -> customizeHttp2MaxHeaderSize(factory, size.toBytes()));
    }
    customizeRequestDecoder(factory, map);
  }

  private boolean getOrDeduceUseForwardHeaders() {
    if (this.serverProperties.getForwardHeadersStrategy() == null) {
      CloudPlatform platform = CloudPlatform.getActive(this.environment);
      return platform != null && platform.isUsingForwardHeaders();
    }
    return this.serverProperties.getForwardHeadersStrategy().equals(ServerProperties.ForwardHeadersStrategy.NATIVE);
  }

  private void customizeConnectionTimeout(ReactorNettyReactiveWebServerFactory factory, Duration connectionTimeout) {
    factory.addServerCustomizers(httpServer -> httpServer.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectionTimeout.toMillis()));
  }

  private void customizeRequestDecoder(ReactorNettyReactiveWebServerFactory factory, PropertyMapper propertyMapper) {
    factory.addServerCustomizers((httpServer) -> httpServer.httpRequestDecoder((httpRequestDecoderSpec) -> {
      propertyMapper.from(this.serverProperties.getMaxHttpRequestHeaderSize())
              .whenNonNull()
              .to(maxHttpRequestHeader -> httpRequestDecoderSpec.maxHeaderSize((int) maxHttpRequestHeader.toBytes()));
      ServerProperties.ReactorNetty nettyProperties = this.serverProperties.getReactorNetty();
      propertyMapper.from(nettyProperties.getMaxChunkSize())
              .whenNonNull()
              .to(maxChunkSize -> httpRequestDecoderSpec.maxChunkSize((int) maxChunkSize.toBytes()));
      propertyMapper.from(nettyProperties.getMaxInitialLineLength())
              .whenNonNull()
              .to(maxInitialLineLength -> httpRequestDecoderSpec.maxInitialLineLength((int) maxInitialLineLength.toBytes()));
      propertyMapper.from(nettyProperties.getH2cMaxContentLength())
              .whenNonNull()
              .to(h2cMaxContentLength -> httpRequestDecoderSpec.h2cMaxContentLength((int) h2cMaxContentLength.toBytes()));
      propertyMapper.from(nettyProperties.getInitialBufferSize())
              .whenNonNull()
              .to(initialBufferSize -> httpRequestDecoderSpec.initialBufferSize((int) initialBufferSize.toBytes()));
      propertyMapper.from(nettyProperties.isValidateHeaders()).whenNonNull().to(httpRequestDecoderSpec::validateHeaders);
      return httpRequestDecoderSpec;
    }));
  }

  private void customizeIdleTimeout(ReactorNettyReactiveWebServerFactory factory, Duration idleTimeout) {
    factory.addServerCustomizers(httpServer -> httpServer.idleTimeout(idleTimeout));
  }

  private void customizeMaxKeepAliveRequests(ReactorNettyReactiveWebServerFactory factory, int maxKeepAliveRequests) {
    factory.addServerCustomizers(httpServer -> httpServer.maxKeepAliveRequests(maxKeepAliveRequests));
  }

  private void customizeHttp2MaxHeaderSize(ReactorNettyReactiveWebServerFactory factory, long size) {
    factory.addServerCustomizers(
            ((httpServer) -> httpServer.http2Settings(settings -> settings.maxHeaderListSize(size))));
  }

}

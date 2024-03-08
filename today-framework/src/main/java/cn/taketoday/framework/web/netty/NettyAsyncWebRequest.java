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

package cn.taketoday.framework.web.netty;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.context.async.AsyncWebRequest;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/21 13:47
 */
public class NettyAsyncWebRequest extends AsyncWebRequest {

  private static final Logger log = LoggerFactory.getLogger(NettyAsyncWebRequest.class);

  private final NettyRequestContext request;

  private final ChannelHandlerContext channelContext;

  private volatile boolean asyncStarted;

  NettyAsyncWebRequest(NettyRequestContext request) {
    this.request = request;
    this.channelContext = request.channelContext;
  }

  @Override
  public void startAsync() {
    if (timeout != null) {
      channelContext.executor().schedule(this::checkTimeout, timeout, TimeUnit.MILLISECONDS);
    }

    this.asyncStarted = true;
  }

  private void checkTimeout() {
    if (!isAsyncComplete()) {
      // timeout
      log.debug("Async timeout, dispatch timeout events");
      dispatchEvent(timeoutHandlers);
    }
  }

  @Override
  public boolean isAsyncStarted() {
    return asyncStarted;
  }

  @Override
  public void dispatch(Object concurrentResult) {
    this.asyncStarted = false;
    if (asyncCompleted.compareAndSet(false, true)) {
      channelContext.executor().execute(() -> {
        try {
          request.dispatchConcurrentResult(concurrentResult);
        }
        catch (Throwable e) {
          // last exception handling
          for (Consumer<Throwable> exceptionHandler : exceptionHandlers) {
            exceptionHandler.accept(e);
          }
          channelContext.fireExceptionCaught(e);
        }
        finally {
          dispatchEvent(completionHandlers);
        }
      });
    }
  }

}

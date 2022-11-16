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

package cn.taketoday.samples;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.context.async.DeferredResult;
import cn.taketoday.web.context.async.WebAsyncTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/5 20:55
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/async")
public class AsyncController {

  private final Executor executor;

  @GET("/deferred-result")
  public DeferredResult<String> deferredResult(@Nullable Long timeout) {
    DeferredResult<String> result = new DeferredResult<>(timeout, "Timeout");
    executor.execute(() -> {
      log.info("异步任务开始执行");

      ExceptionUtils.sneakyThrow(() -> TimeUnit.SECONDS.sleep(2));

      log.info("异步任务执行结束，开始返回");

      if (result.setResult("result from " + Thread.currentThread().getName())) {
        log.info("返回结果设置成功");
      }
      else {
        log.info("返回结果设置失败，已经返回任务可能超时");
      }

      log.info("异步任务执行结束");
    });

    result.onTimeout(() ->
            log.warn("任务执行超时了"));

    result.onCompletion(() ->
            log.info("结束回调"));

    result.onError(throwable ->
            log.error("出现异常", throwable));

    return result;
  }

  @GET("/callable")
  public Callable<String> callable() {
    return () -> {
      // 可以获取 RequestContext
      RequestContext request = RequestContextHolder.getRequired();
      log.info("异步任务开始执行");
      try {
        TimeUnit.SECONDS.sleep(2);
      }
      catch (InterruptedException e) {
        e.printStackTrace();
      }
      HttpHeaders headers = request.getHeaders();
      log.info(headers.toString());
      log.info("异步任务执行结束，开始返回");
      return "result from " + Thread.currentThread().getName();
    };
  }

  @GET("/web-async-task")
  public WebAsyncTask<String> webAsyncTask(@Nullable Long timeout) {
    return new WebAsyncTask<>(timeout, callable());
  }

}
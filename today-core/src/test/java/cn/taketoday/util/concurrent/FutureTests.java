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

package cn.taketoday.util.concurrent;

import junit.framework.AssertionFailedError;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import cn.taketoday.core.Pair;

import static cn.taketoday.util.concurrent.Future.ok;
import static cn.taketoday.util.concurrent.Future.whenAllComplete;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2024/3/22 14:31
 */
class FutureTests {

  @Test
  void validateInitialValues() {
    SettableFuture<Object> future = Future.forSettable();
    assertThat(future.isCancelled()).isFalse();
    assertThat(future.isDone()).isFalse();
    assertThat(future.isSuccess()).isFalse();
    assertThat(future.isFailed()).isFalse();
    assertThat(future.getCause()).isNull();
    assertThat(future.isCancellable()).isTrue();
    assertThat(future.getNow()).isNull();
    assertThatThrownBy(future::obtain)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Result is required");
  }

  @Test
  void validateValuesAfterCancel() {
    SettableFuture<Object> future = Future.forSettable();
    assertThat(future.cancel()).isTrue();
    assertThat(future.isCancelled()).isTrue();
    assertThat(future.isDone()).isTrue();
    assertThat(future.isSuccess()).isFalse();
    assertThat(future.isFailed()).isTrue();
    assertThat(future.getCause()).isNotNull().isInstanceOf(CancellationException.class);
    assertThat(future.isCancellable()).isFalse();
    assertThat(future.getNow()).isNull();
    assertThatThrownBy(future::obtain)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Result is required");

    assertThatThrownBy(future::get)
            .isInstanceOf(CancellationException.class);
  }

  @Test
  void zip() throws ExecutionException, InterruptedException {
    Pair<String, Integer> pair = Future.ok("2")
            .zip(Future.ok(1))
            .onSuccess(result -> {
              assertThat(result).isNotNull();
              assertThat(result.first).isEqualTo("2");
              assertThat(result.second).isEqualTo(1);
            })
            .onFailure((e) -> fail("never"))
            .get();

    assertThat(pair).isNotNull();
    assertThat(pair.first).isEqualTo("2");
    assertThat(pair.second).isEqualTo(1);

  }

  @Test
  void zipWith() throws ExecutionException, InterruptedException {
    Pair<String, Integer> pair = Future.ok("2")
            .zipWith(Future.ok(1), (first, second) -> {
              assertThat(first).isEqualTo("2");
              assertThat(second).isEqualTo(1);
              return Pair.of(first, second);
            })
            .onSuccess(result -> {
              assertThat(result).isNotNull();
              assertThat(result.first).isEqualTo("2");
              assertThat(result.second).isEqualTo(1);
            })
            .onFailure((e) -> fail("never"))
            .get();

    assertThat(pair).isNotNull();
    assertThat(pair.first).isEqualTo("2");
    assertThat(pair.second).isEqualTo(1);
  }

  @Test
  void errorHandling() throws InterruptedException {
    String string = Future.<String>failed(new RuntimeException())
            .errorHandling(e -> "recover")
            .await()
            .getNow();

    assertThat(string).isNotNull().isEqualTo("recover");
  }

  @Test
  void map() throws ExecutionException, InterruptedException {
    var length = Future.ok("ok")
            .map(String::length)
            .get();

    assertThat(length).isEqualTo(2);
  }

  // FutureCombiner

  @Test
  void whenAllComplete_noLeakInterruption() throws Exception {
    Callable<String> combiner = () -> "";

    Future<String> futureResult = whenAllComplete().call(combiner, directExecutor());

    assertThat(Thread.interrupted()).isFalse();
    futureResult.cancel(true);
    assertThat(Thread.interrupted()).isFalse();
  }

  @Test
  void whenAllComplete_wildcard() throws Exception {
    Future<?>[] futures = new Future<?>[0];
    Callable<String> combiner = () -> "hi";

    Future<String> future = whenAllComplete(ok("a"), ok("b"))
            .call(combiner, directExecutor());

    assertThat(future.get()).isEqualTo("hi");
    future = whenAllComplete(futures).call(combiner, directExecutor());
    assertThat(future.get()).isEqualTo("hi");
  }

  @Test
  void whenAllComplete_cancelledNotInterrupted() throws Exception {
    SettableFuture<String> stringFuture = Future.forSettable();
    SettableFuture<Boolean> booleanFuture = Future.forSettable();
    final CountDownLatch inFunction = new CountDownLatch(1);
    final CountDownLatch shouldCompleteFunction = new CountDownLatch(1);
    Callable<String> combiner = () -> {
      inFunction.countDown();
      shouldCompleteFunction.await();
      System.out.println("no interrupt");
      return "result";
    };

    Future<String> futureResult = Future.whenAllComplete(stringFuture, booleanFuture)
            .call(combiner, newSingleThreadExecutor());

    stringFuture.setSuccess("value");
    booleanFuture.setSuccess(true);
    inFunction.await();

    assertThat(futureResult.cancel(false)).isTrue();
    shouldCompleteFunction.countDown();
    try {
      futureResult.get();
      Assertions.fail();
    }
    catch (CancellationException expected) {
    }

  }

  @Test
  void whenAllComplete_interrupted() throws Exception {
    SettableFuture<String> stringFuture = Future.forSettable();
    SettableFuture<Boolean> booleanFuture = Future.forSettable();
    final CountDownLatch inFunction = new CountDownLatch(1);
    final CountDownLatch gotException = new CountDownLatch(1);
    Callable<String> combiner = () -> {
      inFunction.countDown();
      try {
        new CountDownLatch(1).await(); // wait for interrupt
      }
      catch (InterruptedException expected) {
        gotException.countDown();
        throw expected;
      }
      return "a";
    };

    Future<String> futureResult = whenAllComplete(stringFuture, booleanFuture)
            .call(combiner, newSingleThreadExecutor());

    stringFuture.setSuccess("value");
    booleanFuture.setSuccess(true);
    inFunction.await();
    futureResult.cancel(true);
    try {
      futureResult.get();
      Assertions.fail();
    }
    catch (CancellationException expected) {
    }
    gotException.await();
  }

  @Test
  void whenAllComplete_runnableResult() throws Exception {
    final SettableFuture<Integer> futureInteger = Future.forSettable();
    final SettableFuture<Boolean> futureBoolean = Future.forSettable();
    final String[] result = new String[1];
    Runnable combiner = () -> {
      assertTrue(futureInteger.isDone());
      assertTrue(futureBoolean.isDone());
      result[0] = createCombinedResult(futureInteger.obtain(), futureBoolean.obtain());
    };

    Future<?> futureResult = whenAllComplete(futureInteger, futureBoolean)
            .run(combiner, directExecutor());

    Integer integerPartial = 1;
    futureInteger.setSuccess(integerPartial);
    Boolean booleanPartial = true;
    futureBoolean.setSuccess(booleanPartial);
    futureResult.get();
    assertEquals(createCombinedResult(integerPartial, booleanPartial), result[0]);
  }

  @Test
  void whenAllComplete_runnableError() throws Exception {
    final RuntimeException thrown = new RuntimeException("test");

    final SettableFuture<Integer> futureInteger = Future.forSettable();
    final SettableFuture<Boolean> futureBoolean = Future.forSettable();
    Runnable combiner = () -> {
      assertTrue(futureInteger.isDone());
      assertTrue(futureBoolean.isDone());
      throw thrown;
    };

    Future<?> futureResult =
            whenAllComplete(futureInteger, futureBoolean).run(combiner, directExecutor());
    Integer integerPartial = 1;
    futureInteger.setSuccess(integerPartial);
    Boolean booleanPartial = true;
    futureBoolean.setSuccess(booleanPartial);

    try {
      futureResult.get();
      Assertions.fail();
    }
    catch (ExecutionException expected) {
      assertSame(thrown, expected.getCause());
    }
  }

  @Test
  void whenAllCompleteRunnable_resultCanceledWithoutInterrupt_doesNotInterruptRunnable()
          throws Exception {
    SettableFuture<String> stringFuture = Future.forSettable();
    SettableFuture<Boolean> booleanFuture = Future.forSettable();
    final CountDownLatch inFunction = new CountDownLatch(1);
    final CountDownLatch shouldCompleteFunction = new CountDownLatch(1);
    final CountDownLatch combinerCompletedWithoutInterrupt = new CountDownLatch(1);
    Runnable combiner = () -> {
      inFunction.countDown();
      try {
        shouldCompleteFunction.await();
        combinerCompletedWithoutInterrupt.countDown();
      }
      catch (InterruptedException e) {
        // Ensure the thread's interrupt status is preserved.
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
    };

    Future<?> futureResult =
            whenAllComplete(stringFuture, booleanFuture).run(combiner, newSingleThreadExecutor());

    stringFuture.setSuccess("value");
    booleanFuture.setSuccess(true);
    inFunction.await();
    futureResult.cancel(false);
    shouldCompleteFunction.countDown();
    try {
      futureResult.get();
      Assertions.fail();
    }
    catch (CancellationException expected) {
    }
    combinerCompletedWithoutInterrupt.await();
  }

  @Test
  void whenAllCompleteRunnable_resultCanceledWithInterrupt_InterruptsRunnable() throws Exception {
    SettableFuture<String> stringFuture = Future.forSettable();
    SettableFuture<Boolean> booleanFuture = Future.forSettable();
    final CountDownLatch inFunction = new CountDownLatch(1);
    final CountDownLatch gotException = new CountDownLatch(1);
    Runnable combiner = () -> {
      inFunction.countDown();
      try {
        new CountDownLatch(1).await(); // wait for interrupt
      }
      catch (InterruptedException expected) {
        // Ensure the thread's interrupt status is preserved.
        Thread.currentThread().interrupt();
        gotException.countDown();
      }
    };

    Future<?> futureResult = whenAllComplete(stringFuture, booleanFuture)
            .run(combiner, newSingleThreadExecutor());

    stringFuture.setSuccess("value");
    booleanFuture.setSuccess(true);
    inFunction.await();
    futureResult.cancel(true);
    try {
      futureResult.get();
      Assertions.fail();
    }
    catch (CancellationException expected) {
    }
    gotException.await();
  }

  @Test
  void whenAllSucceed() throws Exception {
    class PartialResultException extends Exception { }

    final SettableFuture<Integer> futureInteger = Future.forSettable();
    final SettableFuture<Boolean> futureBoolean = Future.forSettable();
    Callable<String> combiner = new Callable<String>() {
      @Override
      public String call() throws Exception {
        throw new AssertionFailedError("Callable should not have been called.");
      }
    };

    Future<String> futureResult = Future.whenAllSucceed(futureInteger, futureBoolean)
            .call(combiner, directExecutor());

    PartialResultException partialResultException = new PartialResultException();
    futureInteger.setFailure(partialResultException);
    Boolean booleanPartial = true;
    futureBoolean.setSuccess(booleanPartial);
    try {
      futureResult.get();
      Assertions.fail();
    }
    catch (ExecutionException expected) {
      assertSame(partialResultException, expected.getCause());
    }
  }

  @Test
  void whenAllSucceed_releasesInputFuturesUponSubmission() throws Exception {
    SettableFuture<Long> future1 = Future.forSettable();
    SettableFuture<Long> future2 = Future.forSettable();
    WeakReference<SettableFuture<Long>> future1Ref = new WeakReference<>(future1);
    WeakReference<SettableFuture<Long>> future2Ref = new WeakReference<>(future2);

    Callable<Long> combiner = new Callable<Long>() {
      @Override
      public Long call() {
        throw new AssertionError();
      }
    };

    Future<Long> unused = Future.whenAllSucceed(future1, future2)
            .call(combiner, noOpScheduledExecutor());

    future1.setSuccess(1L);
    future1 = null;
    future2.setSuccess(2L);
    future2 = null;

  }

  @Test
  void whenAllComplete_releasesInputFuturesUponCancellation() throws Exception {
    SettableFuture<Long> future = Future.forSettable();
    WeakReference<SettableFuture<Long>> futureRef = new WeakReference<>(future);

    Callable<Long> combiner = new Callable<Long>() {
      @Override
      public Long call() {
        throw new AssertionError();
      }
    };

    Future<Long> unused = Future.whenAllComplete(future)
            .call(combiner, noOpScheduledExecutor());

    unused.cancel(false);
    future = null;

  }

  static Executor directExecutor() {
    return Runnable::run;
  }

  static Executor noOpScheduledExecutor() {
    return runnable -> { };
  }

  private static String createCombinedResult(Integer i, Boolean b) {
    return "-" + i + "-" + b;
  }

}
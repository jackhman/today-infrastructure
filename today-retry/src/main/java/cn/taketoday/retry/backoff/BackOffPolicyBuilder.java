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

package cn.taketoday.retry.backoff;

/**
 * Fluent API for creating a {@link BackOffPolicy} based on given attributes. The delay
 * values are expressed in milliseconds. If any provided value is less than one, the
 * resulting policy will set it to one. The default policy is a {@link FixedBackOffPolicy}
 * with a delay of 1000ms.
 *
 * <p>
 * Examples: <pre>
 *
 * // Default {@link FixedBackOffPolicy} with 1000ms delay
 * BackOffPolicyBuilder
 * 		.newDefaultPolicy();
 *
 * // {@link FixedBackOffPolicy}
 * BackOffPolicyBuilder
 * 		.newBuilder()
 * 		.delay(2000)
 * 		.build();
 *
 * // {@link UniformRandomBackOffPolicy}
 * BackOffPolicyBuilder
 * 		.newBuilder()
 * 		.delay(500)
 * 		.maxDelay(1000)
 * 		.build();
 *
 * // {@link ExponentialBackOffPolicy}
 * BackOffPolicyBuilder
 * 		.newBuilder()
 * 		.delay(1000)
 * 		.maxDelay(5000)
 * 		.multiplier(2)
 * 		.build();
 *
 * // {@link ExponentialRandomBackOffPolicy} with provided {@link Sleeper}
 * BackOffPolicyBuilder
 * 		.newBuilder()
 * 		.delay(3000)
 * 		.maxDelay(5000)
 * 		.multiplier(1.5)
 * 		.random(true)
 * 		.sleeper(mySleeper)
 * 		.build();
 * </pre>
 * <p>
 * Not thread safe. Building should be performed in a single thread. The resulting
 * {@link BackOffPolicy} however is expected to be thread-safe and designed for moderate
 * load concurrent access.
 *
 * @author Tomaz Fernandes
 * @since 4.0
 */
public class BackOffPolicyBuilder {

  private static final long DEFAULT_INITIAL_DELAY = 1000L;

  private long delay = DEFAULT_INITIAL_DELAY;

  private long maxDelay;

  private double multiplier;

  private boolean random;

  private Sleeper sleeper;

  private BackOffPolicyBuilder() { }

  /**
   * Creates a new {@link BackOffPolicyBuilder} instance.
   *
   * @return the builder instance
   */
  public static BackOffPolicyBuilder newBuilder() {
    return new BackOffPolicyBuilder();
  }

  /**
   * Creates a new {@link FixedBackOffPolicy} instance with a delay of 1000ms.
   *
   * @return the back off policy instance
   */
  public static BackOffPolicy newDefaultPolicy() {
    return new BackOffPolicyBuilder().build();
  }

  /**
   * A canonical backoff period. Used as an initial value in the exponential case, and
   * as a minimum value in the uniform case.
   *
   * @param delay the initial or canonical backoff period in milliseconds
   * @return this
   */
  public BackOffPolicyBuilder delay(long delay) {
    this.delay = delay;
    return this;
  }

  /**
   * The maximum wait in milliseconds between retries. If less than {@link #delay(long)}
   * then a default value is applied depending on the resulting policy.
   *
   * @param maxDelay the maximum wait between retries in milliseconds
   * @return this
   */
  public BackOffPolicyBuilder maxDelay(long maxDelay) {
    this.maxDelay = maxDelay;
    return this;
  }

  /**
   * If positive, then used as a multiplier for generating the next delay for backoff.
   *
   * @param multiplier a multiplier to use to calculate the next backoff delay
   * @return this
   */
  public BackOffPolicyBuilder multiplier(double multiplier) {
    this.multiplier = multiplier;
    return this;
  }

  /**
   * In the exponential case ({@link #multiplier} &gt; 0) set this to true to have the
   * backoff delays randomized, so that the maximum delay is multiplier times the
   * previous delay and the distribution is uniform between the two values.
   *
   * @param random the flag to signal randomization is required
   * @return this
   */
  public BackOffPolicyBuilder random(boolean random) {
    this.random = random;
    return this;
  }

  /**
   * The {@link Sleeper} instance to be used to back off. Policies default to
   * {@link ThreadWaitSleeper}.
   *
   * @param sleeper the {@link Sleeper} instance
   * @return this
   */
  public BackOffPolicyBuilder sleeper(Sleeper sleeper) {
    this.sleeper = sleeper;
    return this;
  }

  /**
   * Builds the {@link BackOffPolicy} with the given parameters.
   *
   * @return the {@link BackOffPolicy} instance
   */
  public BackOffPolicy build() {
    if (this.multiplier > 0) {
      ExponentialBackOffPolicy policy = new ExponentialBackOffPolicy();
      if (this.random) {
        policy = new ExponentialRandomBackOffPolicy();
      }
      policy.setInitialInterval(this.delay);
      policy.setMultiplier(this.multiplier);
      policy.setMaxInterval(
              this.maxDelay > this.delay ? this.maxDelay : ExponentialBackOffPolicy.DEFAULT_MAX_INTERVAL);
      if (this.sleeper != null) {
        policy.setSleeper(this.sleeper);
      }
      return policy;
    }
    if (this.maxDelay > this.delay) {
      UniformRandomBackOffPolicy policy = new UniformRandomBackOffPolicy();
      policy.setMinBackOffPeriod(this.delay);
      policy.setMaxBackOffPeriod(this.maxDelay);
      if (this.sleeper != null) {
        policy.setSleeper(this.sleeper);
      }
      return policy;
    }
    FixedBackOffPolicy policy = new FixedBackOffPolicy();
    policy.setBackOffPeriod(this.delay);
    if (this.sleeper != null) {
      policy.setSleeper(this.sleeper);
    }
    return policy;
  }

}
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
package cn.taketoday.web.session;

import java.io.Serial;
import java.io.Serializable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

import cn.taketoday.core.AttributeAccessorSupport;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;

/**
 * Memory based {@link SessionRepository}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-09-28 10:31
 */
public class InMemorySessionRepository implements SessionRepository {

  private int maxSessions = 10000;
  private Clock clock = Clock.system(ZoneId.of("GMT"));

  private final SessionIdGenerator idGenerator;
  private final SessionEventDispatcher eventDispatcher;
  private final ExpiredSessionChecker expiredSessionChecker = new ExpiredSessionChecker();
  private final ConcurrentHashMap<String, InMemoryWebSession> sessions = new ConcurrentHashMap<>();

  public InMemorySessionRepository(SessionEventDispatcher eventDispatcher, SessionIdGenerator idGenerator) {
    Assert.notNull(idGenerator, "SessionIdGenerator is required");
    Assert.notNull(eventDispatcher, "SessionEventDispatcher is required");
    this.idGenerator = idGenerator;
    this.eventDispatcher = eventDispatcher;
  }

  /**
   * Set the maximum number of sessions that can be stored. Once the limit is
   * reached, any attempt to store an additional session will result in an
   * {@link IllegalStateException}.
   * <p>By default set to 10000.
   *
   * @param maxSessions the maximum number of sessions
   * @since 4.0
   */
  public void setMaxSessions(int maxSessions) {
    this.maxSessions = maxSessions;
  }

  /**
   * Return the maximum number of sessions that can be stored.
   *
   * @since 4.0
   */
  public int getMaxSessions() {
    return this.maxSessions;
  }

  /**
   * Configure the {@link Clock} to use to set lastAccessTime on every created
   * session and to calculate if it is expired.
   * <p>This may be useful to align to different timezone or to set the clock
   * back in a test, e.g. {@code Clock.offset(clock, Duration.ofMinutes(-31))}
   * in order to simulate session expiration.
   * <p>By default this is {@code Clock.system(ZoneId.of("GMT"))}.
   *
   * @param clock the clock to use
   * @since 4.0
   */
  public void setClock(Clock clock) {
    Assert.notNull(clock, "Clock is required");
    this.clock = clock;
    removeExpiredSessions();
  }

  /**
   * Return the configured clock for session lastAccessTime calculations.
   *
   * @since 4.0
   */
  public Clock getClock() {
    return this.clock;
  }

  @Override
  public int getSessionCount() {
    return sessions.size();
  }

  @Override
  public String[] getIdentifiers() {
    return StringUtils.toStringArray(sessions.keySet());
  }

  /**
   * Return the map of sessions with an {@link Collections#unmodifiableMap
   * unmodifiable} wrapper. This could be used for management purposes, to
   * list active sessions, invalidate expired ones, etc.
   */
  public Map<String, WebSession> getSessions() {
    return Collections.unmodifiableMap(this.sessions);
  }

  @Override
  public WebSession createSession() {
    // Opportunity to clean expired sessions
    Instant now = clock.instant();
    expiredSessionChecker.checkIfNecessary(now);
    return new InMemoryWebSession(idGenerator.generateId(), now);
  }

  @Override
  public WebSession retrieveSession(String id) {
    Instant now = clock.instant();
    expiredSessionChecker.checkIfNecessary(now);

    InMemoryWebSession session = sessions.get(id);
    if (session == null) {
      return null;
    }
    else if (session.isExpired(now)) {
      sessions.remove(id);
      return null;
    }
    else {
      session.updateLastAccessTime(now);
      return session;
    }
  }

  @Override
  public WebSession removeSession(String id) {
    return sessions.remove(id);
  }

  @Override
  public void updateLastAccessTime(WebSession session) {
    session.setLastAccessTime(clock.instant());
  }

  @Override
  public boolean contains(String id) {
    return sessions.containsKey(id);
  }

  /**
   * Check for expired sessions and remove them. Typically such checks are
   * kicked off lazily during calls to {@link #createSession() create} or
   * {@link #retrieveSession retrieve}, no less than 60 seconds apart.
   * This method can be called to force a check at a specific time.
   *
   * @since 4.0
   */
  public void removeExpiredSessions() {
    expiredSessionChecker.removeExpiredSessions(clock.instant());
  }

  final class InMemoryWebSession extends AttributeAccessorSupport implements WebSession, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final AtomicReference<String> id;

    private final Instant creationTime;

    private volatile Instant lastAccessTime;

    private volatile Duration maxIdleTime = Duration.ofMinutes(30);

    private final AtomicReference<State> state = new AtomicReference<>(State.NEW);

    InMemoryWebSession(String id, Instant creationTime) {
      this.id = new AtomicReference<>(id);
      this.creationTime = creationTime;
      this.lastAccessTime = this.creationTime;
    }

    @Override
    public String getId() {
      return id.get();
    }

    @Override
    public Instant getCreationTime() {
      return creationTime;
    }

    @Override
    public Instant getLastAccessTime() {
      return lastAccessTime;
    }

    @Override
    public void setLastAccessTime(Instant lastAccessTime) {
      this.lastAccessTime = lastAccessTime;
    }

    @Override
    public void changeSessionId() {
      sessions.remove(getId());
      String newId = idGenerator.generateId();
      id.set(newId);
      sessions.put(newId, this);
    }

    @Override
    public void invalidate() {
      state.set(State.EXPIRED);
      clearAttributes();
      eventDispatcher.onSessionDestroyed(this);
      sessions.remove(getId());
    }

    @Override
    public void save() {
      checkMaxSessionsLimit();

      // Implicitly started session..
      if (hasAttributes()) {
        state.compareAndSet(State.NEW, State.STARTED);
      }
      if (isStarted()) {
        // Save
        sessions.put(getId(), this);

        // Unless it was invalidated
        if (state.get().equals(State.EXPIRED)) {
          sessions.remove(getId());
          throw new IllegalStateException("Session was invalidated");
        }
      }
    }

    @Override
    public void setMaxIdleTime(Duration maxIdleTime) {
      this.maxIdleTime = maxIdleTime;
    }

    @Override
    public Duration getMaxIdleTime() {
      return this.maxIdleTime;
    }

    /**
     * Force the creation of a session causing the session id to be sent when
     * {@link #save()} is called.
     */
    @Override
    public void start() {
      state.compareAndSet(State.NEW, State.STARTED);
      eventDispatcher.onSessionCreated(this);
    }

    /**
     * Whether a session with the client has been started explicitly via
     * {@link #start()} or implicitly by adding session attributes.
     * If "false" then the session id is not sent to the client and the
     * {@link #save()} method is essentially a no-op.
     */
    @Override
    public boolean isStarted() {
      return state.get().equals(State.STARTED) || attributes != null;
    }

    private void checkMaxSessionsLimit() {
      if (sessions.size() >= maxSessions) {
        expiredSessionChecker.removeExpiredSessions(clock.instant());
        if (sessions.size() >= maxSessions) {
          throw new IllegalStateException("Max sessions limit reached: " + sessions.size());
        }
      }
    }

    @Override
    public boolean isExpired() {
      return isExpired(clock.instant());
    }

    private boolean isExpired(Instant now) {
      if (state.get().equals(State.EXPIRED)) {
        return true;
      }
      if (checkExpired(now)) {
        state.set(State.EXPIRED);
        return true;
      }
      return false;
    }

    private boolean checkExpired(Instant currentTime) {
      return isStarted()
              && !maxIdleTime.isNegative()
              && currentTime.minus(maxIdleTime).isAfter(lastAccessTime);
    }

    private void updateLastAccessTime(Instant currentTime) {
      this.lastAccessTime = currentTime;
    }

  }

  private final class ExpiredSessionChecker {

    /** Max time between expiration checks. */
    private static final int CHECK_PERIOD = 60 * 1000;

    private final ReentrantLock lock = new ReentrantLock();

    private Instant checkTime = clock.instant().plus(CHECK_PERIOD, ChronoUnit.MILLIS);

    public void checkIfNecessary(Instant now) {
      if (checkTime.isBefore(now)) {
        removeExpiredSessions(now);
      }
    }

    public void removeExpiredSessions(Instant now) {
      if (!sessions.isEmpty()) {
        if (lock.tryLock()) {
          try {
            Iterator<InMemoryWebSession> iterator = sessions.values().iterator();
            while (iterator.hasNext()) {
              InMemoryWebSession session = iterator.next();
              if (session.isExpired(now)) {
                iterator.remove();
                session.invalidate();
              }
            }
          }
          finally {
            this.checkTime = now.plus(CHECK_PERIOD, ChronoUnit.MILLIS);
            lock.unlock();
          }
        }
      }
    }
  }

  private enum State {
    NEW, STARTED, EXPIRED
  }

}
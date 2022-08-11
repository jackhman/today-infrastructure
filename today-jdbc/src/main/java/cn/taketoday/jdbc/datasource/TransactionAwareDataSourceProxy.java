/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.jdbc.datasource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;

/**
 * Proxy for a target JDBC {@link DataSource}, adding awareness of
 * Framework-managed transactions. Similar to a transactional JNDI DataSource
 * as provided by a Jakarta EE server.
 *
 * <p>Data access code that should remain unaware of  data access support
 * can work with this proxy to seamlessly participate in Framework-managed transactions.
 * Note that the transaction manager, for example {@link DataSourceTransactionManager},
 * still needs to work with the underlying DataSource, <i>not</i> with this proxy.
 *
 * <p><b>Make sure that TransactionAwareDataSourceProxy is the outermost DataSource
 * of a chain of DataSource proxies/adapters.</b> TransactionAwareDataSourceProxy
 * can delegate either directly to the target connection pool or to some
 * intermediary proxy/adapter like {@link LazyConnectionDataSourceProxy} or
 * {@link UserCredentialsDataSourceAdapter}.
 *
 * <p>Delegates to {@link DataSourceUtils} for automatically participating in
 * thread-bound transactions, for example managed by {@link DataSourceTransactionManager}.
 * {@code getConnection} calls and {@code close} calls on returned Connections
 * will behave properly within a transaction, i.e. always operate on the transactional
 * Connection. If not within a transaction, normal DataSource behavior applies.
 *
 * <p>This proxy allows data access code to work with the plain JDBC API and still
 * participate in Framework-managed transactions, similar to JDBC code in a Jakarta EE/JTA
 * environment. However, if possible, use  DataSourceUtils, JdbcTemplate or
 * JDBC operation objects to get transaction participation even without a proxy for
 * the target DataSource, avoiding the need to define such a proxy in the first place.
 *
 * <p>As a further effect, using a transaction-aware DataSource will apply remaining
 * transaction timeouts to all created JDBC (Prepared/Callable)Statement. This means
 * that all operations performed through standard JDBC will automatically participate
 * in Framework-managed transaction timeouts.
 *
 * <p><b>NOTE:</b> This DataSource proxy needs to return wrapped Connections (which
 * implement the {@link ConnectionProxy} interface) in order to handle close calls
 * properly. Use {@link Connection#unwrap} to retrieve the native JDBC Connection.
 *
 * @author Juergen Hoeller
 * @see DataSource#getConnection()
 * @see Connection#close()
 * @see DataSourceUtils#doGetConnection
 * @see DataSourceUtils#applyTransactionTimeout
 * @see DataSourceUtils#doReleaseConnection
 * @since 4.0
 */
public class TransactionAwareDataSourceProxy extends DelegatingDataSource {

  private boolean reobtainTransactionalConnections = false;

  /**
   * Create a new TransactionAwareDataSourceProxy.
   *
   * @see #setTargetDataSource
   */
  public TransactionAwareDataSourceProxy() { }

  /**
   * Create a new TransactionAwareDataSourceProxy.
   *
   * @param targetDataSource the target DataSource
   */
  public TransactionAwareDataSourceProxy(DataSource targetDataSource) {
    super(targetDataSource);
  }

  /**
   * Specify whether to reobtain the target Connection for each operation
   * performed within a transaction.
   * <p>The default is "false". Specify "true" to reobtain transactional
   * Connections for every call on the Connection proxy; this is advisable
   * on JBoss if you hold on to a Connection handle across transaction boundaries.
   * <p>The effect of this setting is similar to the
   * "hibernate.connection.release_mode" value "after_statement".
   */
  public void setReobtainTransactionalConnections(boolean reobtainTransactionalConnections) {
    this.reobtainTransactionalConnections = reobtainTransactionalConnections;
  }

  /**
   * Delegates to DataSourceUtils for automatically participating in Framework-managed
   * transactions. Throws the original SQLException, if any.
   * <p>The returned Connection handle implements the ConnectionProxy interface,
   * allowing to retrieve the underlying target Connection.
   *
   * @return a transactional Connection if any, a new one else
   * @see DataSourceUtils#doGetConnection
   * @see ConnectionProxy#getTargetConnection
   */
  @Override
  public Connection getConnection() throws SQLException {
    return getTransactionAwareConnectionProxy(obtainTargetDataSource());
  }

  /**
   * Wraps the given Connection with a proxy that delegates every method call to it
   * but delegates {@code close()} calls to DataSourceUtils.
   *
   * @param targetDataSource the DataSource that the Connection came from
   * @return the wrapped Connection
   * @see Connection#close()
   * @see DataSourceUtils#doReleaseConnection
   */
  protected Connection getTransactionAwareConnectionProxy(DataSource targetDataSource) {
    return (Connection) Proxy.newProxyInstance(
            ConnectionProxy.class.getClassLoader(),
            new Class<?>[] { ConnectionProxy.class },
            new TransactionAwareInvocationHandler(targetDataSource));
  }

  /**
   * Determine whether to obtain a fixed target Connection for the proxy
   * or to reobtain the target Connection for each operation.
   * <p>The default implementation returns {@code true} for all
   * standard cases. This can be overridden through the
   * {@link #setReobtainTransactionalConnections "reobtainTransactionalConnections"}
   * flag, which enforces a non-fixed target Connection within an active transaction.
   * Note that non-transactional access will always use a fixed Connection.
   *
   * @param targetDataSource the target DataSource
   */
  protected boolean shouldObtainFixedConnection(DataSource targetDataSource) {
    return !TransactionSynchronizationManager.isSynchronizationActive() || !this.reobtainTransactionalConnections;
  }

  /**
   * Invocation handler that delegates close calls on JDBC Connections
   * to DataSourceUtils for being aware of thread-bound transactions.
   */
  private class TransactionAwareInvocationHandler implements InvocationHandler {

    private final DataSource targetDataSource;

    @Nullable
    private Connection target;

    private boolean closed = false;

    public TransactionAwareInvocationHandler(DataSource targetDataSource) {
      this.targetDataSource = targetDataSource;
    }

    @Override
    @Nullable
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      // Invocation on ConnectionProxy interface coming in...

      switch (method.getName()) {
        case "equals":
          // Only considered as equal when proxies are identical.
          return (proxy == args[0]);
        case "hashCode":
          // Use hashCode of Connection proxy.
          return System.identityHashCode(proxy);
        case "toString":
          // Allow for differentiating between the proxy and the raw Connection.
          StringBuilder sb = new StringBuilder("Transaction-aware proxy for target Connection ");
          if (this.target != null) {
            sb.append('[').append(this.target).append(']');
          }
          else {
            sb.append(" from DataSource [").append(this.targetDataSource).append(']');
          }
          return sb.toString();
        case "close":
          // Handle close method: only close if not within a transaction.
          DataSourceUtils.doReleaseConnection(this.target, this.targetDataSource);
          this.closed = true;
          return null;
        case "isClosed":
          return this.closed;
        case "unwrap":
          if (((Class<?>) args[0]).isInstance(proxy)) {
            return proxy;
          }
          break;
        case "isWrapperFor":
          if (((Class<?>) args[0]).isInstance(proxy)) {
            return true;
          }
          break;
      }

      if (this.target == null) {
        if (method.getName().equals("getWarnings") || method.getName().equals("clearWarnings")) {
          // Avoid creation of target Connection on pre-close cleanup (e.g. Hibernate Session)
          return null;
        }
        if (this.closed) {
          throw new SQLException("Connection handle already closed");
        }
        if (shouldObtainFixedConnection(this.targetDataSource)) {
          this.target = DataSourceUtils.doGetConnection(this.targetDataSource);
        }
      }
      Connection actualTarget = this.target;
      if (actualTarget == null) {
        actualTarget = DataSourceUtils.doGetConnection(this.targetDataSource);
      }

      if (method.getName().equals("getTargetConnection")) {
        // Handle getTargetConnection method: return underlying Connection.
        return actualTarget;
      }

      // Invoke method on target Connection.
      try {
        Object retVal = method.invoke(actualTarget, args);

        // If return value is a Statement, apply transaction timeout.
        // Applies to createStatement, prepareStatement, prepareCall.
        if (retVal instanceof Statement) {
          DataSourceUtils.applyTransactionTimeout((Statement) retVal, this.targetDataSource);
        }

        return retVal;
      }
      catch (InvocationTargetException ex) {
        throw ex.getTargetException();
      }
      finally {
        if (actualTarget != this.target) {
          DataSourceUtils.doReleaseConnection(actualTarget, this.targetDataSource);
        }
      }
    }
  }

}
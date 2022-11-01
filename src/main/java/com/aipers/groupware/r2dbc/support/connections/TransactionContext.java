package com.aipers.groupware.r2dbc.support.connections;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.IsolationLevel;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Getter
@Setter
public class TransactionContext {

  private final Supplier<UUID> uuid;

  private final boolean readOnly;
  private final TransactionIsolation isolation;

  private SQLConnection connection;

  private String transactionId;
  private boolean transactionReadonly;
  private Integer transactionIsolation;
  private Long transactionTimeout = 3000L;
  private boolean transactionActive = false;
  private boolean transactionRollbackFor = false;

  public TransactionContext() {
    this(false, 3000L, TransactionIsolation.READ_UNCOMMITTED);
  }

  public TransactionContext(
      final boolean readOnly, final Long timeoutMilli, final TransactionIsolation isolation
  ) {
    this.uuid = (UUID::randomUUID);
    this.transactionTimeout = timeoutMilli;
    this.isolation = isolation;
    this.readOnly = readOnly;
  }

  public UUID getUuid() {
    return this.uuid.get();
  }

  public TransactionContext setConnection(final Connection connection) {
    this.connection = new SQLConnection(connection, this.transactionTimeout);
    this.connection.begin();
    this.transactionActive = true;

    return this;
  }

  public boolean isConnection() {
    return null != this.connection && null != this.connection.getConnection();
  }

  public boolean isTransactionActive() {
    return this.transactionActive && isConnection();
  }

  public Connection getR2dbcConnection() {
    return this.isConnection()
        ? this.connection.getConnection()
        : null;
  }

  public void complete() {
    this.connection.clear(true);
    this.connection = null;
    this.clear();
  }

  public void rollback() {
    this.connection.clear(false);
    this.connection = null;
    this.clear();
  }

  public void clear() {
    this.transactionId = null;
    this.transactionActive = false;
    this.transactionReadonly = false;
    this.transactionIsolation = null;
    this.transactionRollbackFor = false;

    Optional.ofNullable(this.connection).ifPresent(conn -> {
      this.connection.clear(false);
      this.connection = null;
    });
  }

  private class SQLConnection implements AutoCloseable {

    private final long createdAt;
    private final Long timeoutMilli;
    @Getter
    private final Connection connection;

    private SQLConnection(final Connection connection, final Long timeoutMilli) {
      this.createdAt = Instant.now().toEpochMilli();
      this.connection = connection;
      this.timeoutMilli = timeoutMilli;
      this.connection.setStatementTimeout(Duration.ofMillis(this.timeoutMilli));
    }

    public boolean isTimeout() {
      return createdAt < Instant.now().toEpochMilli() - timeoutMilli;
    }

    private void setIsolationLevel(final IsolationLevel isolationLevel) {
      this.connection.setTransactionIsolationLevel(isolationLevel);
    }

    private Mono<Void> begin() {
      return Mono.zip(
          Mono.from(this.connection.setTransactionIsolationLevel(isolation.getIsolationLevel())),
          Mono.from(this.connection.setAutoCommit(false)),
          Mono.from(this.connection.beginTransaction())
      ).flatMap(tuple -> Mono.empty());
    }

    private Mono<Void> commit() {
      return Mono.from(this.connection.commitTransaction());
    }

    private Mono<Void> rollback() {
      return Mono.from(this.connection.rollbackTransaction());
    }

    private void clear(final boolean isSuccess) {
      (isSuccess ? commit() : rollback())
          .subscribe(null, null, () -> this.connection.close());
    }

    @Override
    public void close() {

      Mono.from(this.connection.close()).subscribe((Void v) -> log.debug("Connection closed !!"));
    }

  }

}

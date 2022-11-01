package com.aipers.groupware.r2dbc.support.connections;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Function;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

@Slf4j
public class TransactionManager {

  private static TransactionManager _instance;

  private final ConnectionFactory factory;

  @Getter
  private final boolean useSpringTransaction;

  private TransactionManager(final ConnectionFactory connectionFactory) {
    this._instance = this;
    this.factory = connectionFactory;

    boolean inUseSpringTransaction = false;
    try {
      Class.forName("org.springframework.r2dbc.connection.ConnectionFactoryUtils");
      inUseSpringTransaction = true;
    } catch (ClassNotFoundException e) {}

    if (inUseSpringTransaction) {
      this.useSpringTransaction = true;
      log.info("Use Transaction Manager managed by spring");
    } else {
      this.useSpringTransaction = false;
    }
  }

  public ConnectionFactory getConnectionFactory() {
    return _instance.factory;
  }

  public static final void initialize(final ConnectionFactory connectionFactory) {
    if (null == _instance) new TransactionManager(connectionFactory);
  }

  /**
   * Spring 에서 관리하는 현재 컨텍스트의 R2dbc 트랜잭션 커넥션을 invoke 합니다.
   * @return
   */
  private Mono<? extends Connection> getSpringConnection() {
    try {
      final Method method = Class
          .forName("org.springframework.r2dbc.connection.ConnectionFactoryUtils")
          .getMethod("getConnection", ConnectionFactory.class);

      return (Mono<Connection>) method.invoke(null, _instance.getConnectionFactory());
    } catch (ClassNotFoundException | NoSuchMethodException |
             IllegalAccessException | InvocationTargetException e
    ) {
      throw new RuntimeException("Could not find the Transaction Manager managed by spring");
    }
  }

  /**
   * R2dbc 커넥션을 생성합니다
   * @return
   */
  public static Mono<Connection> createConnection() {
    return _instance.isUseSpringTransaction()
      ? (Mono<Connection>) _instance.getSpringConnection()
      : Mono.defer(() -> Mono.from(_instance.factory.create()));
  }

  /**
   * R2dbc 커넥션을 Wrapping 하는 Transaction Context 생성합니다.
   * @return
   */
  public static Mono<TransactionContext> createTransaction() {
    return Mono.zip(
            Mono.deferContextual(context -> Mono.just(context.get(TransactionContext.class))),
            createConnection()
        )
        .contextWrite(getOrCreateTransactionContext())
        .map(tuple -> {
          tuple.getT1().setConnection(tuple.getT2());

          return tuple.getT1();
        });
  }

  /**
   * 현재 컨텍스트의 Transaction Context 반환합니다.
   * ※ 주의: NULL 리턴될 수 있습니다.
   * @return
   */
  public static Mono<TransactionContext> getCurrentTransaction() {
    return Mono.deferContextual(context -> Mono.just(context.get(TransactionContext.class)));
  }

  /**
   * 현재 컨텍스트의 Transaction Context 에서 트랜잭션 커넥션을 반환합니다.
   * 없는 경우 새 Transaction Context 생성하고 트랜잭션 커넥션을 새로 생성하여 반환합니다.
   * 이 메서드는 proxy 에서 메서드의 Transaction 처리를 위해서 사용해야 합니다.
   * ※ 주의: Fetch 에서 사용하게 되면 하나의 쿼리에 대해 트랜잭션이 생성되어 처리됩니다.
   * @return
   */
  public static Mono<TransactionContext> getOrCreateTransaction(
      final boolean readOnly, final long timeoutMilli, final TransactionIsolation isolation
  ) {
    return Mono
        .deferContextual(context -> Optional
                .ofNullable(context.get(TransactionContext.class))
                .map(ctx -> ctx.isTransactionActive() ? Mono.just(ctx) : null)
                .orElse(createTransaction())
        )
        .contextWrite(getOrCreateTransactionContext(readOnly, timeoutMilli, isolation));
  }

  /**
   * 현재 컨텍스트의 Transaction Context 반환합니다.
   * 없는 경우 새 Transaction Context 생성하여 반환합니다.
   * Transaction 옵션을 지정 할 수 있습니다. (readonly, isolation, timeout)
   * @return
   */
  public static Function<Context, Context> getOrCreateTransactionContext() {
    return getOrCreateTransactionContext(false, 3000L, TransactionIsolation.READ_UNCOMMITTED);
  }
  public static Function<Context, Context> getOrCreateTransactionContext(
      final boolean readOnly, final long timeoutMilli, final TransactionIsolation isolation
  ) {
    return context -> context.hasKey(TransactionContext.class)
        ? context
        : context.put(
            TransactionContext.class,
            new TransactionContext(readOnly, timeoutMilli, isolation)
        );
  }

}

package com.aipers.groupware.r2dbc.support.aspects;

import com.aipers.groupware.r2dbc.support.connections.TransactionContext;
import com.aipers.groupware.r2dbc.support.connections.TransactionManager;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public abstract class TransactingInvocationProcessor {

  public static Mono<?> monoProceed(final Mono<?> proxy, final Transacting transacting) {
    return TransactionManager
        .getOrCreateTransaction(transacting.readOnly(), transacting.timeout(), transacting.isolation())
        .flatMap(transaction ->
            proxy.doOnTerminate(() -> {
              log.debug("Proxy TX :: rollback ?? {}", transaction.isTransactionRollbackFor());
              transaction.complete();
            })
            .doAfterTerminate(() -> log.debug("Mono interceptor proxy complete"))
            .contextWrite(context -> context.put(TransactionContext.class, transaction))
        );
  }

  public static Flux<?> fluxProceed(final Flux<?> proxy, final Transacting transacting) {
    return TransactionManager
        .getOrCreateTransaction(transacting.readOnly(), transacting.timeout(), transacting.isolation())
        .flatMapMany(transaction ->
            proxy.doOnTerminate(() -> {
              log.debug("Proxy TX :: rollback ?? {}", transaction.isTransactionRollbackFor());
              transaction.complete();
            })
            .doAfterTerminate(() -> log.debug("Flux interceptor proxy complete"))
            .contextWrite(context -> context.put(TransactionContext.class, transaction))
        );
  }

}

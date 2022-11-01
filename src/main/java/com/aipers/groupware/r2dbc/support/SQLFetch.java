package com.aipers.groupware.r2dbc.support;

import com.aipers.groupware.r2dbc.support.connections.TransactionContext;
import com.aipers.groupware.r2dbc.support.connections.TransactionException;
import com.aipers.groupware.r2dbc.support.connections.TransactionManager;
import com.aipers.groupware.r2dbc.support.models.SQLQueryType;
import com.aipers.groupware.r2dbc.support.models.SQLSequenceQuery;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import io.r2dbc.spi.Statement;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class SQLFetch<T> {

  private final Function<Connection, Statement> statementFunction;
  private final SQLSequenceQuery sqlSequenceQuery;
  private final SQLQueryType type;
  private BiFunction<Row, RowMetadata, T> rowMapper;

  public SQLFetch(
      final Function<Connection, Statement> statementFunction,
      final SQLQueryType type, final SQLSequenceQuery sqlSequenceQuery,
      final BiFunction<Row, RowMetadata, T> rowMapper
  ) {
    this.statementFunction = statementFunction;
    this.sqlSequenceQuery = sqlSequenceQuery;
    this.rowMapper = rowMapper;
    this.type = type;
  }

  public SQLFetch setRowMapper(final BiFunction<Row, RowMetadata, T> rowMapper) {
    this.rowMapper = rowMapper;
    return this;
  }

  public Mono<T> first() {
    return this.first(false);
  }
  public Mono<T> first(final boolean useNewTransaction) {
    return all(useNewTransaction).next();
  }

  public Mono<T> one() {
    return this.one(false);
  }
  public Mono<T> one(final boolean useNewTransaction) {
    return all(useNewTransaction)
        .buffer(2)
        .flatMap(list -> {
          if (list.isEmpty()) return Mono.empty();
          if (1 < list.size()) Mono.error(new SQLExecuteException("Too many results error"));

          return Mono.just(list.get(0));
        })
        .next();
  }

  public Flux<T> all() {
    return this.all(false);
  }
  public Flux<T> all(final boolean useNewTransaction) {
    return run(this.getTransaction(useNewTransaction))
        .flatMap(result -> result.map(rowMapper::apply));
  }

  public Mono<Long> rowsUpdated() {
    return this.rowsUpdated(false);
  }
  public Mono<Long> rowsUpdated(final boolean useNewTransaction) {
    return run(this.getTransaction(useNewTransaction))
        .flatMap(Result::getRowsUpdated)
        .collect(Collectors.summingLong(Long::longValue))
        .doOnSuccess(affectedRows -> {
          Optional
              .ofNullable(this.sqlSequenceQuery)
              .ifPresent(query -> {
                // TODO: SelectKey Query execute !!
              });
        });
  }

  private Flux<Result> run(final Mono<TransactionContext> connectionWrapper) {
    return connectionWrapper.flatMapMany(transaction -> {
      if (transaction.isReadOnly() && SQLQueryType.SELECT != type) {
        return Flux.error(
            new SQLExecuteException("Type of query that cannot be executed in a read-only session"));
      }

      if (!transaction.isTransactionActive()) {
        return Flux.error(
            new TransactionException("Transactions in the current context are not active"));
      }

      return Flux
          .from(statementFunction.apply(transaction.getR2dbcConnection()).execute())
          .onErrorResume((t) -> {
            transaction.setTransactionRollbackFor(true);
            return Flux.error(t);
          });
    });
  }

  private Mono<TransactionContext> getTransaction(final boolean isNew) {
    return isNew
        ? newTransaction()
        : TransactionManager.getCurrentTransaction().switchIfEmpty(newTransaction());
  }

  private Mono<TransactionContext> newTransaction() {
    return TransactionManager.createTransaction()
        .flatMap(transaction -> Mono.just(transaction).doOnTerminate(() -> transaction.complete()));
  }

}

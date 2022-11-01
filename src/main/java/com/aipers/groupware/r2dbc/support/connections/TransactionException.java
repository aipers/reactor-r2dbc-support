package com.aipers.groupware.r2dbc.support.connections;

public class TransactionException extends RuntimeException {

  public TransactionException() {
    super();
  }

  public TransactionException(final String message) {
    super(message);
  }

  public TransactionException(final String message, Throwable t) {
    super(message, t);
  }

}

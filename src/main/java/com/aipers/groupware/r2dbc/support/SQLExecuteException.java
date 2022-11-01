package com.aipers.groupware.r2dbc.support;

public class SQLExecuteException extends RuntimeException {

  public SQLExecuteException() {
    super();
  }

  public SQLExecuteException(final String message) {
    super(message);
  }

}

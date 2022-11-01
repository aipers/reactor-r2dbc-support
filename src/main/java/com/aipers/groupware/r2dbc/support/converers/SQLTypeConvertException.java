package com.aipers.groupware.r2dbc.support.converers;

public class SQLTypeConvertException extends RuntimeException {

  public SQLTypeConvertException() {
  }

  public SQLTypeConvertException(final String message) {
    super(message);
  }

  public static SQLTypeConvertException found() {
    return new SQLTypeConvertException("Type converter not found");
  }

}

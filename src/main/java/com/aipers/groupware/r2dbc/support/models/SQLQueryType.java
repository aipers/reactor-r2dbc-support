package com.aipers.groupware.r2dbc.support.models;

import com.aipers.groupware.r2dbc.support.SQLLoadException;
import java.util.Arrays;

public enum SQLQueryType {

  SELECT,
  INSERT,
  UPDATE,
  DELETE,

  SQL,

  IF,
  WHEN,
  OTHERWISE,

  WHERE,
  CHOOSE,
  TRIM,
  SET,

  FOREACH,
  SELECTKEY;

  public static SQLQueryType of(final String type) {
    return Arrays.stream(SQLQueryType.values())
        .filter(queryType -> type.toUpperCase().equals(queryType.name()))
        .findFirst()
        .orElseThrow(SQLLoadException::new);
  }

}

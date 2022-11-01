package com.aipers.groupware.r2dbc.support.connections;

import io.r2dbc.spi.IsolationLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TransactionIsolation {

  SERIALIZABLE(IsolationLevel.SERIALIZABLE),
  READ_COMMITTED(IsolationLevel.READ_COMMITTED),
  READ_UNCOMMITTED(IsolationLevel.READ_UNCOMMITTED),
  REPEATABLE_READ(IsolationLevel.REPEATABLE_READ);

  @Getter
  private final IsolationLevel isolationLevel;

}

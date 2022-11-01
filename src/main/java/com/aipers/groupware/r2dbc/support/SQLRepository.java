package com.aipers.groupware.r2dbc.support;

public interface SQLRepository {

  default String namespace() {
    return getClass().getInterfaces()[0].getName();
  }

  default String id() {
    return Thread.currentThread().getStackTrace()[2].getMethodName();
  }

}

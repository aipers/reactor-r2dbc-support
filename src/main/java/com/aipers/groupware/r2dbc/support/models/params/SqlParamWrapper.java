package com.aipers.groupware.r2dbc.support.models.params;

import com.aipers.groupware.r2dbc.support.models.SQLParamType;

public interface SqlParamWrapper {

  Object getValue();
  Object get(String key);

  SQLParamType getType();
  Class getType(String key);

  void set(String key, Object value);


}


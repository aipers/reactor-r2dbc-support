package com.aipers.groupware.r2dbc.support.models.params;

import com.aipers.groupware.r2dbc.support.models.SQLParamType;
import java.util.HashMap;
import java.util.Map;

public class IterableSqlParam implements SqlParamWrapper {

  private final SQLParamType type = SQLParamType.ITERABLE;

  private final Map<String, Object> value;

  public IterableSqlParam(final Object param) {
    this.value = new HashMap<String, Object>() {
      {
        put("list", param);
      }
    };
  }

  @Override
  public Object getValue() {
    return this.value.get("list");
  }

  @Override
  public Object get(final String key) {
    return this.value.get(key.startsWith("foreach") ? key : "list");
  }

  @Override
  public void set(final String key, final Object value) {
    this.value.put(key, value);
  }

  @Override
  public Class getType(final String key) {
    return Iterable.class;
  }

  @Override
  public SQLParamType getType() {
    return this.type;
  }

}

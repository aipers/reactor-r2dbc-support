package com.aipers.groupware.r2dbc.support.models.params;

import com.aipers.groupware.r2dbc.support.models.SQLParamType;

public class EnumSqlParam implements SqlParamWrapper {

  private final SQLParamType type = SQLParamType.ENUM;

  private final Enum value;

  public EnumSqlParam(final Enum value) {
    this.value = value;
  }

  @Override
  public Object get(String key) {
    return getValue();
  }

  @Override
  public void set(String key, Object value) {
    throw new IllegalAccessError("Set not support enum type");
  }

  @Override
  public Object getValue() {
    return value.name();
  }

  @Override
  public Class getType(String key) {
    return String.class;
  }

  @Override
  public SQLParamType getType() {
    return this.type;
  }

}

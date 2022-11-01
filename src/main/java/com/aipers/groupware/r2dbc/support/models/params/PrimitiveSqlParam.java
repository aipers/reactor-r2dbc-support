package com.aipers.groupware.r2dbc.support.models.params;

import com.aipers.groupware.r2dbc.support.models.SQLParamType;
import lombok.Getter;

public class PrimitiveSqlParam implements SqlParamWrapper {

  private final SQLParamType type = SQLParamType.PRIMITIVE;

  @Getter
  private final Object value;
  private final Class paramType;

  public PrimitiveSqlParam(final Object value, final Class paramType) {
    this.value = value;
    this.paramType = paramType;
  }

  @Override
  public Object get(String key) {
    return value;
  }

  @Override
  public void set(String key, Object value) {
    throw new IllegalAccessError("Set not support primitive type");
  }

  @Override
  public Class getType(String key) {
    return paramType;
  }

  @Override
  public SQLParamType getType() {
    return this.type;
  }

}

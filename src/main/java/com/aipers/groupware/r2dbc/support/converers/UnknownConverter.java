package com.aipers.groupware.r2dbc.support.converers;

public class UnknownConverter implements SQLResultTypeConverter<Object> {

  @Override
  public boolean isSupport(final Object value) {
    return true;
  }

  @Override
  public Object convert(final Object value) {
    return null;
  }

}

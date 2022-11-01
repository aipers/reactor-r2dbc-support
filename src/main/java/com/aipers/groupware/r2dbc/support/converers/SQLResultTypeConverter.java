package com.aipers.groupware.r2dbc.support.converers;

public interface SQLResultTypeConverter<T> {

  boolean isSupport(Object value);

  T convert(Object value);

}

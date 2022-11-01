package com.aipers.groupware.r2dbc.support.converers;

public class LongConverter implements SQLResultTypeConverter<Long> {

  @Override
  public boolean isSupport(final Object value) {
    return Number.class.isAssignableFrom(value.getClass())
        || String.class.isAssignableFrom(value.getClass());
  }

  @Override
  public Long convert(final Object value) {
    if (Number.class.isAssignableFrom(value.getClass())) return ((Number) value).longValue();
    if (String.class.isAssignableFrom(value.getClass())) return Long.parseLong((String) value);

    return Long.parseLong(String.valueOf(value));
  }

}

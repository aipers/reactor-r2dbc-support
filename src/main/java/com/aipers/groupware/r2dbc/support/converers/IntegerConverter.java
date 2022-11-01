package com.aipers.groupware.r2dbc.support.converers;

public class IntegerConverter implements SQLResultTypeConverter<Integer> {

  @Override
  public boolean isSupport(final Object value) {
    return String.class.isAssignableFrom(value.getClass())
        || Integer.class.isAssignableFrom(value.getClass());
  }

  @Override
  public Integer convert(final Object value) {
    if (Number.class.isAssignableFrom(value.getClass())) return ((Number) value).intValue();
    if (String.class.isAssignableFrom(value.getClass())) return Integer.parseInt((String) value);

    return Integer.parseInt(String.valueOf(value));
  }

}

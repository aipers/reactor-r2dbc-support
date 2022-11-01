package com.aipers.groupware.r2dbc.support.converers;

public class ShortConverter implements SQLResultTypeConverter<Short> {

  @Override
  public boolean isSupport(final Object value) {
    return Number.class.isAssignableFrom(value.getClass())
        || String.class.isAssignableFrom(value.getClass());
  }

  @Override
  public Short convert(final Object value) {
    if (Number.class.isAssignableFrom(value.getClass())) return ((Number) value).shortValue();
    if (String.class.isAssignableFrom(value.getClass())) return Short.parseShort((String) value);

    return Short.parseShort(String.valueOf(value));
  }

}

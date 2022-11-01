package com.aipers.groupware.r2dbc.support.converers;

public class FloatConverter implements SQLResultTypeConverter<Float> {

  @Override
  public boolean isSupport(final Object value) {
    return Number.class.isAssignableFrom(value.getClass())
        || String.class.isAssignableFrom(value.getClass());
  }

  @Override
  public Float convert(final Object value) {
    if (Number.class.isAssignableFrom(value.getClass())) return ((Number) value).floatValue();
    if (String.class.isAssignableFrom(value.getClass())) return Float.parseFloat((String) value);

    return Float.parseFloat(String.valueOf(value));
  }

}

package com.aipers.groupware.r2dbc.support.converers;

public class DoubleConverter implements SQLResultTypeConverter<Double> {

  @Override
  public boolean isSupport(final Object value) {
    return Number.class.isAssignableFrom(value.getClass())
        || String.class.isAssignableFrom(value.getClass());
  }

  @Override
  public Double convert(final Object value) {
    if (Number.class.isAssignableFrom(value.getClass())) return ((Number) value).doubleValue();
    if (String.class.isAssignableFrom(value.getClass())) return Double.parseDouble((String) value);

    return Double.parseDouble(String.valueOf(value));
  }

}

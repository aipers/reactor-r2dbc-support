package com.aipers.groupware.r2dbc.support.converers;

import java.math.BigDecimal;

public class BigDecimalConvert implements SQLResultTypeConverter<BigDecimal> {

  @Override
  public boolean isSupport(final Object value) {
    return Number.class.isAssignableFrom(value.getClass());
  }

  @Override
  public BigDecimal convert(final Object value) {
    if (Double.class.isAssignableFrom(value.getClass())) BigDecimal.valueOf((Double) value);
    if (Float.class.isAssignableFrom(value.getClass())) BigDecimal.valueOf((Float) value);
    if (Long.class.isAssignableFrom(value.getClass())) BigDecimal.valueOf((Long) value);
    if (Integer.class.isAssignableFrom(value.getClass())) BigDecimal.valueOf((Integer) value);

    return null;
  }

}

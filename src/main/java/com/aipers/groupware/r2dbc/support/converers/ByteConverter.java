package com.aipers.groupware.r2dbc.support.converers;

public class ByteConverter implements SQLResultTypeConverter<Byte> {

  @Override
  public boolean isSupport(final Object value) {
    return Byte.class.isAssignableFrom(value.getClass())
        || String.class.isAssignableFrom(value.getClass());
  }

  @Override
  public Byte convert(final Object value) {
    if (Byte.class.isAssignableFrom(value.getClass())) return (Byte) value;
    if (String.class.isAssignableFrom(value.getClass())) return convertChar(((String) value).charAt(0));

    return convertChar(String.valueOf(value).charAt(0));
  }

  private Byte convertChar(final char c) {
    byte result = (byte) c;

    return result;
  }

}

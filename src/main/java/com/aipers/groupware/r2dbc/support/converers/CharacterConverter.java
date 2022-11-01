package com.aipers.groupware.r2dbc.support.converers;

public class CharacterConverter implements SQLResultTypeConverter<Character> {

  @Override
  public boolean isSupport(final Object value) {
    return Character.class.isAssignableFrom(value.getClass())
        || String.class.isAssignableFrom(value.getClass());
  }

  @Override
  public Character convert(final Object value) {
    if (Character.class.isAssignableFrom(value.getClass())) return (Character) value;
    if (String.class.isAssignableFrom(value.getClass())) return ((String) value).charAt(0);

    return null;
  }

}

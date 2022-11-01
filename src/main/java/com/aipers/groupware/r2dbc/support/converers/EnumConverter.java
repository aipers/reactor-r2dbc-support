package com.aipers.groupware.r2dbc.support.converers;

import com.aipers.groupware.common.utilities.CommonUtils;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EnumConverter implements SQLResultTypeConverter<Enum> {

  @Setter
  private Class<? extends Enum> targetType;

  @Override
  public boolean isSupport(final Object value) {
    return Enum.class.isAssignableFrom(value.getClass());
  }

  @Override
  public Enum convert(final Object value) {
    if (CommonUtils.isEmpty(value)) return null;

    return Enum.valueOf(targetType, String.valueOf(value));
  }

}

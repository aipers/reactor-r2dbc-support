package com.aipers.groupware.r2dbc.support.models.params;

import com.aipers.groupware.r2dbc.support.models.SQLParamType;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BeanSqlParam implements SqlParamWrapper {

  private final SQLParamType type = SQLParamType.BEAN;

  @Getter
  private final ReflectionWrapper value;
  private final Map<String, Object> params;

  public BeanSqlParam(final Object value) {
    this.params = new HashMap<>();
    this.value = new ReflectionWrapper(value);
  }

  @Override
  public Object get(final String key) {
    return this.value.isReadableProperty(key)
        ? Enum.class.isAssignableFrom(this.getType(key))
            ? String.valueOf(this.value.getPropertyValue(key))
            : this.value.getPropertyValue(key)
        : this.params.get(key);
  }

  @Override
  public void set(final String key, final Object value) {
    if (this.value.isWritableProperty(key))
      this.value.setPropertyValue(key, value);
    else
      this.params.put(key, value);
  }

  @Override
  public Class getType(final String key) {
    return this.value.isReadableProperty(key)
      ? this.value.getPropertyType(key)
      : this.params.get(key).getClass();
  }

  @Override
  public SQLParamType getType() {
    return this.type;
  }

}

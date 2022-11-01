package com.aipers.groupware.r2dbc.support.models.params;

import com.aipers.groupware.r2dbc.support.models.SQLParamType;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MapSqlParam implements SqlParamWrapper {

  private final SQLParamType type = SQLParamType.MAP;

  @Getter
  private final Map<String, Object> value;

  @Override
  public Object get(final String key) {
    return Enum.class.isAssignableFrom(this.getType(key))
      ? String.valueOf(this.value.get(key))
      : this.value.get(key);
  }

  @Override
  public void set(final String key, final Object value) {
    this.value.put(key, value);
  }

  @Override
  public Class getType(final String key) {
    return this.value.get(key).getClass();
  }

  @Override
  public SQLParamType getType() {
    return this.type;
  }

}

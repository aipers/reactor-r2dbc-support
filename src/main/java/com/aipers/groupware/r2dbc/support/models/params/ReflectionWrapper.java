package com.aipers.groupware.r2dbc.support.models.params;

import com.aipers.groupware.common.utilities.ReflectionUtils;
import com.aipers.groupware.common.utilities.StringUtils;
import com.aipers.groupware.r2dbc.support.converers.SQLTypeConverter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReflectionWrapper<T> {

  private final T pojo;
  private final Class type;

  @Setter
  private SQLTypeConverter converter;

  public ReflectionWrapper(final T object) {
    this.pojo = object;
    this.type = object.getClass();
  }

  public ReflectionWrapper(final T object, final SQLTypeConverter converter) {
    this(object);
    this.converter = converter;
  }

  public String getter(final String key, final Class type) {
    return Boolean.class.isAssignableFrom(type)
        ? this.getChecker(key)
        : this.getGetter(key);
  }

  public String getGetter(final String key) {
    return "get".concat(StringUtils.firstCapitalize(key));
  }

  public String getChecker(final String key) {
    return "is".concat(StringUtils.firstCapitalize(key));
  }

  public String getSetter(final String key) {
    return "set".concat(StringUtils.firstCapitalize(key));
  }

  public boolean isWritableProperty(final String key) {
    return Optional.ofNullable(ReflectionUtils.getField(this.type, key))
        .map(field -> 1 == (field.getModifiers() & Modifier.PUBLIC) ||
            Optional.ofNullable(ReflectionUtils.getMethod(type, getSetter(key), field.getType()))
                .map(method -> 1 == (method.getModifiers() & Modifier.PUBLIC))
                .orElse(false)
        )
        .orElse(false);
  }

  public boolean isReadableProperty(final String key) {
    return Optional.ofNullable(ReflectionUtils.getField(this.type, key))
        .map(field -> 1 == (field.getModifiers() & Modifier.PUBLIC) ||
            Optional.ofNullable(
                ReflectionUtils.getMethod(type, this.getter(key, field.getType()), field.getType())
            )
            .map(method -> 1 == (method.getModifiers() & Modifier.PUBLIC))
            .orElse(false)
        )
        .orElse(false);
  }

  public Class getPropertyType(final String key) {
    final Field field = ReflectionUtils.getField(this.type, key);

    return null == field
        ? ReflectionUtils.getMethod(this.type, this.getGetter(key)).getReturnType()
        : field.getType();
  }

  public Object getPropertyValue(final String key) {
    final Field field = ReflectionUtils.getField(this.type, key);

    try {
      if (1 == (field.getModifiers() & Modifier.PUBLIC)) {
        return field.get(this.pojo);
      }

      final Method method = ReflectionUtils.getMethod(this.type, this.getter(key, field.getType()));
      if (null != method) return method.invoke(this.pojo);
    } catch (IllegalAccessException | InvocationTargetException e) {}

    return null;
  }

  public void setPropertyValue(final String key, Object value) {
    final Field field = ReflectionUtils.getField(this.type, key);

    if (1 == (field.getModifiers() & Modifier.PUBLIC)) {
      try {
        field.set(this.pojo, value);
      }
      catch (ClassCastException c) {
        this.setPropertyUsingSetter(key, value);
      }
      catch (IllegalAccessException e) {}
      return;
    }

    this.setPropertyUsingSetter(key, value);
  }

  private void setPropertyUsingSetter(final String key, final Object value) {
    final Class valueType = value.getClass();
    final Class propertyType = this.getPropertyType(key);

    Optional.ofNullable(
        ReflectionUtils.getMethod(this.type, this.getSetter(key), propertyType)
    ).ifPresent(method -> {
      try {
        method.invoke(
            this.pojo,
            propertyType.isAssignableFrom(valueType)
                ? value
                : Optional.of(converter).map(c -> c.getConvertedValue(propertyType, value)).get()
        );
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {}
    });
  }

}

package com.aipers.groupware.r2dbc.support;

import com.aipers.groupware.common.utilities.StreamUtils;
import com.aipers.groupware.r2dbc.support.converers.SQLTypeConverter;
import com.aipers.groupware.r2dbc.support.models.params.ReflectionWrapper;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import java.lang.reflect.InvocationTargetException;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class SQLResult<R> implements BiFunction<Row, RowMetadata, R> {

  private final Class<R> resultType;
  private final SQLTypeConverter converter;

  @Override
  public R apply(final Row row, final RowMetadata metadata) {
    if (this.isPrimitive()) return row.get(0, resultType);

    try {
      final R bean = resultType.getDeclaredConstructor().newInstance();
      final ReflectionWrapper wrapper = new ReflectionWrapper(bean, converter);

      StreamUtils.iterableAsStream(metadata.getColumnMetadatas()).forEach(meta -> {
        final String property = meta.getName();
        final Class metaType = meta.getJavaType();
        if (wrapper.isWritableProperty(property)) {
          try {
            wrapper.setPropertyValue(property, row.get(property, metaType));
          } catch (IllegalArgumentException e) {
            log.warn(
                "Object type and query type miss match :: [{}@{}] {} <> {}",
                resultType, property, metaType, wrapper.getPropertyType(property)
            );
          }
        }
      });

      return bean;
    } catch (NoSuchMethodException | IllegalAccessException |
             InstantiationException | InvocationTargetException e
    ) {
      log.warn("Bind pojo no such no args constructor", e);
    }

    return null;
  }

  private boolean isPrimitive() {
    return CharSequence.class.isAssignableFrom(resultType)
        || Boolean.class.isAssignableFrom(resultType)
        || Number.class.isAssignableFrom(resultType);
  }

}

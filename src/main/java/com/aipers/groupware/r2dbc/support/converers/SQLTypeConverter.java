package com.aipers.groupware.r2dbc.support.converers;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SQLTypeConverter {

  private final Map<Class, SQLResultTypeConverter> converters = Collections.unmodifiableMap(
      new HashMap<Class, SQLResultTypeConverter>() {
        {
          put(Date.class, new DateConverter());
          put(Enum.class, new EnumConverter());
          put(Double.class, new DoubleConverter());
          put(Float.class, new FloatConverter());
          put(Integer.class, new IntegerConverter());
          put(Short.class, new ShortConverter());
          put(Long.class, new LongConverter());
          put(Character.class, new CharacterConverter());
          put(Byte.class, new ByteConverter());
          put(BigDecimal.class, new BigDecimalConvert());
          put(Void.class, new UnknownConverter());
        }
      }
  );
  private final Map<Class, Class> primitiveTypeMapper = Collections.unmodifiableMap(
      new HashMap<Class, Class>() {
        {
          put(boolean.class, Boolean.class);
          put(char.class, Character.class);
          put(byte.class, Byte.class);
          put(double.class, Double.class);
          put(float.class, Float.class);
          put(int.class, Integer.class);
          put(short.class, Short.class);
          put(long.class, Long.class);
          put(void.class, Void.class);
        }
      }
  );

  public SQLResultTypeConverter getConverter(final Class type, final Object value) {
    log.trace("find type convert :: {} : {} : {}", type, value, converters.get(type));

    return Optional.ofNullable(converters.get(type))
//        .map(converter -> converter.isSupport(value) ? converter : null)
//        .orElse(new UnknownConverter())
        .orElseThrow(SQLTypeConvertException::found)
    ;
  }

  public Object getConvertedValue(final Class type, final Object value) {
    return this.getConverter(
        type.isPrimitive()
            ? this.primitiveTypeMapper.get(type)
            : type
      , value
    ).convert(value);
  }

}

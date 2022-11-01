package com.aipers.groupware.r2dbc.support.converers;

import com.aipers.groupware.common.utilities.DateUtils;
import java.sql.Time;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

public class DateConverter implements SQLResultTypeConverter<Date> {

  @Override
  public boolean isSupport(final Object value) {
    Class type = value.getClass();

    return String.class.isAssignableFrom(type)
        || Long.class.isAssignableFrom(type)
        || Time.class.isAssignableFrom(type) || java.sql.Date.class.isAssignableFrom(type)
        || LocalDate.class.isAssignableFrom(type) || LocalDateTime.class.isAssignableFrom(type)
        || OffsetDateTime.class.isAssignableFrom(type) || ZonedDateTime.class.isAssignableFrom(type);
  }

  @Override
  public Date convert(final Object value) {
    if (Long.class.isAssignableFrom(value.getClass())) return new Date((Long) value);
    if (Time.class.isAssignableFrom(value.getClass())) return new Date(((Time) value).getTime());
    if (java.sql.Date.class.isAssignableFrom(value.getClass())) {
      return new Date(((java.sql.Date) value).getTime());
    }
    if (LocalDate.class.isAssignableFrom(value.getClass())) {
      return new Date(Instant.from(((LocalDate) value)).toEpochMilli());
    }
    if (LocalDateTime.class.isAssignableFrom(value.getClass())) {
      return new Date(Instant.from(((LocalDateTime) value)).toEpochMilli());
    }
    if (OffsetDateTime.class.isAssignableFrom(value.getClass())) {
      return new Date(Instant.from(((OffsetDateTime) value)).toEpochMilli());
    }
    if (ZonedDateTime.class.isAssignableFrom(value.getClass())) {
      return new Date(Instant.from(((ZonedDateTime) value)).toEpochMilli());
    }

    try {
      return DateUtils.stringToDate(String.valueOf(value));
    } catch (ParseException e) {
      return null;
    }
  }

}

package com.aipers.groupware.r2dbc.support.aspects;

import com.aipers.groupware.r2dbc.support.connections.TransactionIsolation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Transacting {

  TransactionIsolation isolation() default TransactionIsolation.READ_COMMITTED;
  boolean readOnly() default false;
  long timeout() default 3000L;

}

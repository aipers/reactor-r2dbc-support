package com.aipers.groupware.r2dbc.support.aspects;

import java.lang.reflect.Method;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class TransactingInvocationInterceptor implements MethodInterceptor {

  @Override
  public Object invoke(final MethodInvocation invocation) throws Throwable {
    final Method method = invocation.getMethod();
    final Transacting transacting = method.getAnnotation(Transacting.class);

    if (null != transacting) {
      final Class returnType = method.getReturnType();

      if (Mono.class == returnType) {
        return TransactingInvocationProcessor.monoProceed((Mono<?>) invocation.proceed(), transacting);
      }
      if (Flux.class == returnType) {
        return TransactingInvocationProcessor.fluxProceed((Flux<?>) invocation.proceed(), transacting);
      }
    }

    return invocation.proceed();
  }

}

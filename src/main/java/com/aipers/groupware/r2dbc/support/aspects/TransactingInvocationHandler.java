package com.aipers.groupware.r2dbc.support.aspects;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class TransactingInvocationHandler<E> implements InvocationHandler {

  private final E proxied;

  @Override
  public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
    final Transacting transacting = method.getAnnotation(Transacting.class);

    if (null != transacting) {
      final Class returnType = method.getReturnType();

      if (Mono.class == returnType) {
        return TransactingInvocationProcessor.monoProceed((Mono<?>) method.invoke(proxied, args), transacting);
      }
      if (Flux.class == returnType) {
        return TransactingInvocationProcessor.fluxProceed((Flux<?>) method.invoke(proxied, args), transacting);
      }
    }


    return method.invoke(proxy, args);
  }

}

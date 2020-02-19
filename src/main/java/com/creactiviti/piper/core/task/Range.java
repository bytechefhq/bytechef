package com.creactiviti.piper.core.task;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;

/**
 * @author Arik Cohen
 * @since Feb, 19 2020
 */
class Range implements MethodExecutor {

  @Override
  public TypedValue execute(EvaluationContext aContext, Object aTarget, Object... aArguments) throws AccessException {
    List<Integer> value = IntStream.rangeClosed((int)aArguments[0], (int)aArguments[1])
        .boxed()
        .collect(Collectors.toList());
    return new TypedValue(value);
  }

}

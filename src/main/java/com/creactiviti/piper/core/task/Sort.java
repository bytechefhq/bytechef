package com.creactiviti.piper.core.task;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;

/**
 * @author Arik Cohen
 * @since Feb, 25 2020
 */
class Sort implements MethodExecutor {
  
  @Override
  public TypedValue execute(EvaluationContext aContext, Object aTarget, Object... aArguments) throws AccessException {
    Collection<?> list = (Collection<?>) aArguments[0];
    List<?> sorted = list.stream()
                         .sorted()
                         .collect(Collectors.toList());
    return new TypedValue(sorted);
  }

}

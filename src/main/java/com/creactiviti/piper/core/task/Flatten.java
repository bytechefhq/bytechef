package com.creactiviti.piper.core.task;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;

/**
 * @author Arik Cohen
 * @since Feb, 19 2020
 */
class Flatten implements MethodExecutor {

  @Override
  public TypedValue execute(EvaluationContext aContext, Object aTarget, Object... aArguments) throws AccessException {
    @SuppressWarnings("unchecked")
    List<List<?>> list = (List<List<?>>) aArguments[0];
    List<?> flat = list.stream()
                       .flatMap(List::stream)
                       .collect(Collectors.toList());
    return new TypedValue(flat);
  }

}

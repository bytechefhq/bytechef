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
class Join implements MethodExecutor {

  @Override
  public TypedValue execute (EvaluationContext aContext, Object aTarget, Object... aArguments) throws AccessException {
    String separator = (String) aArguments[0];
    List<?> values = (List<?>) aArguments[1];
    String str = values.stream()
                       .map(String::valueOf)
                       .collect(Collectors.joining(separator));
    return new TypedValue(str);
  }

}

package com.creactiviti.piper.core.task;

import java.util.ArrayList;
import java.util.List;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;

/**
 * @author Arik Cohen
 * @since Feb, 19 2020
 */
class Concat implements MethodExecutor {

  @Override
  public TypedValue execute (EvaluationContext aContext, Object aTarget, Object... aArguments) throws AccessException {
    List<?> l1 = (List<?>) aArguments[0];
    List<?> l2 = (List<?>) aArguments[1];
    List<Object> joined = new ArrayList<>(l1.size()+l2.size());
    joined.addAll(l1);
    joined.addAll(l2);
    return new TypedValue(joined);
  }

}

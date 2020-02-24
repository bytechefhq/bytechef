package com.creactiviti.piper.core.task;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;

/**
 * @author Arik Cohen
 * @since Feb, 24 2020
 */
class StringFormat implements MethodExecutor {
  
  @Override
  public TypedValue execute(EvaluationContext aContext, Object aTarget, Object... aArguments) throws AccessException {
    Object[] args = new Object[aArguments.length-1];
    System.arraycopy(aArguments, 1, args, 0, aArguments.length-1);
    return new TypedValue(String.format((String)aArguments[0], args));
  }

}

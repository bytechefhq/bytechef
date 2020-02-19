package com.creactiviti.piper.core.task;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;

import com.creactiviti.piper.core.uuid.UUIDGenerator;

/**
 * @author Arik Cohen
 * @since Feb, 19 2020
 */
class Uuid implements MethodExecutor {

  @Override
  public TypedValue execute (EvaluationContext aContext, Object aTarget, Object... aArguments) throws AccessException {
    return new TypedValue(UUIDGenerator.generate());
  }

}

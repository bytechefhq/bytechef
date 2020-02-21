package com.creactiviti.piper.core.task;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;

import com.google.common.io.Files;

/**
 * @author Arik Cohen
 * @since Feb, 21 2020
 */
public class CreateTempDir implements MethodExecutor {

  @Override
  public TypedValue execute (EvaluationContext aContext, Object aTarget, Object... aArguments) throws AccessException {
    return new TypedValue(Files.createTempDir().getPath());
  }

}

package com.creactiviti.piper.core.task;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;

/**
 * @author Arik Cohen
 * @since Mar, 03 2020
 */
class DateFormat implements MethodExecutor {

  @Override
  public TypedValue execute (EvaluationContext aContext, Object aTarget, Object... aArguments) throws AccessException {
    Date date = (Date) aArguments[0];
    SimpleDateFormat sdf = new SimpleDateFormat((String)aArguments[1]);
    return new TypedValue(sdf.format(date));
  }

}

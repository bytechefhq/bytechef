package com.creactiviti.piper.core.task;

import org.springframework.core.env.Environment;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.stereotype.Component;

/**
 * @author Arik Cohen
 * @since Mar, 06 2020
 */
@Component
class Config implements MethodExecutor {

  private final Environment environment;
  
  public Config (Environment aEnvironment) {
    environment = aEnvironment;
  }
  
  @Override
  public TypedValue execute (EvaluationContext aContext, Object aTarget, Object... aArguments) throws AccessException {
    String propertyName = (String) aArguments[0];
    String value = environment.getProperty(propertyName);
    if(value == null) {
      throw new SpelEvaluationException(SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE, propertyName, Environment.class);
    }
    return new TypedValue(value);
  }

}

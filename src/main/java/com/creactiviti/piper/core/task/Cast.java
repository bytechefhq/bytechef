package com.creactiviti.piper.core.task;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;

/**
 * @author Arik Cohen
 * @since Feb, 19 2020
 */
class Cast<T> implements MethodExecutor {
  
  private static final ConversionService conversionService = DefaultConversionService.getSharedInstance();
  
  private final Class<T> type;
  
  Cast(Class<T> aType) {
    type = aType;
  }
  
  @Override
  public TypedValue execute(EvaluationContext aContext, Object aTarget, Object... aArguments) throws AccessException {
    T value = type.cast(conversionService.convert(aArguments[0], type));
    return new TypedValue(value);
  }

}

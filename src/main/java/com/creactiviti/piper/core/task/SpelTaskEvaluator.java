/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.creactiviti.piper.core.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.beanutils.ConvertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.MethodResolver;
import org.springframework.expression.TypedValue;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.creactiviti.piper.core.context.Context;

/**
 * a {@link TaskEvaluator} implemenation which is based on 
 * Spring Expression Language for resolving expressions.
 * 
 * @author Arik Cohen
 * @since Mar 31, 2017
 */
public class SpelTaskEvaluator implements TaskEvaluator {

  private final ExpressionParser parser = new SpelExpressionParser();
  
  private static final String PREFIX = "${";
  private static final String SUFFIX = "}";
  
  private Logger logger = LoggerFactory.getLogger(getClass());
  
  @Override
  public TaskExecution evaluate(TaskExecution aJobTask, Context aContext) {
    Map<String, Object> map = aJobTask.asMap();
    Map<String, Object> newMap = evaluateInternal(map,aContext);
    return SimpleTaskExecution.createFromMap(newMap);
  }

  private Map<String, Object> evaluateInternal(Map<String, Object> aMap, Context aContext) {
    Map<String,Object> newMap = new HashMap<String, Object>();
    for(Entry<String,Object> entry : aMap.entrySet()) {
      newMap.put(entry.getKey(), evaluate(entry.getValue(),aContext));
    }
    return newMap;
  }
  
  private Object evaluate (Object aValue, Context aContext) {
    StandardEvaluationContext context = createEvaluationContext(aContext);
    if(aValue instanceof String) {
      Expression expression = parser.parseExpression((String)aValue,new TemplateParserContext(PREFIX,SUFFIX));
      try {
        return(expression.getValue(context));
      }
      catch (SpelEvaluationException e) {
        logger.debug(e.getMessage());
        return aValue;
      }
    }
    else if (aValue instanceof List) {
      List<Object> evaluatedlist = new ArrayList<>();
      List<Object> list = (List<Object>) aValue;
      for(Object item : list) {
        evaluatedlist.add(evaluate(item, aContext));
      }
      return evaluatedlist;
    }
    else if (aValue instanceof Map) {
      return evaluateInternal((Map<String, Object>) aValue, aContext);
    }
    return aValue;
  }

  private StandardEvaluationContext createEvaluationContext(Context aContext) {
    StandardEvaluationContext context = new StandardEvaluationContext(aContext);
    context.addPropertyAccessor(new MapPropertyAccessor());
    context.addMethodResolver(methodResolver());
    return context;
  }
  
  private MethodResolver methodResolver () {
    return (ctx,target,name,args) -> {
      switch(name) {
        case "systemProperty":
          return this::systemProperty;
        case "range":
          return range();
        case "boolean":
          return cast(Boolean.class);
        case "byte":
          return cast(Byte.class);
        case "char":
          return cast(Character.class);
        case "short":
          return cast(Short.class);
        case "int":
          return cast(Integer.class);
        case "long":
          return cast(Long.class);
        case "float":
          return cast(Float.class);
        case "double":
          return cast(Double.class);
        case "join":
          return join();
        case "concat":
          return concat();
        default:
          return null;
      }
    };
  }
  
  private TypedValue systemProperty (EvaluationContext aContext, Object aTarget, Object... aArgs) throws AccessException {
    return new TypedValue(System.getProperty((String)aArgs[0]));
  }
  
  private MethodExecutor range () {
    return (ctx,target,args) -> {
      List<Integer> value = IntStream.rangeClosed((int)args[0], (int)args[1])
                                     .boxed()
                                     .collect(Collectors.toList());
      return new TypedValue(value);
    };
  }
  
  private <T> MethodExecutor cast(Class<T> type) {
    return (ctx,target,args) -> {
      T value = type.cast(ConvertUtils.convert(args[0], type));
      return new TypedValue(value);
    };
  }

  private <T> MethodExecutor join () {
    return (ctx,target,args) -> {
      String separator = (String) args[0];
      List<T> values = (List<T>) args[1];
      String str = values.stream()
                         .map(String::valueOf)
                         .collect(Collectors.joining(separator));
      return new TypedValue(str);
    };
  }

  private <T> MethodExecutor concat () {
    return (ctx,target,args) -> {
      List<T> l1 = (List<T>) args[0];
      List<T> l2 = (List<T>) args[1];
      List<T> joined = new ArrayList<T>(l1.size()+l2.size());
      joined.addAll(l1);
      joined.addAll(l2);
      return new TypedValue(joined);
    };
  }
}

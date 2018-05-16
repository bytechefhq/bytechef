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

import java.util.Map;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;

/**
 * Simple {@link PropertyAccessor} that can access {@link Map} properties.
 *
 * @author Arik Cohen
 * @since Mar 31, 2017
 */
public class MapPropertyAccessor implements PropertyAccessor {

  @Override
  public Class<?>[] getSpecificTargetClasses() {
    return new Class<?>[]{Map.class};
  }

  @Override
  public boolean canRead(EvaluationContext aContext, Object aTarget, String aName) throws AccessException {
    if(!(aTarget instanceof Map)) {
      return false;
    }
    return ((Map)aTarget).containsKey(aName);
  }

  @Override
  public TypedValue read(EvaluationContext aContext, Object aTarget, String aName) throws AccessException {
    Map<String,Object> map = (Map<String, Object>) aTarget;
    Object value = map.get(aName);
    return new TypedValue(value, TypeDescriptor.forObject(value));
  }

  @Override
  public boolean canWrite(EvaluationContext aContext, Object aTarget, String aName) throws AccessException {
    return false;
  }

  @Override
  public void write(EvaluationContext aContext, Object aTarget, String aName, Object aNewValue) throws AccessException {
    throw new UnsupportedOperationException();
  }

}

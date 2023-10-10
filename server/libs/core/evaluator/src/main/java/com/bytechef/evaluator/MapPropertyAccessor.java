
/*
 * Copyright 2016-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2023 ByteChef Inc.
 */

package com.bytechef.evaluator;

import java.util.Map;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.lang.NonNull;

/**
 * Simple {@link PropertyAccessor} that can access {@link Map} properties.
 *
 * @author Arik Cohen
 * @since Mar 31, 2017
 */
class MapPropertyAccessor implements PropertyAccessor {

    @Override
    public Class<?>[] getSpecificTargetClasses() {
        return new Class<?>[] {
            Map.class
        };
    }

    @Override
    public boolean canRead(EvaluationContext evaluationContext, Object target, String name) throws AccessException {
        if (!(target instanceof Map)) {
            return false;
        }
        return ((Map) target).containsKey(name);
    }

    @Override
    public TypedValue read(EvaluationContext evaluationContext, @NonNull Object target, String name)
        throws AccessException {
        Map<String, Object> map = (Map<String, Object>) target;
        Object value = map.get(name);
        return new TypedValue(value, TypeDescriptor.forObject(value));
    }

    @Override
    public boolean canWrite(EvaluationContext evaluationContext, Object target, String name) throws AccessException {
        return false;
    }

    @Override
    public void write(EvaluationContext evaluationContext, Object target, String name, Object aNewValue)
        throws AccessException {
        throw new UnsupportedOperationException();
    }
}

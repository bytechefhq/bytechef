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
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.evaluator;

import java.util.List;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;

/**
 * @author Arik Cohen
 * @author Monika Kušter
 * @since Feb, 19 2020
 */
class Contains implements MethodExecutor {

    @Override
    public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
        Object firstArgument = arguments[0];

        if (firstArgument == null) {
            return new TypedValue(false);
        } else {
            if (firstArgument instanceof List<?> list) {
                Object value = arguments[1];

                return new TypedValue(list.contains(value));
            } else if (firstArgument instanceof String string) {
                String substring = (String) arguments[1];

                return new TypedValue(string.contains(substring));
            } else {
                throw new IllegalArgumentException(
                    "Invalid arguments for contains. Expected 2 string or list and element.");
            }
        }
    }
}

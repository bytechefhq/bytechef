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
import java.util.stream.Stream;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;

/**
 * @author Arik Cohen
 * @since Feb, 19 2020
 */
class Concat implements MethodExecutor {

    @Override
    public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
        if (arguments[0] instanceof String s1 && arguments[1] instanceof String s2) {
            return new TypedValue(s1.concat(s2));
        } else if (arguments[0] instanceof List<?> l1 && arguments[1] instanceof List<?> l2) {
            return new TypedValue(
                Stream.concat(l1.stream(), l2.stream())
                    .toList());
        } else {
            throw new IllegalArgumentException(
                "Invalid arguments for String.concat. Expected 2 String or 2 List, got " + arguments.length);
        }
    }
}

/*
 * Copyright 2025 ByteChef
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
 */

package com.bytechef.evaluator;

import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;

/**
 * @author Monika Kušter
 */
class EqualsIgnoreCase implements MethodExecutor {

    @Override
    public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
        if (arguments.length != 2) {
            throw new IllegalArgumentException(
                "Invalid number of arguments for equalsIgnoreCase. Expected 2, got " + arguments.length);
        }

        Object arg1 = arguments[0];
        Object arg2 = arguments[1];

        if (arg1 == null && arg2 == null) {
            return new TypedValue(true);
        }

        if (arg1 == null || arg2 == null) {
            return new TypedValue(false);
        }

        if (!(arg1 instanceof String s1) || !(arg2 instanceof String s2)) {
            throw new IllegalArgumentException("Invalid arguments for equalsIgnoreCase.");
        }

        return new TypedValue(s1.equalsIgnoreCase(s2));
    }
}

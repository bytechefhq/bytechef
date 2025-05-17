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
 * @author Ivica Cardic
 */
class LastIndexOf implements MethodExecutor {

    @Override
    public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
        int result;

        if (arguments.length != 2 && arguments.length != 3) {
            throw new IllegalArgumentException(
                "Invalid number of arguments for String.lastIndexOf. Expected 1 or 2, got " + arguments.length);
        }

        String string = (String) arguments[0];

        if (string == null) {
            // String.lastIndexOf(null) or String.lastIndexOf(null, int) would throw NullPointerException.
            throw new IllegalArgumentException("The first argument to lastIndexOf cannot be null.");
        }

        Object firstArgument = arguments[1];
        Class<?> firstArgumentClass = firstArgument.getClass();

        if (arguments.length == 2) {
            // Handles:
            // String.lastIndexOf(String str)
            // String.lastIndexOf(int ch)

            result = switch (firstArgument) {
                case String s -> string.lastIndexOf(s);
                case Integer i -> string.lastIndexOf(i);
                case Character c -> string.lastIndexOf(c);
                default ->
                    throw new AccessException(
                        "Invalid argument type for String.lastIndexOf(arg). Expected String, Integer, or Character. " +
                            "Got: " + firstArgumentClass.getName());
            };
        } else {
            // arguments.length == 3
            // Handles:
            // String.lastIndexOf(String str, int fromIndex)
            // String.lastIndexOf(int ch, int fromIndex)

            Object secondArgument = arguments[2];

            if (secondArgument == null) {
                throw new AccessException("The second argument (fromIndex) to String.lastIndexOf cannot be null.");
            }

            if (!(secondArgument instanceof Integer)) {
                Class<?> secondArgumentClass = secondArgument.getClass();

                throw new AccessException(
                    "The second argument (fromIndex) for String.lastIndexOf must be an Integer. Got: " +
                        secondArgumentClass.getName());
            }

            int fromIndex = (Integer) secondArgument;

            result = switch (firstArgument) {
                case String s -> string.lastIndexOf(s, fromIndex);
                case Integer i -> string.lastIndexOf(i, fromIndex);
                case Character c -> string.lastIndexOf(c);
                default -> throw new AccessException(
                    "Invalid first argument type for String.lastIndexOf(arg, fromIndex). Expected String, Integer, " +
                        "or Character. Got: " + firstArgumentClass.getName());
            };
        }

        return new TypedValue(result);
    }
}

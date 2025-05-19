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
class IndexOf implements MethodExecutor {

    @Override
    public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
        int result;

        if (arguments.length != 2 && arguments.length != 3) {
            throw new IllegalArgumentException(
                "Invalid number of arguments for String.indexOf. Expected 1 or 2, got " + arguments.length);
        }

        String string = (String) arguments[0];

        if (string == null) {
            // String.indexOf(null) or String.indexOf(null, int) would throw NullPointerException.
            throw new IllegalArgumentException("The first argument to indexOf cannot be null.");
        }

        Object firstArgument = arguments[1];
        Class<?> firstArgumentClass = firstArgument.getClass();

        if (arguments.length == 2) {
            // Handles:
            // String.indexOf(String str)
            // String.indexOf(int ch)

            result = switch (firstArgument) {
                case String s -> string.indexOf(s);
                case Integer i -> string.indexOf(i);
                case Character c -> string.indexOf(c);
                default ->
                    throw new AccessException(
                        "Invalid argument type for String.indexOf(arg). Expected String, Integer, or Character. Got: " +
                            firstArgumentClass.getName());
            };
        } else {
            // arguments.length == 3
            // Handles:
            // String.indexOf(String str, int fromIndex)
            // String.indexOf(int ch, int fromIndex)

            Object secondArgument = arguments[2];

            if (secondArgument == null) {
                throw new AccessException("The second argument (fromIndex) to String.indexOf cannot be null.");
            }

            if (!(secondArgument instanceof Integer)) {
                Class<?> secondArgumentClass = secondArgument.getClass();

                throw new AccessException(
                    "The second argument (fromIndex) for String.indexOf must be an Integer. Got: " +
                        secondArgumentClass.getName());
            }

            int fromIndex = (Integer) secondArgument;

            result = switch (firstArgument) {
                case String s -> string.indexOf(s, fromIndex);
                case Integer i -> string.indexOf(i, fromIndex);
                case Character c -> string.indexOf(c);
                default -> throw new AccessException(
                    "Invalid first argument type for String.indexOf(arg, fromIndex). Expected String, Integer, or " +
                        "Character. Got: " + firstArgumentClass.getName());
            };
        }

        return new TypedValue(result);
    }
}

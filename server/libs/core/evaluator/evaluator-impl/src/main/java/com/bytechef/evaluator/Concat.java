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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
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
        if (arguments.length == 0) {
            throw new IllegalArgumentException(
                "Invalid number of arguments for concat. Expected at least 1, got " + arguments.length);
        }

        if (Arrays.stream(arguments)
            .allMatch(argument -> argument instanceof String)) {
            return new TypedValue(
                Arrays.stream(arguments)
                    .map(argument -> (String) argument)
                    .collect(Collectors.joining()));
        }

        if (Arrays.stream(arguments)
            .allMatch(argument -> argument instanceof List<?>)) {
            return new TypedValue(
                Arrays.stream(arguments)
                    .flatMap(argument -> ((List<?>) argument).stream())
                    .toList());
        }

        throw new IllegalArgumentException(
            "Invalid arguments for concat. Expected all arguments to be strings or all to be lists.");
    }
}

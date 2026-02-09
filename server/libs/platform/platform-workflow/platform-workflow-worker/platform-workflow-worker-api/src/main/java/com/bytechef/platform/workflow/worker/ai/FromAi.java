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

package com.bytechef.platform.workflow.worker.ai;

import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.TypedValue;

/**
 * @author Ivica Cardic
 */
public class FromAi implements MethodExecutor {

    @Override
    public TypedValue execute(EvaluationContext context, Object target, Object... arguments) throws AccessException {
        if (arguments.length == 0) {
            throw new IllegalArgumentException("fromAi requires at least a name argument.");
        }

        if (arguments.length > 4) {
            throw new IllegalArgumentException(
                "fromAi accepts at most 4 arguments (name, description, type, default).");
        }

        Object nameArgument = arguments[0];

        if (!(nameArgument instanceof String string) || StringUtils.isBlank(string)) {
            throw new IllegalArgumentException("fromAi name argument must be a non-empty String.");
        }

        String name = string.trim();

        String description = null;

        if (arguments.length > 1) {
            Object descriptionArgument = arguments[1];

            if (descriptionArgument != null && !(descriptionArgument instanceof String)) {
                throw new IllegalArgumentException("fromAi description argument must be a String or null.");
            }

            description = (String) descriptionArgument;
        }

        String type = "STRING";

        if (arguments.length > 2) {
            Object typeArgument = arguments[2];

            if (typeArgument != null && !(typeArgument instanceof String)) {
                throw new IllegalArgumentException("fromAi type argument must be a String or null.");
            }

            String typeCandidate = (String) typeArgument;

            if (typeCandidate != null && !typeCandidate.trim()
                .isEmpty()) {
                type = typeCandidate;
            }
        }

        Object defaultValue = arguments.length > 3 ? arguments[3] : null;

        return new TypedValue(new FromAiResult(name, description, type, defaultValue));
    }

}

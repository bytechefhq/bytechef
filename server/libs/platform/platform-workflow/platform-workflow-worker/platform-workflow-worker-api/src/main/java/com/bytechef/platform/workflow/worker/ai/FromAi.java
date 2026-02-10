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

        String name = (String) arguments[0];
        String description = arguments.length > 1 ? (String) arguments[1] : null;
        String type = arguments.length > 2 ? (String) arguments[2] : "STRING";
        Object defaultValue = arguments.length > 3 ? arguments[3] : null;

        return new TypedValue(new FromAiResult(name, description, type, defaultValue));
    }

}

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

import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.TypedValue;

/**
 * @author Ivica Cardic
 */
final class ContextPaths {

    private ContextPaths() {
    }

    static @Nullable Object get(EvaluationContext context, String path) {
        TypedValue rootObject = context.getRootObject();

        Object value = rootObject.getValue();

        for (String segment : path.split("\\.", -1)) {
            if (!(value instanceof Map<?, ?> map) || !map.containsKey(segment)) {
                return null;
            }

            value = map.get(segment);
        }

        return value;
    }
}

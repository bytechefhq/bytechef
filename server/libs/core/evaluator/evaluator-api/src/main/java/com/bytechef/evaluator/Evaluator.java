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

/**
 * Strategy interface for evaluating a map.
 *
 * @author Ivica Cardic
 */
public interface Evaluator {

    /**
     * Evaluate the {@link java.util.Map} against the provided {@link java.util.Map}.}
     *
     * @param map     The {@link java.util.Map} instance to evaluate
     * @param context The context to evaluate the task against
     * @return the evaluate {@link java.util.Map}.
     */
    Map<String, Object> evaluate(Map<String, ?> map, Map<String, ?> context);
}

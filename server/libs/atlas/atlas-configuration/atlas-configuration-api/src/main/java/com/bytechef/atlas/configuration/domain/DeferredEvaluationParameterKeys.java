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

package com.bytechef.atlas.configuration.domain;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for parameter keys that should not be eagerly evaluated when a task execution is evaluated. Task dispatchers
 * with conditional branches (e.g., condition, branch) register their sub-task parameter keys here so that sub-task
 * definitions retain their original expressions until the selected branch is dispatched.
 *
 * <p>
 * Without deferred evaluation, the evaluator resolves expressions in ALL branches before the condition is checked. This
 * can corrupt sub-task definitions with wrong values when expressions partially resolve against the current context.
 *
 * @author Ivica Cardic
 */
public final class DeferredEvaluationParameterKeys {

    private static final Map<String, Set<String>> parameterKeysByTaskTypePrefix = new ConcurrentHashMap<>();

    private DeferredEvaluationParameterKeys() {
    }

    /**
     * Returns the set of parameter keys that should be deferred from evaluation for the given task type.
     *
     * @param taskType the task type (e.g., "condition/v1")
     * @return the set of parameter keys to defer, or an empty set if none
     */
    public static Set<String> forTaskType(String taskType) {
        if (taskType == null) {
            return Set.of();
        }

        for (Map.Entry<String, Set<String>> entry : parameterKeysByTaskTypePrefix.entrySet()) {
            if (taskType.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }

        return Set.of();
    }

    /**
     * Registers parameter keys that should be deferred from evaluation for a task type prefix.
     *
     * @param taskTypePrefix the task type prefix (e.g., "condition/")
     * @param parameterKeys  the parameter keys to defer (e.g., "caseTrue", "caseFalse")
     */
    public static void register(String taskTypePrefix, String... parameterKeys) {
        parameterKeysByTaskTypePrefix.put(taskTypePrefix, Set.of(parameterKeys));
    }
}

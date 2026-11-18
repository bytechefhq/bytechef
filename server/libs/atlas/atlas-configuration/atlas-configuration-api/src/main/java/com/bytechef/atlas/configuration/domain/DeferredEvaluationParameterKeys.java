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
 * <p>
 * A second, unrelated case is a parameter whose value is not an expression at all but merely looks like one - the
 * {@code commands} component's command lines, where POSIX parameter expansion ({@code ${VAR}}, {@code ${VAR:-default}},
 * {@code ${#a[@]}}) is character-for-character the evaluator's own accessor syntax. Left evaluated, a line as
 * unremarkable as {@code echo ${GREETING:-hi}} fails the task with {@code Invalid expression}, naming neither the
 * component nor the property, because a shell default is not a valid accessor.
 *
 * <p>
 * The bare form {@code echo ${HOME}} is worse than that rather than better: it <em>is</em> a valid accessor, so it
 * passes validation and is evaluated against the workflow context. It survives untouched only for as long as that
 * context happens to carry no {@code HOME} key - the day one does, the line is silently rewritten before the shell ever
 * sees it.
 *
 * <p>
 * Component prefixes are seeded by this class itself, as plain string literals, rather than registered from the
 * component module that owns them. A task dispatcher can register from its own module because every JVM that evaluates
 * a task also carries every dispatcher module - {@code JobExecutor} calls {@code TaskExecution.evaluate} in the
 * coordinator, and the coordinator depends on all of {@code server:libs:modules:task-dispatchers}. A component module
 * has no such guarantee: the coordinator depends on no component module at all, so a registration living in a component
 * module would never run there, and a distributed deployment would fail in the coordinator with
 * {@code Invalid expression} before the worker was ever reached - while the monolith and the worker both worked, which
 * is what makes that failure mode hard to see. Seeding here is correct by construction, because every JVM that
 * evaluates a task necessarily carries this module. Literals are used because atlas must not depend on a component
 * module to name them.
 *
 * @author Ivica Cardic
 */
public final class DeferredEvaluationParameterKeys {

    private static final Map<String, Set<String>> parameterKeysByTaskTypePrefix = new ConcurrentHashMap<>();

    static {
        register("commands/", "commands");
    }

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

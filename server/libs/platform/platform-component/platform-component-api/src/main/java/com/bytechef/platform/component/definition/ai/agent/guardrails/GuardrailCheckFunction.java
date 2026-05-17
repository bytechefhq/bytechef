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

package com.bytechef.platform.component.definition.ai.agent.guardrails;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import java.util.List;
import java.util.Optional;

/**
 * Functional contract implemented by each guardrail used under a {@code CheckForViolations} cluster.
 *
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface GuardrailCheckFunction extends StagedGuardrail {

    ClusterElementType CHECK_FOR_VIOLATIONS =
        new ClusterElementType("CHECK_FOR_VIOLATIONS", "checkForViolations", "Check for Violations", true, false);

    /**
     * Evaluate the text and return a violation if the check fires.
     *
     * @param text    the text to evaluate
     * @param context the call-site context
     * @return a Violation if the guardrail fires; {@link Optional#empty()} otherwise
     */
    Optional<Violation> apply(String text, GuardrailContext context) throws Exception;

    /**
     * Multi-violation variant of {@link #apply}. Default wraps the single result. Implementations that aggregate
     * multiple independent classifiers in a single call must override this to emit one {@link Violation} per fired
     * entry.
     *
     * @param text    the text to evaluate
     * @param context the call-site context
     * @return zero or more violations; never {@code null}
     */
    default List<Violation> applyAll(String text, GuardrailContext context) throws Exception {
        return apply(text, context)
            .map(List::of)
            .orElse(List.of());
    }
}

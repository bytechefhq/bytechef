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
import java.util.Optional;

/**
 * Functional contract implemented by each guardrail type used under a {@code CheckForViolations} cluster. Inherits
 * {@link StagedGuardrail#stage()} so the two-stage pipeline contract has a single source of truth.
 *
 * <p>
 * {@link #stage()} controls when the check runs in {@code CheckForViolationsAdvisor}:
 * <ul>
 * <li>{@link GuardrailStage#PREFLIGHT} — rule-based checks (regex/keyword/URL/secret) run first; they MAY also mask by
 * additionally implementing {@link PreflightMasking} (typically via {@link PreflightCheckFunction}).</li>
 * <li>{@link GuardrailStage#LLM} — LLM-classifier checks run after preflight. They do not implement
 * {@link PreflightMasking} — masking is not part of their contract.</li>
 * </ul>
 *
 * @author Ivica Cardic
 */
public interface GuardrailCheckFunction extends StagedGuardrail {

    ClusterElementType CHECK_FOR_VIOLATIONS =
        new ClusterElementType("CHECK_FOR_VIOLATIONS", "checkForViolations", "Check for Violations", true, false);

    /**
     * Evaluate the text and return a violation if the check fires.
     *
     * @param text    the user-supplied text to evaluate (already preflight-masked when {@link #stage()} is
     *                {@link GuardrailStage#LLM})
     * @param context the call-site context (input/connection/parent parameters, extensions, connections, chat client)
     * @return a Violation if the guardrail fires; {@link Optional#empty()} otherwise
     * @throws Exception on unrecoverable errors. Exception handling in {@code CheckForViolationsAdvisor} depends on the
     *                   per-check {@code failMode} parameter and the exception type:
     *                   <ul>
     *                   <li><b>FAIL_CLOSED</b> (default) — any exception becomes an
     *                   {@link Violation#ofExecutionFailure(String, Throwable) execution-failure} violation that blocks
     *                   the request.</li>
     *                   <li><b>FAIL_OPEN</b> — transient exceptions are logged, the check is skipped, and the failure
     *                   is surfaced on {@code guardrail.skippedFailures} response metadata without blocking.</li>
     *                   <li><b>Configuration errors</b> (invalid regex, missing MODEL child, NPE/CCE bugs, etc.) —
     *                   always fail closed regardless of {@code failMode}. See {@code CheckForViolationsAdvisor} class
     *                   Javadoc for the full classification.</li>
     *                   </ul>
     *                   Implementations should therefore prefer to surface configuration errors as
     *                   {@link IllegalArgumentException} / {@link IllegalStateException} so they are correctly
     *                   classified as operator-bug failures rather than transient outages.
     */
    Optional<Violation> apply(String text, GuardrailContext context) throws Exception;
}

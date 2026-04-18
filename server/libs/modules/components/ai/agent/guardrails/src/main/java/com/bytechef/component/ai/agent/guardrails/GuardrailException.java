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

package com.bytechef.component.ai.agent.guardrails;

/**
 * Sealed base class for every guardrail-execution exception. The class hierarchy is flat — every variant is a direct
 * permit — so Java {@code switch} pattern-matching on {@code GuardrailException} is exhaustive over the four real
 * failure kinds:
 * <ul>
 * <li>{@link GuardrailUnavailableException} ({@link GuardrailExceptionKind#UPSTREAM_UNAVAILABLE}) — upstream dependency
 * (LLM, API) returned an error or was unreachable. {@code non-sealed} so consumers may add their own subclasses for
 * finer-grained upstream failure types.</li>
 * <li>{@link GuardrailOutputParseException} ({@link GuardrailExceptionKind#OUTPUT_PARSE}) — LLM returned a response the
 * structured-output converter could not parse. Distinct from UPSTREAM_UNAVAILABLE because the root cause is typically a
 * prompt/schema bug on our side, not an upstream outage.</li>
 * <li>{@link MissingModelChildException} ({@link GuardrailExceptionKind#MISSING_MODEL}) — misconfiguration: LLM
 * guardrail with no MODEL child.</li>
 * <li>{@link SanitizerExecutionFailureException} ({@link GuardrailExceptionKind#SANITIZER_FAILED}) — aggregate
 * sanitizer failure across a single pass.</li>
 * </ul>
 *
 * <p>
 * {@link #kind()} is retained as a stable external tag: the enum name survives class-hierarchy refactors (e.g. if a
 * variant is later renamed), so external consumers serialising {@code kind().name()} stay readable across versions.
 * Internal code should prefer exhaustive {@code switch} over the sealed hierarchy.
 *
 * @author Ivica Cardic
 */
public abstract sealed class GuardrailException extends RuntimeException permits
    GuardrailUnavailableException, GuardrailOutputParseException, MissingModelChildException,
    SanitizerExecutionFailureException {

    protected GuardrailException(String message) {
        super(message);
    }

    protected GuardrailException(String message, Throwable cause) {
        super(message, cause);
    }

    /** Stable tag for the failure variant, safe for external serialization. */
    public abstract GuardrailExceptionKind kind();

    /**
     * Name of the guardrail that raised the failure. {@code null} when the failure aggregates multiple guardrails (see
     * {@link SanitizerExecutionFailureException}).
     */
    public abstract String guardrailName();
}

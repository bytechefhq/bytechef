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
 * Stable, enumerable tag for guardrail failure modes. Preferred over {@code instanceof} in downstream tooling because
 * the enum survives serialization, is comparable cheaply, and future refactors that change the exception class
 * hierarchy do not break external consumers pattern-matching on the tag.
 *
 * <p>
 * Every non-{@link #CONFIGURATION} value corresponds 1:1 to a permit of {@link GuardrailException}; see that class for
 * the authoritative mapping. {@link #CONFIGURATION} is intentionally different: it is <b>synthesised</b> by
 * {@code CheckForViolationsAdvisor.resolveFailureKind} for non-{@code GuardrailException} causes that the advisor's
 * {@code isConfigurationError} classifier treats as operator-actionable (malformed regex,
 * {@code PatternSyntaxException}, {@code IllegalArgumentException}, {@code IllegalStateException},
 * {@code NullPointerException}, {@code ClassCastException}, {@code RegexExecutionLimitException}). There is
 * deliberately <em>no</em> {@code ConfigurationException} permit on {@link GuardrailException} — these causes originate
 * in detector code or the JDK regex engine, not in guardrail code, so wrapping them would lose the original type
 * information that operators need in stack traces.
 *
 * @author Ivica Cardic
 */
public enum GuardrailExceptionKind {

    /** LLM returned a response the structured-output converter could not parse. */
    OUTPUT_PARSE,

    /** Upstream dependency (LLM, API) returned an error or was unreachable. */
    UPSTREAM_UNAVAILABLE,

    /** LLM-based guardrail was configured without a MODEL child cluster element. */
    MISSING_MODEL,

    /** One or more sanitizers threw during a sanitize pass. */
    SANITIZER_FAILED,

    /**
     * Synthesised tag (not a {@link GuardrailException} permit) for operator-actionable failures originating outside
     * guardrail code — malformed regex, pathological ReDoS pattern, invalid parameter, programming bug escaping the
     * detector. Distinct from {@link #UPSTREAM_UNAVAILABLE} because retries will not recover: an operator must fix the
     * configuration. Alerting pipelines should page on this kind rather than treat it as a transient outage.
     */
    CONFIGURATION
}

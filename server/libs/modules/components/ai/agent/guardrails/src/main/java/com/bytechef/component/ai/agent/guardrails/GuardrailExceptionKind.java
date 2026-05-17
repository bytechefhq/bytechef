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
 * Stable tag for guardrail failure modes. Each non-{@link #CONFIGURATION} value maps 1:1 to a
 * {@link GuardrailException} subtype; {@link #CONFIGURATION} is synthesised for operator-actionable causes that
 * originate outside guardrail code.
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

    /** Operator-actionable configuration failure. */
    CONFIGURATION
}

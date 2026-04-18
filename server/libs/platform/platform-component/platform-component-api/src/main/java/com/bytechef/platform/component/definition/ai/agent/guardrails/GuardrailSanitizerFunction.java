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

/**
 * Functional contract implemented by each guardrail type used under a SanitizeText tool. Inherits
 * {@link StagedGuardrail#stage()} so the two-stage pipeline contract has a single source of truth across both check and
 * sanitizer flavours.
 *
 * <p>
 * {@link #stage()} controls when the sanitizer runs in {@code SanitizeTextAdvisor}:
 * <ul>
 * <li>{@link GuardrailStage#PREFLIGHT} — rule-based sanitizers (Pii, SecretKeys, Urls, Keywords, CustomRegex) run first
 * and mask their detected entities — typically via {@link PreflightSanitizerFunction}.</li>
 * <li>{@link GuardrailStage#LLM} — LLM-based sanitizers ({@code LlmPii.ofSanitize}) run after preflight and see
 * already-masked text. LLM-stage sanitizers do not implement {@link PreflightMasking}.</li>
 * </ul>
 *
 * @author Ivica Cardic
 */
public interface GuardrailSanitizerFunction extends StagedGuardrail {

    ClusterElementType SANITIZE_TEXT =
        new ClusterElementType("SANITIZE_TEXT", "sanitizeText", "Sanitize Text", true, false);

    /**
     * @param text    the text to sanitize (already preflight-masked when {@link #stage()} is
     *                {@link GuardrailStage#LLM})
     * @param context the call-site context (input/connection parameters, extensions, connections, chat client);
     *                parentParameters is unused for sanitizers
     * @return the sanitized text (possibly equal to {@code text})
     * @throws Exception on unrecoverable errors. Exception handling in {@code SanitizeTextAdvisor} depends on the
     *                   per-sanitizer {@code failMode} parameter:
     *                   <ul>
     *                   <li><b>FAIL_OPEN</b> — the sanitizer is skipped; the last-good intermediate text propagates to
     *                   the next sanitizer and ultimately to the caller. Partial masking is preserved.</li>
     *                   <li><b>FAIL_CLOSED</b> (default) — the failure is collected and at end-of-pass the advisor
     *                   throws a {@code SanitizerExecutionFailureException}. The outer {@code adviseCall} /
     *                   {@code adviseStream} methods replace the response with a withheld-placeholder rather than risk
     *                   leaking unredacted model text.</li>
     *                   </ul>
     *                   Streaming adds an additional caveat: a failure mid-stream can withhold the remainder but cannot
     *                   recall already-delivered chunks — see {@code SanitizeTextAdvisor#adviseStream} Javadoc.
     */
    String apply(String text, GuardrailContext context) throws Exception;
}

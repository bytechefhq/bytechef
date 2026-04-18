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

/**
 * Shared stage contract between {@link GuardrailCheckFunction} and {@link GuardrailSanitizerFunction}. Carries only the
 * stage ordering — masking lives on {@link PreflightMasking} so LLM-stage guardrails cannot declare masking methods the
 * advisor would silently ignore.
 *
 * @author Ivica Cardic
 */
public interface StagedGuardrail {

    /**
     * Pipeline stage for this guardrail. Default is {@link GuardrailStage#PREFLIGHT} — rule-based guardrails run first
     * and may mask their entities (via {@link PreflightMasking}) so LLM-stage guardrails see already-masked text.
     * LLM-based guardrails (Jailbreak, Nsfw, TopicalAlignment, Custom, LlmPii) override to {@link GuardrailStage#LLM}.
     */
    default GuardrailStage stage() {
        return GuardrailStage.PREFLIGHT;
    }
}

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
 * Shared stage contract between {@link GuardrailCheckFunction} and {@link GuardrailSanitizerFunction}.
 *
 * @author Ivica Cardic
 */
public interface StagedGuardrail {

    /**
     * Pipeline stage for this guardrail. Defaults to {@link GuardrailStage#PREFLIGHT}; LLM-based guardrails override to
     * {@link GuardrailStage#LLM}.
     */
    default GuardrailStage stage() {
        return GuardrailStage.PREFLIGHT;
    }

    /**
     * Whether this guardrail needs a shared {@link org.springframework.ai.chat.client.ChatClient} from a sibling MODEL
     * child. Defaults to {@code false}; LLM-stage guardrails that call the chat client must override to {@code true}.
     */
    default boolean requiresChatClient() {
        return false;
    }
}

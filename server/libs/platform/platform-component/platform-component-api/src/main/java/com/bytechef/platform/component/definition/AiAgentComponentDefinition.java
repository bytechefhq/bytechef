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

package com.bytechef.platform.component.definition;

import static com.bytechef.component.definition.ai.agent.BaseToolFunction.TOOLS;
import static com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction.CHAT_MEMORY;
import static com.bytechef.platform.component.definition.ai.agent.GuardrailsFunction.GUARDRAILS;
import static com.bytechef.platform.component.definition.ai.agent.ModelFunction.MODEL;
import static com.bytechef.platform.component.definition.ai.agent.RagFunction.RAG;
import static com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailCheckFunction.CHECK_FOR_VIOLATIONS;
import static com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailKeys.CUSTOM;
import static com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailKeys.JAILBREAK;
import static com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailKeys.NSFW;
import static com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailKeys.TOPICAL_ALIGNMENT;
import static com.bytechef.platform.component.definition.ai.agent.guardrails.GuardrailSanitizerFunction.SANITIZE_TEXT;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public interface AiAgentComponentDefinition extends ClusterRootComponentDefinition {

    @Override
    default List<ClusterElementType> getClusterElementTypes() {
        return List.of(MODEL, CHAT_MEMORY, RAG, GUARDRAILS, TOOLS);
    }

    @Override
    default Map<String, List<String>> getClusterElementClusterElementTypes() {
        return Map.of(
            CHECK_FOR_VIOLATIONS.key(), List.of(CHECK_FOR_VIOLATIONS.key()),
            SANITIZE_TEXT.key(), List.of(SANITIZE_TEXT.key()),
            JAILBREAK, List.of(MODEL.key()),
            NSFW, List.of(MODEL.key()),
            TOPICAL_ALIGNMENT, List.of(MODEL.key()),
            CUSTOM, List.of(MODEL.key()));
    }
}

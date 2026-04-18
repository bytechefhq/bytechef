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
import com.bytechef.platform.component.definition.ClusterRootComponentDefinition;
import java.util.List;

/**
 * Common interface for guardrail component definitions. A guardrail is either LLM-backed (requires a {@code MODEL}
 * child cluster element, signalled by {@link #requiresModel()} returning {@code true}) or rule-based (no child cluster
 * elements). Guardrails declare no child cluster types of their own — the parent {@code CheckForViolations} or
 * {@code SanitizeText} cluster owns any shared {@code MODEL} child and injects the resolved
 * {@link org.springframework.ai.chat.client.ChatClient} into every child via {@code GuardrailContext.chatClient()}.
 *
 * @author Ivica Cardic
 */
public interface GuardrailComponentDefinition extends ClusterRootComponentDefinition {

    /**
     * Guardrails have no child cluster elements of their own; exposing an empty list lets tooling iterate without a
     * special case. Implementations must not override this method to return a non-empty list — doing so silently allows
     * arbitrary cluster elements to appear under a guardrail leaf.
     */
    @Override
    default List<ClusterElementType> getClusterElementTypes() {
        return List.of();
    }

    /**
     * Whether this guardrail needs its parent cluster root to wire a {@code MODEL} child and inject a
     * {@link org.springframework.ai.chat.client.ChatClient} at runtime. Tooling should enable model-configuration UI
     * and runtime checks only when at least one child returns {@code true}.
     */
    boolean requiresModel();
}

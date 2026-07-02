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

package com.bytechef.component.ai.llm.router.litellm;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.llm.router.litellm.action.LiteLLMChatAction;
import com.bytechef.component.ai.llm.router.litellm.cluster.LiteLLMChatModel;
import com.bytechef.component.ai.llm.router.litellm.cluster.LiteLLMEmbedding;
import com.bytechef.component.ai.llm.router.litellm.connection.LiteLLMConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Aarish Yadav
 */
@AutoService(ComponentHandler.class)
public class LiteLLMComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("liteLlm")
        .title("LiteLLM")
        .description(
            "LiteLLM is a self-hosted AI gateway proxy that provides a unified OpenAI-compatible API for " +
                "100+ LLM providers, enabling model fallbacks, load balancing, and spend tracking through a " +
                "single endpoint.")
        .icon("path:assets/litellm.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(LiteLLMConnection.CONNECTION_DEFINITION)
        .actions(
            LiteLLMChatAction.ACTION_DEFINITION)
        .clusterElements(
            LiteLLMChatModel.CLUSTER_ELEMENT_DEFINITION,
            LiteLLMEmbedding.CLUSTER_ELEMENT_DEFINITION)
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

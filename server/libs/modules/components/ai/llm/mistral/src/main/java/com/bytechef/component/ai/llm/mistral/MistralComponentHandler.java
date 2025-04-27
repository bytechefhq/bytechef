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

package com.bytechef.component.ai.llm.mistral;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.llm.mistral.action.MistralChatAction;
import com.bytechef.component.ai.llm.mistral.action.MistralOcrAction;
import com.bytechef.component.ai.llm.mistral.cluster.MistralAiChatModel;
import com.bytechef.component.ai.llm.mistral.cluster.MistralAiEmbedding;
import com.bytechef.component.ai.llm.mistral.connection.MistralConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 * @author Marko Kriskovic
 */
@AutoService(ComponentHandler.class)
public class MistralComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("mistral")
        .title("MistralAI")
        .description("Open, efficient, helpful and trustworthy AI models through ground-breaking innovations.")
        .icon("path:assets/mistral.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(MistralConnection.CONNECTION_DEFINITION)
        .actions(
            MistralChatAction.ACTION_DEFINITION,
            MistralOcrAction.ACTION_DEFINITION)
        .clusterElements(
            MistralAiEmbedding.CLUSTER_ELEMENT_DEFINITION,
            MistralAiChatModel.CLUSTER_ELEMENT_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

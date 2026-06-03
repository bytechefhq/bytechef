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

package com.bytechef.component.ai.llm.gemini;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.llm.gemini.action.GeminiChatAction;
import com.bytechef.component.ai.llm.gemini.cluster.GeminiChatModel;
import com.bytechef.component.ai.llm.gemini.connection.GeminiConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 * @author Marko Kriskovic
 */
@AutoService(ComponentHandler.class)
public class GeminiComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("gemini")
        .title("Gemini")
        .version(1)
        .description(
            "Google Gemini is a multimodal generative AI model. This component supports both Vertex AI and the " +
                "Gemini Developer API.")
        .icon("path:assets/google-vertex.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(GeminiConnection.CONNECTION_DEFINITION)
        .actions(GeminiChatAction.ACTION_DEFINITION)
        .clusterElements(GeminiChatModel.CLUSTER_ELEMENT_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

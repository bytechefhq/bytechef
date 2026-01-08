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

package com.bytechef.component.ai.llm.openai;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.llm.openai.action.OpenAiChatAction;
import com.bytechef.component.ai.llm.openai.action.OpenAiCreateImageAction;
import com.bytechef.component.ai.llm.openai.action.OpenAiCreateSpeechAction;
import com.bytechef.component.ai.llm.openai.action.OpenAiCreateTranscriptionAction;
import com.bytechef.component.ai.llm.openai.action.OpenAiStreamChatAction;
import com.bytechef.component.ai.llm.openai.cluster.OpenAiChatModel;
import com.bytechef.component.ai.llm.openai.cluster.OpenAiEmbedding;
import com.bytechef.component.ai.llm.openai.connection.OpenAiConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 * @author Marko Kriskovic
 */
@AutoService(ComponentHandler.class)
public class OpenAiComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("openAi")
        .title("OpenAI")
        .description(
            "OpenAI is a research organization that aims to develop and direct artificial intelligence (AI) in ways " +
                "that benefit humanity as a whole.")
        .icon("path:assets/open-ai.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(OpenAiConnection.CONNECTION_DEFINITION)
        .actions(
            OpenAiChatAction.ACTION_DEFINITION,
            OpenAiStreamChatAction.ACTION_DEFINITION,
            OpenAiCreateImageAction.ACTION_DEFINITION,
            OpenAiCreateSpeechAction.ACTION_DEFINITION,
            OpenAiCreateTranscriptionAction.ACTION_DEFINITION)
        .clusterElements(
            OpenAiEmbedding.CLUSTER_ELEMENT_DEFINITION,
            OpenAiChatModel.CLUSTER_ELEMENT_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

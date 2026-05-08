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

package com.bytechef.component.ai.llm.nano.gpt;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.llm.nano.gpt.action.NanoGptChatAction;
import com.bytechef.component.ai.llm.nano.gpt.action.NanoGptCreateImageAction;
import com.bytechef.component.ai.llm.nano.gpt.action.NanoGptCreateSpeechAction;
import com.bytechef.component.ai.llm.nano.gpt.action.NanoGptCreateTranscriptionAction;
import com.bytechef.component.ai.llm.nano.gpt.cluster.NanoGptChatModel;
import com.bytechef.component.ai.llm.nano.gpt.cluster.NanoGptEmbedding;
import com.bytechef.component.ai.llm.nano.gpt.connection.NanoGptConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Marko Kriskovic
 */
@AutoService(ComponentHandler.class)
public class NanoGptComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("nanoGpt")
        .title("Nano GPT")
        .description("The NanoGPT API allows you to generate text, images and video using any AI model available.")
        .icon("path:assets/nano-gpt.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(NanoGptConnection.CONNECTION_DEFINITION)
        .actions(NanoGptChatAction.ACTION_DEFINITION,
            NanoGptCreateImageAction.ACTION_DEFINITION,
            NanoGptCreateSpeechAction.ACTION_DEFINITION,
            NanoGptCreateTranscriptionAction.ACTION_DEFINITION)
        .clusterElements(NanoGptChatModel.CLUSTER_ELEMENT_DEFINITION,
            NanoGptEmbedding.CLUSTER_ELEMENT_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

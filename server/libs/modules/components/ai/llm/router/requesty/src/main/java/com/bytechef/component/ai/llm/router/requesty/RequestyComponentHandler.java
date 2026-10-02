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

package com.bytechef.component.ai.llm.router.requesty;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.llm.router.requesty.action.RequestyChatAction;
import com.bytechef.component.ai.llm.router.requesty.action.RequestyCreateImageAction;
import com.bytechef.component.ai.llm.router.requesty.action.RequestyCreateSpeechAction;
import com.bytechef.component.ai.llm.router.requesty.action.RequestyCreateTranscriptionAction;
import com.bytechef.component.ai.llm.router.requesty.cluster.RequestyChatModel;
import com.bytechef.component.ai.llm.router.requesty.cluster.RequestyEmbedding;
import com.bytechef.component.ai.llm.router.requesty.connection.RequestyConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Thibault Jaigu
 */
@AutoService(ComponentHandler.class)
public class RequestyComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("requesty")
        .title("Requesty")
        .description(
            "Requesty is an LLM router that gives you access to hundreds of AI models from OpenAI, Anthropic, " +
                "Google and other providers through a single OpenAI compatible API, with routing, caching, " +
                "guardrails and spend controls built in.")
        .icon("path:assets/requesty.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(RequestyConnection.CONNECTION_DEFINITION)
        .actions(
            RequestyChatAction.ACTION_DEFINITION,
            RequestyCreateImageAction.ACTION_DEFINITION,
            RequestyCreateSpeechAction.ACTION_DEFINITION,
            RequestyCreateTranscriptionAction.ACTION_DEFINITION)
        .clusterElements(
            RequestyChatModel.CLUSTER_ELEMENT_DEFINITION,
            RequestyEmbedding.CLUSTER_ELEMENT_DEFINITION)
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

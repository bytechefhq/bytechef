/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.openai;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.openai.constant.OpenAIConstants.OPENAI;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.openai.action.OpenAIChatAction;
import com.bytechef.component.openai.action.OpenAICreateImageAction;
import com.bytechef.component.openai.action.OpenAICreateSpeechAction;
import com.bytechef.component.openai.action.OpenAICreateTranscriptionAction;
import com.bytechef.component.openai.connection.OpenAIConnection;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 * @author Marko Kriskovic
 */
@AutoService(ComponentHandler.class)
public class OpenAIComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(OPENAI)
        .title("OpenAI")
        .description(
            "OpenAI is a research organization that aims to develop and direct artificial intelligence (AI) in ways " +
                "that benefit humanity as a whole.")
        .icon("path:assets/openai.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(OpenAIConnection.CONNECTION_DEFINITION)
        .actions(
            OpenAIChatAction.ACTION_DEFINITION,
            OpenAICreateImageAction.ACTION_DEFINITION,
            OpenAICreateSpeechAction.ACTION_DEFINITION,
            OpenAICreateTranscriptionAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

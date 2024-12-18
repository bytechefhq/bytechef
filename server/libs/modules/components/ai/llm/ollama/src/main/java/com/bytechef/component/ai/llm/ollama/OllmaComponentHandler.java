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

package com.bytechef.component.ai.llm.ollama;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.llm.ollama.action.OllamaChatAction;
import com.bytechef.component.ai.llm.ollama.connection.OllamaConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 * @author Marko Kriskovic
 */
@AutoService(ComponentHandler.class)
public class OllmaComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("ollama")
        .title("Ollama")
        .description(
            "Get up and running with large language models.")
        .icon("path:assets/ollama.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(OllamaConnection.CONNECTION_DEFINITION)
        .actions(OllamaChatAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

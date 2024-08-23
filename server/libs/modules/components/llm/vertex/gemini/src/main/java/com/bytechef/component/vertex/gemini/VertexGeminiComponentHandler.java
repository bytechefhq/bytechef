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

package com.bytechef.component.vertex.gemini;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.vertex.gemini.constant.VertexGeminiConstants.VERTEX_GEMINI;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.vertex.gemini.action.VertexGeminiChatAction;
import com.bytechef.component.vertex.gemini.connection.VertexGeminiConnection;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class VertexGeminiComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(VERTEX_GEMINI)
        .title("Vertex AI Gemini")
        .description(
            "Vertex AI is a fully-managed, unified AI development platform for building and using generative AI. ")
        .icon("path:assets/google-vertex.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(VertexGeminiConnection.CONNECTION_DEFINITION)
        .actions(VertexGeminiChatAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

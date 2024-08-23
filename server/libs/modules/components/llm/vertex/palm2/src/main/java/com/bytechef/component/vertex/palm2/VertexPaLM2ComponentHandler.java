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

package com.bytechef.component.vertex.palm2;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.vertex.palm2.constant.VertexPaLM2Constants.VERTEX_PALM2;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.vertex.palm2.action.VertexPaLM2ChatAction;
import com.bytechef.component.vertex.palm2.connection.VertexPaLM2Connection;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class VertexPaLM2ComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(VERTEX_PALM2)
        .title("Vertex AI PaLM2")
        .description(
            "Vertex AI is a fully-managed, unified AI development platform for building and using generative AI.")
        .icon("path:assets/google-vertex.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(VertexPaLM2Connection.CONNECTION_DEFINITION)
        .actions(VertexPaLM2ChatAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

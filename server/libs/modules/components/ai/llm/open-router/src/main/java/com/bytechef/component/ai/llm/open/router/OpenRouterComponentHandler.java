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

package com.bytechef.component.ai.llm.open.router;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.llm.open.router.action.OpenRouterChatAction;
import com.bytechef.component.ai.llm.open.router.cluster.OpenRouterChatModel;
import com.bytechef.component.ai.llm.open.router.connection.OpenRouterConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Marko Kriskovic
 */
@AutoService(ComponentHandler.class)
public class OpenRouterComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("openRouter")
        .title("Open Router")
        .description("OpenRouter provides a unified API that gives you access to hundreds of AI models through a single endpoint, while automatically handling fallbacks and selecting the most cost-effective options.")
        .icon("path:assets/open-router.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(OpenRouterConnection.CONNECTION_DEFINITION)
        .actions(OpenRouterChatAction.ACTION_DEFINITION)
        .clusterElements(OpenRouterChatModel.CLUSTER_ELEMENT_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

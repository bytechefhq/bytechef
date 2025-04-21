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

package com.bytechef.component.ai.llm.perplexity;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.llm.perplexity.action.PerplexityChatAction;
import com.bytechef.component.ai.llm.perplexity.cluster.PerplexityChatModel;
import com.bytechef.component.ai.llm.perplexity.connection.PerplexityConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Marko Kriskovic
 */
@AutoService(ComponentHandler.class)
public class PerplexityComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("perplexity")
        .title("Perplexity")
        .description(
            "Perplexity AI provides a unique AI service that integrates its language models with real-time search " +
                "capabilities.")
        .icon("path:assets/perplexity.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(PerplexityConnection.CONNECTION_DEFINITION)
        .actions(PerplexityChatAction.ACTION_DEFINITION)
        .clusterElements(PerplexityChatModel.CLUSTER_ELEMENT_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

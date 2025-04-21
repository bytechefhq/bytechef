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

package com.bytechef.component.ai.llm.deepseek;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.llm.deepseek.action.DeepSeekChatAction;
import com.bytechef.component.ai.llm.deepseek.cluster.DeepSeekChatModel;
import com.bytechef.component.ai.llm.deepseek.connection.DeepSeekConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 * @author Marko Kriskovic
 */
@AutoService(ComponentHandler.class)
public class DeepSeekComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("deepseek")
        .title("DeepSeek")
        .description(
            "DeepSeek AI provides the open-source DeepSeek V3 model, renowned for its cutting-edge reasoning and problem-solving capabilities.")
        .icon("path:assets/deepseek.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(DeepSeekConnection.CONNECTION_DEFINITION)
        .actions(DeepSeekChatAction.ACTION_DEFINITION)
        .clusterElements(DeepSeekChatModel.CLUSTER_ELEMENT_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

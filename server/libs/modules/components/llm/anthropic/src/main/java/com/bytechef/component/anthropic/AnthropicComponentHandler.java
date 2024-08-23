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

package com.bytechef.component.anthropic;

import static com.bytechef.component.anthropic.constant.AnthropicConstants.ANTHROPIC;
import static com.bytechef.component.definition.ComponentDSL.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.anthropic.action.AnthropicChatAction;
import com.bytechef.component.anthropic.connection.AnthropicConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class AnthropicComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(ANTHROPIC)
        .title("Anthropic")
        .description(
            "Anthropic is an AI safety and research company that's working to build reliable, interpretable, and steerable AI systems.")
        .icon("path:assets/anthropic.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(AnthropicConnection.CONNECTION_DEFINITION)
        .actions(AnthropicChatAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

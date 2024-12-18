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

package com.bytechef.component.ai.llm.amazon.bedrock;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.llm.amazon.bedrock.action.AmazonBedrockAnthropic2ChatAction;
import com.bytechef.component.ai.llm.amazon.bedrock.action.AmazonBedrockAnthropic3ChatAction;
import com.bytechef.component.ai.llm.amazon.bedrock.action.AmazonBedrockCohereChatAction;
import com.bytechef.component.ai.llm.amazon.bedrock.action.AmazonBedrockJurassic2ChatAction;
import com.bytechef.component.ai.llm.amazon.bedrock.action.AmazonBedrockLlamaChatAction;
import com.bytechef.component.ai.llm.amazon.bedrock.action.AmazonBedrockTitanChatAction;
import com.bytechef.component.ai.llm.amazon.bedrock.connection.AmazonBedrockConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class AmazonBedrockComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("amazonBedrock")
        .title("Amazon Bedrock")
        .description(
            "Amazon Bedrock is a fully managed service that offers a choice of high-performing foundation models " +
                "(FMs) from leading AI companies.")
        .icon("path:assets/amazon-bedrock.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(AmazonBedrockConnection.CONNECTION_DEFINITION)
        .actions(
            AmazonBedrockAnthropic3ChatAction.ACTION_DEFINITION,
            AmazonBedrockAnthropic2ChatAction.ACTION_DEFINITION,
            AmazonBedrockCohereChatAction.ACTION_DEFINITION,
            AmazonBedrockJurassic2ChatAction.ACTION_DEFINITION,
            AmazonBedrockLlamaChatAction.ACTION_DEFINITION,
            AmazonBedrockTitanChatAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

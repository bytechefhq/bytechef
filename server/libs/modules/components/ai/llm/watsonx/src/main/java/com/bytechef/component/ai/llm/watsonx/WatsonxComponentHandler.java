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

package com.bytechef.component.ai.llm.watsonx;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.llm.watsonx.action.WatsonxChatAction;
import com.bytechef.component.ai.llm.watsonx.connection.WatsonxConnection;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 * @author Marko Kriskovic
 */
@AutoService(ComponentHandler.class)
public class WatsonxComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("watsonx")
        .title("Watsonx AI")
        .description(
            "IBM watsonx.ai AI studio is part of the IBM watsonx AI and data platform, bringing together new " +
                "generative AI (gen AI) capabilities powered by foundation models and traditional machine " +
                "learning (ML) into a powerful studio spanning the AI lifecycle.")
        .icon("path:assets/watsonx.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(WatsonxConnection.CONNECTION_DEFINITION)
        .actions(WatsonxChatAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

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

package com.bytechef.component.wolfram.alpha.shortanswers;

import com.bytechef.component.OpenApiComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableComponentDefinition;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.google.auto.service.AutoService;
import java.util.List;

/**
 * @author Nikolina Spehar
 */
@AutoService(OpenApiComponentHandler.class)
public class WolframAlphaShortanswersComponentHandler extends AbstractWolframAlphaShortanswersComponentHandler {

    @Override
    public List<ModifiableActionDefinition> modifyActions(ModifiableActionDefinition... actionDefinitions) {
        for (ModifiableActionDefinition actionDefinition : actionDefinitions) {
            String name = actionDefinition.getName();

            if (name.equals("getShortAnswer")) {
                actionDefinition.help(
                    "",
                    "https://docs.bytechef.io/reference/components/wolfram-alpha-shortanswers_v1#get-short-answer");
            }
        }

        return super.modifyActions(actionDefinitions);
    }

    @Override
    public ModifiableComponentDefinition modifyComponent(ModifiableComponentDefinition modifiableComponentDefinition) {
        return modifiableComponentDefinition
            .icon("path:assets/wolfram-alpha.svg")
            .categories(ComponentCategory.HELPERS)
            .customAction(true)
            .customActionHelp("", "https://products.wolframalpha.com/short-answers-api/documentation/")
            .version(1);
    }

    @Override
    public ModifiableConnectionDefinition modifyConnection(
        ModifiableConnectionDefinition modifiableConnectionDefinition) {

        return modifiableConnectionDefinition
            .help(
                "",
                "https://docs.bytechef.io/reference/components/wolfram-alpha-shortanswers_v1#connection-setup")
            .version(1);
    }
}

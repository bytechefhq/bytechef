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

package com.bytechef.component.gamma;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.gamma.action.GammaGeneratePresentationAction;
import com.bytechef.component.gamma.action.GammaGetGenerationAction;
import com.bytechef.component.gamma.connection.GammaConnection;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class GammaComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("gamma")
        .title("Gamma")
        .description(
            "Gamma is an AI-powered platform for creating presentations, documents, and social posts from text " +
                "prompts, outlines, or existing content.")
        .icon("path:assets/gamma.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE, ComponentCategory.PRODUCTIVITY_AND_COLLABORATION)
        .connection(GammaConnection.CONNECTION_DEFINITION)
        .actions(
            GammaGeneratePresentationAction.ACTION_DEFINITION,
            GammaGetGenerationAction.ACTION_DEFINITION)
        .clusterElements(
            tool(GammaGeneratePresentationAction.ACTION_DEFINITION),
            tool(GammaGetGenerationAction.ACTION_DEFINITION))
        .version(1)
        .customAction(true)
        .customActionHelp("", "https://developers.gamma.app");

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

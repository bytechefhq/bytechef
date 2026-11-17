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

package com.bytechef.component.specstream;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.specstream.action.SpecStreamExtractAction;
import com.bytechef.component.specstream.action.SpecStreamWebSearchAction;
import com.bytechef.component.specstream.connection.SpecStreamConnection;
import com.google.auto.service.AutoService;

/**
 * @author Tarek Etman
 */
@AutoService(ComponentHandler.class)
public class SpecStreamComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("specstream")
        .title("SpecStream")
        .description(
            "SpecStream is a live web data engine. It searches the web for answers with sources and extracts the " +
                "main text of web pages and documents.")
        .icon("path:assets/specstream.svg")
        .categories(ComponentCategory.HELPERS, ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .connection(SpecStreamConnection.CONNECTION_DEFINITION)
        .actions(
            SpecStreamWebSearchAction.ACTION_DEFINITION,
            SpecStreamExtractAction.ACTION_DEFINITION)
        .clusterElements(
            tool(SpecStreamWebSearchAction.ACTION_DEFINITION),
            tool(SpecStreamExtractAction.ACTION_DEFINITION))
        .customAction(true)
        .customActionHelp("SpecStream API", "https://specstream.ai/docs/api")
        .version(1);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

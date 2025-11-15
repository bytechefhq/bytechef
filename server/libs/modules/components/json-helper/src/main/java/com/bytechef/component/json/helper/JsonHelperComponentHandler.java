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

package com.bytechef.component.json.helper;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.json.helper.action.JsonHelperParseAction;
import com.bytechef.component.json.helper.action.JsonHelperStringifyAction;
import com.google.auto.service.AutoService;

/**
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class JsonHelperComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("jsonHelper")
        .title("JSON Helper")
        .description("JSON helper component provides actions for parsing and stringifying JSON.")
        .icon("path:assets/json-helper.svg")
        .categories(ComponentCategory.HELPERS)
        .actions(
            JsonHelperParseAction.ACTION_DEFINITION,
            JsonHelperStringifyAction.ACTION_DEFINITION)
        .clusterElements(
            tool(JsonHelperParseAction.ACTION_DEFINITION),
            tool(JsonHelperStringifyAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }

}

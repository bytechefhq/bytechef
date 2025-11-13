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

package com.bytechef.component.random.helper;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.random.helper.action.RandomHelperRandomFloatAction;
import com.bytechef.component.random.helper.action.RandomHelperRandomHexAction;
import com.bytechef.component.random.helper.action.RandomHelperRandomIntAction;
import com.bytechef.component.random.helper.action.RandomHelperRandomStringAction;
import com.bytechef.component.random.helper.action.RandomHelperRandomUuidAction;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class RandomHelperComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("randomHelper")
        .title("Random Helper")
        .description("The Random Helper allows you to generate random values.")
        .icon("path:assets/random-helper.svg")
        .categories(ComponentCategory.HELPERS)
        .actions(
            RandomHelperRandomFloatAction.ACTION_DEFINITION,
            RandomHelperRandomHexAction.ACTION_DEFINITION,
            RandomHelperRandomIntAction.ACTION_DEFINITION,
            RandomHelperRandomStringAction.ACTION_DEFINITION,
            RandomHelperRandomUuidAction.ACTION_DEFINITION)
        .clusterElements(
            tool(RandomHelperRandomFloatAction.ACTION_DEFINITION),
            tool(RandomHelperRandomHexAction.ACTION_DEFINITION),
            tool(RandomHelperRandomIntAction.ACTION_DEFINITION),
            tool(RandomHelperRandomStringAction.ACTION_DEFINITION),
            tool(RandomHelperRandomUuidAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }

}

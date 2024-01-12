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

package com.bytechef.component.random.helper;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.random.helper.constant.RandomHelperConstants.RANDOM_HELPER;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.random.helper.action.RandomHelperRandomFloatAction;
import com.bytechef.component.random.helper.action.RandomHelperRandomIntAction;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class RandomHelperComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(RANDOM_HELPER)
        .title("Random Helper")
        .description("The Random Helper allows you to generate random values.")
        .icon("path:assets/random-helper.svg")
        .actions(
            RandomHelperRandomIntAction.ACTION_DEFINITION,
            RandomHelperRandomFloatAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }

}

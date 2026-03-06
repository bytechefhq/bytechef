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

package com.bytechef.component.wait;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.wait.constant.WaitConstants.WAIT;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.wait.action.WaitAfterTimeIntervalAction;
import com.bytechef.component.wait.action.WaitAtSpecifiedTimeAction;
import com.bytechef.component.wait.action.WaitOnWebHookCallAction;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class WaitComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(WAIT)
        .title("Wait")
        .description(
            "Pauses the workflow execution for a specified amount of time or until a webhook call is received.")
        .categories(ComponentCategory.HELPERS)
        .icon("path:assets/wait.svg")
        .actions(
            WaitAfterTimeIntervalAction.of(), WaitAtSpecifiedTimeAction.of(), WaitOnWebHookCallAction.of());

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

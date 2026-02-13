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

package com.bytechef.component.logger;

import static com.bytechef.component.logger.constant.LoggerConstants.LOGGER;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.logger.action.LoggerDebugAction;
import com.bytechef.component.logger.action.LoggerErrorAction;
import com.bytechef.component.logger.action.LoggerInfoAction;
import com.bytechef.component.logger.action.LoggerWarnAction;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class LoggerComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = ComponentDsl.component(LOGGER)
        .title("Logger")
        .description("Logs a value to the system log.")
        .icon("path:assets/logger.svg")
        .categories(ComponentCategory.HELPERS)
        .actions(
            LoggerDebugAction.ACTION_DEFINITION,
            LoggerErrorAction.ACTION_DEFINITION,
            LoggerInfoAction.ACTION_DEFINITION,
            LoggerWarnAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

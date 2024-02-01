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

package com.bytechef.component.logger.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.logger.constant.LoggerConstants.DEBUG;
import static com.bytechef.component.logger.constant.LoggerConstants.TEXT;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class LoggerDebugAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(DEBUG)
        .title("Debug")
        .properties(string(TEXT))
        .perform(LoggerDebugAction::perform);

    protected static Map<String, ?> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        Object text = inputParameters.get(TEXT);

        context.logger(logger -> logger.debug(text.toString()));

        return null;
    }
}

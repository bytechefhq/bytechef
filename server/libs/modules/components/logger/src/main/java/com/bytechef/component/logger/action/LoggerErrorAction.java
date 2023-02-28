
/*
 * Copyright 2021 <your company/name>.
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

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.Parameters;
import com.bytechef.hermes.component.definition.ActionDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.bytechef.component.logger.constant.LoggerConstants.ERROR;
import static com.bytechef.component.logger.constant.LoggerConstants.TEXT;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.definition.DefinitionDSL.display;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class LoggerErrorAction {

    private static final Logger logger = LoggerFactory.getLogger(LoggerErrorAction.class);

    public static final ActionDefinition ACTION_DEFINITION = action(ERROR)
        .display(display("Error"))
        .properties(string(TEXT))
        .perform(LoggerErrorAction::performError);

    public static Object performError(Context context, Parameters parameters) {
        logger.error(parameters.getString(TEXT));

        return null;
    }
}

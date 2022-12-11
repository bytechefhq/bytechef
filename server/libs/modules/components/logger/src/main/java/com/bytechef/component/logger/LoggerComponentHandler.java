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

package com.bytechef.component.logger;

import static com.bytechef.component.logger.constants.LoggerConstants.DEBUG;
import static com.bytechef.component.logger.constants.LoggerConstants.ERROR;
import static com.bytechef.component.logger.constants.LoggerConstants.INFO;
import static com.bytechef.component.logger.constants.LoggerConstants.LOGGER;
import static com.bytechef.component.logger.constants.LoggerConstants.TEXT;
import static com.bytechef.component.logger.constants.LoggerConstants.WARN;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class LoggerComponentHandler implements ComponentHandler {

    private static final Logger logger = LoggerFactory.getLogger(LoggerComponentHandler.class);

    private final ComponentDefinition componentDefinition = ComponentDSL.component(LOGGER)
            .display(display("Logger").description("Logs a value to the system log."))
            .actions(
                    action(DEBUG)
                            .display(display("Debug"))
                            .properties(string(TEXT))
                            .perform(this::performDebug),
                    action(ERROR)
                            .display(display("Error"))
                            .properties(string(TEXT))
                            .perform(this::performError),
                    action(INFO)
                            .display(display("Info"))
                            .properties(string(TEXT))
                            .perform(this::performInfo),
                    action(WARN)
                            .display(display("Warn"))
                            .properties(string(TEXT))
                            .perform(this::performWarn));

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    protected Object performDebug(Context context, ExecutionParameters executionParameters) {
        logger.debug(executionParameters.getString(TEXT));

        return null;
    }

    protected Object performError(Context context, ExecutionParameters executionParameters) {
        logger.error(executionParameters.getString(TEXT));

        return null;
    }

    protected Object performInfo(Context context, ExecutionParameters executionParameters) {
        logger.info(executionParameters.getString(TEXT));

        return null;
    }

    protected Object performWarn(Context context, ExecutionParameters executionParameters) {
        logger.warn(executionParameters.getString(TEXT));

        return null;
    }
}


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

package com.bytechef.component.delay.action;

import com.bytechef.component.delay.constant.DelayConstants;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.exception.ActionExecutionException;

import java.util.concurrent.TimeUnit;

import static com.bytechef.component.delay.constant.DelayConstants.DELAY;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.definition.DefinitionDSL.display;
import static com.bytechef.hermes.definition.DefinitionDSL.integer;

/**
 * @author Ivica Cardic
 */
public class DelayDelayAction {

    public static final ActionDefinition ACTION_DEFINITION = action(DELAY)
        .display(display("Delay"))
        .properties(integer(DelayConstants.MILLIS)
            .label("Millis")
            .description("Time in milliseconds.")
            .required(true)
            .defaultValue(1))
        .perform(DelayDelayAction::performDelay);

    public static Object performDelay(Context context, ExecutionParameters executionParameters) {
        try {
            if (executionParameters.containsKey("millis")) {
                Thread.sleep(executionParameters.getLong("millis"));
            } else if (executionParameters.containsKey("duration")) {
                Thread.sleep(executionParameters.getDuration("duration")
                    .toMillis());
            } else {
                TimeUnit.SECONDS.sleep(1);
            }
        } catch (InterruptedException interruptedException) {
            throw new ActionExecutionException("Unable to handle task " + executionParameters, interruptedException);
        }

        return null;
    }
}

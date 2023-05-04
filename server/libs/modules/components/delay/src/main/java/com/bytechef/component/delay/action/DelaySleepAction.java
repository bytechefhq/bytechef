
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
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.exception.ComponentExecutionException;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.bytechef.component.delay.constant.DelayConstants.SLEEP;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;

import static com.bytechef.hermes.definition.DefinitionDSL.integer;

/**
 * @author Ivica Cardic
 */
public class DelaySleepAction {

    public static final ActionDefinition ACTION_DEFINITION = action(SLEEP)
        .title("Sleep")
        .description("Delay action execution.")
        .properties(integer(DelayConstants.MILLIS)
            .label("Millis")
            .description("Time in milliseconds.")
            .required(true)
            .defaultValue(1))
        .execute(DelaySleepAction::executeDelay);

    protected static Object executeDelay(Context context, InputParameters inputParameters) {
        try {
            if (inputParameters.containsKey("millis")) {
                Thread.sleep(inputParameters.getLong("millis"));
            } else if (inputParameters.containsKey("duration")) {
                Duration duration = inputParameters.getDuration("duration");

                Thread.sleep(duration.toMillis());
            } else {
                TimeUnit.SECONDS.sleep(1);
            }
        } catch (InterruptedException interruptedException) {
            throw new ComponentExecutionException("Unable to handle action " + inputParameters, interruptedException);
        }

        return null;
    }
}

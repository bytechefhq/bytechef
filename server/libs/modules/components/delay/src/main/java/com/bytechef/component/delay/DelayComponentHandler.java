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

package com.bytechef.component.delay;

import static com.bytechef.component.delay.constants.DelayConstants.DELAY;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;

import com.bytechef.component.delay.constants.DelayConstants;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.exception.ActionExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Ivica Cardic
 */
public class DelayComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition = ComponentDSL.component(DELAY)
            .display(display("Delay").description("Sets a value which can then be referenced in other tasks."))
            .actions(action(DELAY)
                    .display(display("Delay"))
                    .properties(integer(DelayConstants.MILLIS)
                            .label("Millis")
                            .description("Time in milliseconds.")
                            .required(true)
                            .defaultValue(1))
                    .perform(this::sleep));

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    protected Object sleep(Context context, ExecutionParameters executionParameters) {
        try {
            if (executionParameters.containsKey("millis")) {
                Thread.sleep(executionParameters.getLong("millis"));
            } else if (executionParameters.containsKey("duration")) {
                Thread.sleep(executionParameters.getDuration("duration").toMillis());
            } else {
                TimeUnit.SECONDS.sleep(1);
            }
        } catch (InterruptedException interruptedException) {
            throw new ActionExecutionException("Unable to handle task " + executionParameters, interruptedException);
        }

        return null;
    }
}

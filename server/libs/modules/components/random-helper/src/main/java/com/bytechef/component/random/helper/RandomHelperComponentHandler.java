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

package com.bytechef.component.random.helper;

import static com.bytechef.component.random.helper.constants.RandomHelperConstants.END_INCLUSIVE;
import static com.bytechef.component.random.helper.constants.RandomHelperConstants.RANDOM_FLOAT;
import static com.bytechef.component.random.helper.constants.RandomHelperConstants.RANDOM_HELPER;
import static com.bytechef.component.random.helper.constants.RandomHelperConstants.RANDOM_INT;
import static com.bytechef.component.random.helper.constants.RandomHelperConstants.START_INCLUSIVE;
import static com.bytechef.hermes.component.ComponentDSL.action;
import static com.bytechef.hermes.component.ComponentDSL.createComponent;
import static com.bytechef.hermes.component.ComponentDSL.display;
import static com.bytechef.hermes.component.ComponentDSL.integer;

import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import org.apache.commons.lang3.RandomUtils;

/**
 * @author Ivica Cardic
 */
public class RandomHelperComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition = createComponent(RANDOM_HELPER)
            .display(display("Random Helper").description("The Random Helper allows you to generate random values."))
            .actions(
                    action(RANDOM_INT)
                            .display(display("Int").description("Generates a random integer value."))
                            .inputs(
                                    integer(START_INCLUSIVE)
                                            .description("The minimum possible generated value.")
                                            .required(true)
                                            .defaultValue(0),
                                    integer(END_INCLUSIVE)
                                            .description("The maximum possible generated value.")
                                            .required(true)
                                            .defaultValue(100))
                            .performFunction(this::performNextInt),
                    action(RANDOM_FLOAT)
                            .display(display("Float").description("Generates a random float value."))
                            .performFunction(this::performNextFloat));

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    /**
     * Generates a random integer.
     */
    protected int performNextInt(Context context, ExecutionParameters executionParameters) {
        int startInclusive = executionParameters.getInteger("startInclusive", 0);
        int endInclusive = executionParameters.getInteger("endInclusive", 100);

        return RandomUtils.nextInt(startInclusive, endInclusive);
    }

    /**
     * Generates a random float.
     */
    protected Object performNextFloat(Context context, ExecutionParameters executionParameters) {
        int startInclusive = executionParameters.getInteger("startInclusive", 0);
        int endInclusive = executionParameters.getInteger("endInclusive", 100);

        return RandomUtils.nextFloat(startInclusive, endInclusive);
    }
}

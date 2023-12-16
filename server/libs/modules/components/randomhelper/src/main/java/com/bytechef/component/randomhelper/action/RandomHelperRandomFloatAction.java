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

package com.bytechef.component.randomhelper.action;

import static com.bytechef.component.randomhelper.constant.RandomHelperConstants.END_INCLUSIVE;
import static com.bytechef.component.randomhelper.constant.RandomHelperConstants.RANDOM_FLOAT;
import static com.bytechef.component.randomhelper.constant.RandomHelperConstants.START_INCLUSIVE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;

import com.bytechef.component.randomhelper.constant.RandomHelperConstants;
import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.ParameterMap;
import com.bytechef.hermes.component.exception.ComponentExecutionException;

/**
 * @author Ivica Cardic
 */
public class RandomHelperRandomFloatAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(RANDOM_FLOAT)
        .title("Float")
        .description("Generates a random float value.")
        .properties(
            integer(START_INCLUSIVE)
                .description("The minimum possible generated value.")
                .required(true)
                .defaultValue(0),
            integer(END_INCLUSIVE)
                .description("The maximum possible generated value.")
                .required(true)
                .defaultValue(100))
        .perform(RandomHelperRandomFloatAction::perform);

    /**
     * Generates a random float.
     */
    protected static Object perform(
        ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext context) {

        int startInclusive = inputParameters.getInteger(START_INCLUSIVE, 0);
        int endInclusive = inputParameters.getInteger(END_INCLUSIVE, 100);

        return nextFloat(startInclusive, endInclusive);
    }

    private static float nextFloat(final float startInclusive, final float endExclusive) {
        if (endExclusive < startInclusive) {
            throw new ComponentExecutionException("Start value must be smaller or equal to end value");
        }

        if (startInclusive < 0) {
            throw new ComponentExecutionException("Both range values must be non-negative");
        }

        if (startInclusive == endExclusive) {
            return startInclusive;
        }

        return startInclusive + ((endExclusive - startInclusive) * RandomHelperConstants.RANDOM.nextFloat());
    }
}


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

package com.bytechef.component.randomhelper.action;

import com.bytechef.component.randomhelper.constant.RandomHelperConstants;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.Parameters;
import com.bytechef.hermes.component.definition.ActionDefinition;

import static com.bytechef.component.randomhelper.constant.RandomHelperConstants.END_INCLUSIVE;
import static com.bytechef.component.randomhelper.constant.RandomHelperConstants.RANDOM_INT;
import static com.bytechef.component.randomhelper.constant.RandomHelperConstants.START_INCLUSIVE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.definition.DefinitionDSL.display;
import static com.bytechef.hermes.definition.DefinitionDSL.integer;

/**
 * @author Ivica Cardic
 */
public class RandomHelperRandomIntAction {

    public static final ActionDefinition ACTION_DEFINITION = action(RANDOM_INT)
        .display(display("Int").description("Generates a random integer value."))
        .properties(
            integer(START_INCLUSIVE)
                .description("The minimum possible generated value.")
                .required(true)
                .defaultValue(0),
            integer(END_INCLUSIVE)
                .description("The maximum possible generated value.")
                .required(true)
                .defaultValue(100))
        .perform(RandomHelperRandomIntAction::performNextInt);

    /**
     * Generates a random integer.
     */
    public static int performNextInt(Context context, Parameters parameters) {
        int startInclusive = parameters.getInteger("startInclusive", 0);
        int endInclusive = parameters.getInteger("endInclusive", 100);

        return nextInt(startInclusive, endInclusive);
    }

    private static int nextInt(final int startInclusive, final int endExclusive) {
        if (endExclusive < startInclusive) {
            throw new IllegalArgumentException("Start value must be smaller or equal to end value");
        }

        if (startInclusive < 0) {
            throw new IllegalArgumentException("Both range values must be non-negative");
        }

        if (startInclusive == endExclusive) {
            return startInclusive;
        }

        return startInclusive + RandomHelperConstants.RANDOM.nextInt(endExclusive - startInclusive);
    }
}

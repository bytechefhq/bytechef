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

package com.bytechef.component.random.helper.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.random.helper.constant.RandomHelperConstants.END_INCLUSIVE;
import static com.bytechef.component.random.helper.constant.RandomHelperConstants.START_INCLUSIVE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.random.helper.constant.RandomHelperConstants;

/**
 * @author Ivica Cardic
 */
public class RandomHelperRandomIntAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("randomInt")
        .title("Random Integer")
        .description("Generates a random integer value.")
        .help("", "https://docs.bytechef.io/reference/components/random-helper_v1#random-integer")
        .properties(
            integer(START_INCLUSIVE)
                .label("Start Inclusive")
                .description("The minimum possible generated value.")
                .required(true)
                .defaultValue(0),
            integer(END_INCLUSIVE)
                .label("End Inclusive")
                .description("The maximum possible generated value.")
                .required(true)
                .defaultValue(100))
        .output(
            outputSchema(
                integer()
                    .description("Generated random integer value.")))
        .perform(RandomHelperRandomIntAction::perform);

    private RandomHelperRandomIntAction() {
    }

    public static Integer perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        int startInclusive = inputParameters.getInteger(START_INCLUSIVE, 0);
        int endInclusive = inputParameters.getInteger(END_INCLUSIVE, 100);

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

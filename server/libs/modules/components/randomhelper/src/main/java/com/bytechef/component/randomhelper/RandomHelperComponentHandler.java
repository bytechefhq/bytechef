
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

package com.bytechef.component.randomhelper;

import static com.bytechef.component.randomhelper.constants.RandomHelperConstants.END_INCLUSIVE;
import static com.bytechef.component.randomhelper.constants.RandomHelperConstants.RANDOM_FLOAT;
import static com.bytechef.component.randomhelper.constants.RandomHelperConstants.RANDOM_HELPER;
import static com.bytechef.component.randomhelper.constants.RandomHelperConstants.RANDOM_INT;
import static com.bytechef.component.randomhelper.constants.RandomHelperConstants.START_INCLUSIVE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.integer;

import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.definition.ComponentDefinition;

import java.util.Random;

/**
 * @author Ivica Cardic
 */
public class RandomHelperComponentHandler implements ComponentHandler {

    private static final Random RANDOM = new Random();

    private final ComponentDefinition componentDefinition = component(RANDOM_HELPER)
        .display(display("Random Helper").description("The Random Helper allows you to generate random values."))
        .actions(
            action(RANDOM_INT)
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
                .perform(this::performNextInt),
            action(RANDOM_FLOAT)
                .display(display("Float").description("Generates a random float value."))
                .perform(this::performNextFloat));

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

        return nextInt(startInclusive, endInclusive);
    }

    /**
     * Generates a random float.
     */
    protected Object performNextFloat(Context context, ExecutionParameters executionParameters) {
        int startInclusive = executionParameters.getInteger("startInclusive", 0);
        int endInclusive = executionParameters.getInteger("endInclusive", 100);

        return nextFloat(startInclusive, endInclusive);
    }

    private float nextFloat(final float startInclusive, final float endExclusive) {
        if (endExclusive < startInclusive) {
            throw new IllegalArgumentException("Start value must be smaller or equal to end value.");
        }

        if (startInclusive < 0) {
            throw new IllegalArgumentException("Both range values must be non-negative.");
        }

        if (startInclusive == endExclusive) {
            return startInclusive;
        }

        return startInclusive + ((endExclusive - startInclusive) * RANDOM.nextFloat());
    }

    private int nextInt(final int startInclusive, final int endExclusive) {
        if (endExclusive < startInclusive) {
            throw new IllegalArgumentException("Start value must be smaller or equal to end value.");
        }

        if (startInclusive < 0) {
            throw new IllegalArgumentException("Both range values must be non-negative.");
        }

        if (startInclusive == endExclusive) {
            return startInclusive;
        }

        return startInclusive + RANDOM.nextInt(endExclusive - startInclusive);
    }
}

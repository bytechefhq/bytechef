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

package com.bytechef.component.math.helper.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.math.helper.constants.MathHelperConstants.FIRST_NUMBER;
import static com.bytechef.component.math.helper.constants.MathHelperConstants.MULTIPLICATION;
import static com.bytechef.component.math.helper.constants.MathHelperConstants.MULTIPLICATION_DESCRIPTION;
import static com.bytechef.component.math.helper.constants.MathHelperConstants.MULTIPLICATION_TITLE;
import static com.bytechef.component.math.helper.constants.MathHelperConstants.SECOND_NUMBER;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import com.bytechef.component.definition.Property.NumberProperty;
import com.bytechef.definition.BaseOutputDefinition.OutputSchema;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Monika Kušter
 */
public class MathHelperMultiplicationAction {

    @SuppressFBWarnings("MS")
    public static final Property[] PROPERTIES = {
        number(FIRST_NUMBER)
            .label("First Number")
            .required(true),
        number(SECOND_NUMBER)
            .label("Second Number")
            .required(true)
    };

    public static final OutputSchema<NumberProperty> OUTPUT_SCHEMA = outputSchema(
        number().description("Result of multiplication."));

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(MULTIPLICATION)
        .title(MULTIPLICATION_TITLE)
        .description(MULTIPLICATION_DESCRIPTION)
        .properties(PROPERTIES)
        .output(OUTPUT_SCHEMA)
        .perform(MathHelperMultiplicationAction::perform);

    private MathHelperMultiplicationAction() {
    }

    public static Double perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        double firstNumber = inputParameters.getRequiredDouble(FIRST_NUMBER);
        double secondNumber = inputParameters.getRequiredDouble(SECOND_NUMBER);

        return firstNumber * secondNumber;
    }
}

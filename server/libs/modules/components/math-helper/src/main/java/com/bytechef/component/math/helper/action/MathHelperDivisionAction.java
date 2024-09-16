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
import static com.bytechef.component.math.helper.constants.MathHelperConstants.SECOND_NUMBER;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Kušter
 */
public class MathHelperDivisionAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("division")
        .title("Division")
        .description("Divide two numbers.")
        .properties(
            number(FIRST_NUMBER)
                .label("First number")
                .description("Number to be divided.")
                .required(true),
            number(SECOND_NUMBER)
                .label("Second number")
                .description("Number to divide by.")
                .required(true))
        .output(outputSchema(number()))
        .perform(MathHelperDivisionAction::perform);

    private MathHelperDivisionAction() {
    }

    protected static Double perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        double firstNumber = inputParameters.getRequiredDouble(FIRST_NUMBER);
        double secondNumber = inputParameters.getRequiredDouble(SECOND_NUMBER);

        if (secondNumber == 0) {
            throw new UnsupportedOperationException("Division by zero is not allowed.");
        } else {
            return firstNumber / secondNumber;
        }
    }
}

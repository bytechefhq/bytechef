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
import java.math.BigDecimal;

/**
 * @author Monika Kušter
 */
public class MathHelperModuloAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("modulo")
        .title("Modulo")
        .description("Get the remainder of the division of two numbers.")
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
        .perform(MathHelperModuloAction::perform);

    private MathHelperModuloAction() {
    }

    protected static Double perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        BigDecimal firstNumber = BigDecimal.valueOf(inputParameters.getRequiredDouble(FIRST_NUMBER));
        BigDecimal secondNumber = BigDecimal.valueOf(inputParameters.getRequiredDouble(SECOND_NUMBER));

        return firstNumber.remainder(secondNumber)
            .doubleValue();
    }
}

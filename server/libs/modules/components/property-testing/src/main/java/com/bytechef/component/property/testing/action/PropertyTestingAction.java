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

package com.bytechef.component.property.testing.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.ARRAY_MAX_ITEMS;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.ARRAY_MIN_ITEMS;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.INTEGER_MAX_VALUE;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.INTEGER_MIN_VALUE;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.NUMBER_MAX_PRECISION;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.NUMBER_MAX_VALUE;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.NUMBER_MIN_PRECISION;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.NUMBER_MIN_VALUE;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.NUMBER_PRECISION;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.STRING_MAX_LENGTH;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.STRING_MIN_LENGTH;
import static com.bytechef.component.property.testing.constant.PropertyTestingConstants.STRING_REG_EX;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;

public class PropertyTestingAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("testingAction")
        .title("Testing")
        .description("Description")
        .properties(
            array(ARRAY_MAX_ITEMS)
                .label("Array Max Items")
                .maxItems(3)
                .description("Max items set to 3")
                .items(string()),
            array(ARRAY_MIN_ITEMS)
                .label("Array Min Items")
                .minItems(3)
                .description("Min items set to 3")
                .items(string()),
            integer(INTEGER_MAX_VALUE)
                .maxValue(10)
                .label("Integer Max Value")
                .description("Integer maximum value set to 10"),
            integer(INTEGER_MIN_VALUE)
                .minValue(10)
                .label("Integer Min Value")
                .description("Integer minimum value set to 10"),
            number(NUMBER_MAX_PRECISION)
                .label("Number Max Number Precision")
                .description("Number max number precision set to 2")
                .maxNumberPrecision(2),
            number(NUMBER_MIN_PRECISION)
                .label("Number Min Number Precision")
                .description("Number min number precision set to 2")
                .minNumberPrecision(2),
            number(NUMBER_MAX_VALUE)
                .label("Number Max Value")
                .description("Number max value set to 5")
                .maxValue(5),
            number(NUMBER_MIN_VALUE)
                .label("Number Min Value")
                .description("Number min value set to 5")
                .minValue(5),
            number(NUMBER_PRECISION)
                .label("Number Precision")
                .description("Number precision set to 3")
                .numberPrecision(3),
            string(STRING_MAX_LENGTH)
                .label("String Max Length")
                .maxLength(5)
                .description("Maximum length set to 5."),
            string(STRING_MIN_LENGTH)
                .label("String Min Length")
                .description("Minimum length set to 5.")
                .minLength(5),
            string(STRING_REG_EX)
                .label("String Regular Expression")
                .description(
                    "Regular expression is set to: \"[^A-Za-z]\". Just letters from a text should be returned.")
                .regex("[^A-Za-z]"))
        .output(outputSchema(object()))
        .perform(PropertyTestingAction::perform);

    private PropertyTestingAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return inputParameters;
    }
}

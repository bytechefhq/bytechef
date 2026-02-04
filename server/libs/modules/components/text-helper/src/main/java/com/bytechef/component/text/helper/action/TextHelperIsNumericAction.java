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

package com.bytechef.component.text.helper.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * @author Nikolina Špehar
 */
public class TextHelperIsNumericAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("isNumeric")
        .title("Is Numeric?")
        .description("Check if a text string is a number.")
        .properties(
            string(TEXT)
                .description("The input text that will be checked. Decimal point is a point '.'.")
                .label("Text")
                .required(true))
        .output(outputSchema(bool().description("Whether the input text is number.")))
        .perform(TextHelperIsNumericAction::perform);

    private TextHelperIsNumericAction() {
    }

    public static boolean perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String text = inputParameters.getRequiredString(TEXT);

        return NumberUtils.isCreatable(text);
    }
}

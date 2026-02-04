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
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.exception.ProviderException;

/**
 * @author Nikolina Špehar
 */
public class TextHelperChangeTypeAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("changeType")
        .title("Change Type")
        .description("Change the type of the input text to number.")
        .properties(
            string(TEXT)
                .description("The input text to be changed to a number.")
                .label("Text")
                .required(true))
        .output(outputSchema(number().description("Number input text")))
        .help("", "https://docs.bytechef.io/reference/components/text-helper_v1#change-type")
        .perform(TextHelperChangeTypeAction::perform);

    private TextHelperChangeTypeAction() {
    }

    public static double perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String text = inputParameters.getRequiredString(TEXT);

        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException numberFormatException) {
            throw new ProviderException(text + " can not be converted to number.");
        }
    }
}

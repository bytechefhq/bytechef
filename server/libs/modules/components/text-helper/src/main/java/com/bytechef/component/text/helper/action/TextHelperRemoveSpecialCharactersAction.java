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
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;

/**
 * @author Nikolina Špehar
 */
public class TextHelperRemoveSpecialCharactersAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("removeSpecialCharacters")
        .title("Remove Special Characters")
        .description("Remove special characters from a string.")
        .properties(
            string(TEXT)
                .label("Text")
                .description("The text that will be processed to remove special characters.")
                .required(true))
        .output(outputSchema(string()
            .description("Result of removing special characters from a string.")))
        .perform(TextHelperRemoveSpecialCharactersAction::perform);

    private TextHelperRemoveSpecialCharactersAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String text = inputParameters.getRequiredString(TEXT);
        String regularExpression = "[^a-zA-Z0-9]";

        return text.replaceAll(regularExpression, "");
    }
}

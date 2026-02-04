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
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.NUMBER_OF_CHARACTERS;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT_PROPERTY;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Kušter
 */
public class TextHelperSelectFirstNCharactersAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("selectFirstNCharacters")
        .title("Select First N Characters")
        .description("Select the first N characters from a string.")
        .properties(
            TEXT_PROPERTY,
            integer(NUMBER_OF_CHARACTERS)
                .label("Number of Characters")
                .description("The number of characters to select.")
                .required(true))
        .output(outputSchema(string().description("Lower case string.")))
        .help(
            "",
            "https://docs.bytechef.io/reference/components/text-helper_v1#select-first-n-characters")
        .perform(TextHelperSelectFirstNCharactersAction::perform);

    private TextHelperSelectFirstNCharactersAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String text = inputParameters.getRequiredString(TEXT);

        return text.substring(0, inputParameters.getRequiredInteger(NUMBER_OF_CHARACTERS));
    }
}

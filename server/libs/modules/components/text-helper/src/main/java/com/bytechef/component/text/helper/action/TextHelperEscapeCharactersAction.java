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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.ESCAPE_CHARACTER;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.ESCAPE_CHARACTERS;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.List;

/**
 * @author Nikolina Špehar
 */
public class TextHelperEscapeCharactersAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("escapeCharacters")
        .title("Escape Characters")
        .description("Escape characters in a string, specified in the input.")
        .properties(
            string(TEXT)
                .label("Text")
                .description("The text in which you want to escape characters.")
                .required(true),
            array(ESCAPE_CHARACTERS)
                .label("Escape Characters")
                .description("Characters you want to escape.")
                .required(true)
                .items(
                    string(ESCAPE_CHARACTER)
                        .label("Escape Character")
                        .description("Character you want to escape.")
                        .maxLength(2)
                        .required(true)))
        .output(outputSchema(string().description("Text without escaped character.")))
        .perform(TextHelperEscapeCharactersAction::perform);

    private TextHelperEscapeCharactersAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String text = inputParameters.getRequiredString(TEXT);

        List<String> escapeCharacters = inputParameters.getRequiredList(ESCAPE_CHARACTERS, String.class);

        for (String escapeChar : escapeCharacters) {
            text = text.replace(escapeChar, "\\" + escapeChar);
        }

        return text;
    }
}

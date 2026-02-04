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
public class TextHelperSentenceCaseAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sentenceCase")
        .title("Sentence Case")
        .description("Converts string into sentence case.")
        .properties(
            string(TEXT)
                .description("The input text that will be converted to sentence case.")
                .label("Text")
                .required(true))
        .output(outputSchema(string().description("Sentence case text")))
        .help("", "https://docs.bytechef.io/reference/components/text-helper_v1#sentence-case")
        .perform(TextHelperSentenceCaseAction::perform);

    private TextHelperSentenceCaseAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String text = inputParameters.getRequiredString(TEXT);
        text = text.toLowerCase()
            .trim();

        StringBuilder sentenceCaseString = new StringBuilder(text.length());
        boolean capitalizeNext = true;

        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);

            if (capitalizeNext && Character.isLetter(character)) {
                sentenceCaseString.append(Character.toUpperCase(character));
                capitalizeNext = false;
            } else {
                sentenceCaseString.append(character);
            }

            if (character == '.' || character == '!' || character == '?') {
                capitalizeNext = true;
            }
        }

        return sentenceCaseString.toString();
    }
}

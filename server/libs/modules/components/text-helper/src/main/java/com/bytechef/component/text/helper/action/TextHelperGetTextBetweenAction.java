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
import static com.bytechef.component.text.helper.constant.TextHelperConstants.PATTERN_END;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.PATTERN_START;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;
import static com.bytechef.component.text.helper.util.TextHelperUtils.getPatternEndIndex;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;

/**
 * @author Nikolina Špehar
 */
public class TextHelperGetTextBetweenAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getTextBetween")
        .title("Get Text Between")
        .description("Extract text between two patterns.")
        .properties(
            string(TEXT)
                .label("Text")
                .description("The text that will be searched for the patterns.")
                .required(true),
            string(PATTERN_START)
                .label("Start Pattern")
                .description("Start pattern from where substring will be extracted.")
                .required(true),
            string(PATTERN_END)
                .label("End Pattern")
                .description("End pattern to where substring will be extracted.")
                .required(true))
        .output(outputSchema(string().description("Extracted substring.")))
        .perform(TextHelperGetTextBetweenAction::perform);

    private TextHelperGetTextBetweenAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String text = inputParameters.getRequiredString(TEXT);
        String startPattern = inputParameters.getRequiredString(PATTERN_START);
        String endPattern = inputParameters.getRequiredString(PATTERN_END);

        int startPatternEndIndex = getPatternEndIndex(startPattern, text, 1);
        int endPatternIndex = getPatternEndIndex(endPattern, text, 1) - endPattern.length();

        return text.substring(startPatternEndIndex, endPatternIndex);
    }
}

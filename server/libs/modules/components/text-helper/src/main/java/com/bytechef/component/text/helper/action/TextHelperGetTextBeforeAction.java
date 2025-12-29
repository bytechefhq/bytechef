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
import static com.bytechef.component.text.helper.constant.TextHelperConstants.MATCH_NUMBER;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.PATTERN;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;
import static com.bytechef.component.text.helper.util.TextHelperUtils.getPatternEndIndex;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;

/**
 * @author Nikolina Špehar
 */
public class TextHelperGetTextBeforeAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getTextBefore")
        .title("Get Text Before")
        .description(
            "Given a string and a pattern, this operation will return the substring between where the pattern was " +
                "found depending on the match number and beginning of the string.")
        .properties(
            string(TEXT)
                .label("Text")
                .description("The text that will be searched for the pattern.")
                .required(true),
            string(PATTERN)
                .label("Pattern")
                .description("Pattern after which substring will be extracted.")
                .required(true),
            integer(MATCH_NUMBER)
                .label("Match Number")
                .description("Specifies which match to use for extracting the substring when multiple matches exist.")
                .required(true))
        .output(outputSchema(string().description("Extracted substring.")))
        .perform(TextHelperGetTextBeforeAction::perform);

    private TextHelperGetTextBeforeAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String text = inputParameters.getRequiredString(TEXT);
        String pattern = inputParameters.getRequiredString(PATTERN);
        int matchNumber = inputParameters.getRequiredInteger(MATCH_NUMBER);

        int patternIndex = getPatternEndIndex(pattern, text, matchNumber);

        return text.substring(0, patternIndex - pattern.length());
    }
}

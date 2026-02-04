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
import static com.bytechef.component.text.helper.constant.TextHelperConstants.REGULAR_EXPRESSION;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.text.helper.util.TextHelperUtils;
import java.util.List;

/**
 * @author Nikolina Špehar
 */
public class TextHelperMatchAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("match")
        .title("Match")
        .description("Retrieve the result of matching a string against a regular expression.")
        .properties(
            string(TEXT)
                .label("Text")
                .description("The text that will be matched to the regular expression.")
                .required(true),
            string(REGULAR_EXPRESSION)
                .label("Regular Expression")
                .description("Regular expression that will be used on the text.")
                .required(true))
        .output(outputSchema(
            array()
                .description("Array of results of matching a string against a regular expression.")
                .items(
                    string()
                        .description("Result of matching a string against a regular expression."))))
        .help("", "https://docs.bytechef.io/reference/components/text-helper_v1#match")
        .perform(TextHelperMatchAction::perform);

    private TextHelperMatchAction() {
    }

    public static List<String> perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String text = inputParameters.getRequiredString(TEXT);
        String regularExpression = inputParameters.getRequiredString(REGULAR_EXPRESSION);

        return TextHelperUtils.extractByRegEx(text, regularExpression);
    }
}

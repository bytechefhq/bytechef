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
import static com.bytechef.component.text.helper.constant.TextHelperConstants.IGNORE_CASE;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.MULTILINE;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.REGULAR_EXPRESSION;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.UNICODE;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Nikolina Špehar
 */
public final class TextHelperRegularExpressionMatchTestAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("regExMatchTest")
        .title("Regular Expression Match Test")
        .description("Test if a string matches a regex.")
        .properties(
            string(TEXT)
                .label("Text")
                .description("The text that will be matched to the regular expression.")
                .required(true),
            string(REGULAR_EXPRESSION)
                .label("Regular Expression")
                .description("Regular expression that will be used on the text.")
                .required(true),
            bool(IGNORE_CASE)
                .label("Ignore Case")
                .description("If this value is set to true the regular expression will be case-insensitive.")
                .required(false)
                .defaultValue(false),
            bool(MULTILINE)
                .label("Multiline")
                .description("If this value is set to true the regular expression will be applied to multiple lines.")
                .required(false)
                .defaultValue(false),
            bool(UNICODE)
                .label("Unicode")
                .description("If this value is set to true the regular expression will support unicode characters.")
                .required(false)
                .defaultValue(false))
        .output(outputSchema(bool().description("Result of testing if a string matches a regular expression.")))
        .perform(TextHelperRegularExpressionMatchTestAction::perform);

    private TextHelperRegularExpressionMatchTestAction() {
    }

    public static boolean perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String text = inputParameters.getRequiredString(TEXT);
        String regularExpression = inputParameters.getRequiredString(REGULAR_EXPRESSION);

        boolean ignoreCase = inputParameters.getBoolean(IGNORE_CASE);
        boolean multiline = inputParameters.getBoolean(MULTILINE);
        boolean unicode = inputParameters.getBoolean(UNICODE);

        int flags = 0;

        if (ignoreCase) {
            flags |= Pattern.CASE_INSENSITIVE;
        }

        if (multiline) {
            flags |= Pattern.MULTILINE;
        }

        if (unicode) {
            flags |= Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS;
        }

        Pattern pattern = Pattern.compile(regularExpression, flags);
        Matcher matcher = pattern.matcher(text);

        return matcher.find();
    }
}

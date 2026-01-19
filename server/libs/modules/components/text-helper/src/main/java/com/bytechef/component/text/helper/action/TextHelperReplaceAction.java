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
import static com.bytechef.component.text.helper.constant.TextHelperConstants.REPLACE_ONLY_FIRST;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.REPLACE_VALUE;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.SEARCH_VALUE;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT_PROPERTY;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Kušter
 */
public class TextHelperReplaceAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("replace")
        .title("Replace")
        .description("Replace all instances of any word, character, or phrase in text with another.")
        .properties(
            TEXT_PROPERTY,
            string(SEARCH_VALUE)
                .label("Search Value")
                .description("Can be plain text or a regex expression.")
                .required(true),
            string(REPLACE_VALUE)
                .label("Replace Value")
                .description("The text")
                .description("Leave blank to remove the search value.")
                .required(false),
            bool(REPLACE_ONLY_FIRST)
                .label("Replace Only First Match")
                .defaultValue(false)
                .required(true))
        .output(outputSchema(string().description("The text with replaced values.")))
        .perform(TextHelperReplaceAction::perform);

    private TextHelperReplaceAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String text = inputParameters.getRequiredString(TEXT);
        String searchValue = inputParameters.getRequiredString(SEARCH_VALUE);
        String replaceValue = inputParameters.getRequiredString(REPLACE_VALUE);

        if (inputParameters.getRequiredBoolean(REPLACE_ONLY_FIRST)) {
            return text.replaceFirst(searchValue, replaceValue);
        } else {
            return text.replaceAll(searchValue, replaceValue);
        }
    }
}

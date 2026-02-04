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
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT_PROPERTY;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Kušter
 */
public class TextHelperTrimWhitespaceAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("trimWhitespace")
        .title("Trim Whitespace")
        .description("Trim whitespace from the beginning and end of a string.")
        .properties(TEXT_PROPERTY)
        .output(outputSchema(string().description("Trimmed string.")))
        .help("", "https://docs.bytechef.io/reference/components/text-helper_v1#trim-whitespace")
        .perform(TextHelperTrimWhitespaceAction::perform);

    private TextHelperTrimWhitespaceAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String text = inputParameters.getRequiredString(TEXT);

        return text.trim();
    }
}

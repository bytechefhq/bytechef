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
public class TextHelperUpperCaseAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("upperCase")
        .title("Upper Case")
        .description("Convert a string to upper case.")
        .properties(TEXT_PROPERTY)
        .output(outputSchema(string().description("Upper case string.")))
        .perform(TextHelperUpperCaseAction::perform);

    private TextHelperUpperCaseAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return inputParameters.getRequiredString(TEXT)
            .toUpperCase();
    }
}

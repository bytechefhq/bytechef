/*
 * Copyright 2023-present ByteChef Inc.
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
import static com.bytechef.component.text.helper.constant.TextHelperConstants.EXPRESSION;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;

/**
 * @author Monika Kušter
 */
public class TextHelperFindAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("find")
        .title("Find")
        .description("Find substring")
        .properties(
            TEXT_PROPERTY,
            string(EXPRESSION)
                .label("Expression")
                .description("Text to search for.")
                .required(true))
        .output(outputSchema(bool()))
        .perform(TextHelperFindAction::perform);

    private TextHelperFindAction() {
    }

    protected static boolean perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        String text = inputParameters.getRequiredString(TEXT);

        return text.contains(inputParameters.getRequiredString(EXPRESSION));
    }
}

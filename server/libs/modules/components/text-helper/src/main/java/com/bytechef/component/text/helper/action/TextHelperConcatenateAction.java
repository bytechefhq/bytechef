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
import static com.bytechef.component.text.helper.constant.TextHelperConstants.SEPARATOR;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXTS;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.List;

/**
 * @author Monika Kušter
 */
public class TextHelperConcatenateAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("concatenate")
        .title("Concatenate")
        .description("Concatenate a list of texts.")
        .properties(
            array(TEXTS)
                .label("Texts")
                .description("A list of texts to concatenate.")
                .items(TEXT_PROPERTY)
                .required(true),
            string(SEPARATOR)
                .label("Separator")
                .description("The text that separates the texts you want to concatenate.")
                .required(false))
        .output(outputSchema(string().description("The concatenated text.")))
        .perform(TextHelperConcatenateAction::perform);

    private TextHelperConcatenateAction() {
    }

    public static String perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        List<String> texts = inputParameters.getRequiredList(TEXTS, String.class);

        return String.join(inputParameters.getString(SEPARATOR, ""), texts);
    }
}

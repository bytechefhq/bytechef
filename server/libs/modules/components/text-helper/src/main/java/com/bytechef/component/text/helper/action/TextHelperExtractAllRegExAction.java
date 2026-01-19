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
import static com.bytechef.component.text.helper.util.TextHelperUtils.extractByRegEx;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.List;

/**
 * @author Nikolina Špehar
 */
public class TextHelperExtractAllRegExAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("extractAllRegEx")
        .title("Extract All by Regular Expression")
        .description("Extract all strings that match a given pattern.")
        .properties(
            string(TEXT)
                .label("Text")
                .description("The text on which regular expression will be used on.")
                .required(true),
            string(REGULAR_EXPRESSION)
                .label("Regular Expression")
                .description("Regular expression that will be used for extracting strings.")
                .required(true))
        .output(
            outputSchema(
                array()
                    .description("Array of strings that were extracted")
                    .items(
                        string()
                            .description("String that matches the regular expression."))))
        .perform(TextHelperExtractAllRegExAction::perform);

    private TextHelperExtractAllRegExAction() {
    }

    public static List<String> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return extractByRegEx(
            inputParameters.getRequiredString(TEXT),
            inputParameters.getRequiredString(REGULAR_EXPRESSION));
    }
}

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
import static com.bytechef.component.text.helper.constant.TextHelperConstants.TEXT;
import static com.bytechef.component.text.helper.util.TextHelperUtils.extractByRegEx;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import java.util.List;

/**
 * @author Monika Kušter
 */
public class TextHelperExtractUrlsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("extractUrls")
        .title("Extract URLs")
        .description("Extract all of the URLs from a given piece of text, returning them as a list.")
        .properties(
            string(TEXT)
                .label("Text")
                .description("The text from which to extract URLs.")
                .controlType(ControlType.TEXT_AREA)
                .required(true))
        .output(outputSchema(array().description("Extracted URLs.")
            .items(string())))
        .perform(TextHelperExtractUrlsAction::perform);

    private TextHelperExtractUrlsAction() {
    }

    public static List<String> perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String regexStr =
            "\\b((?:https?|ftp|file)://[a-zA-Z0-9+&@#/%?=~_|!:,.;]*[a-zA-Z0-9+&@#/%=~_|]|www\\.[a-zA-Z0-9-]+\\.[a-zA-Z]{2,}(?:/[\\w+&@#/%?=~_|!:,.;]*)?)";

        return extractByRegEx(inputParameters.getRequiredString(TEXT), regexStr);
    }
}

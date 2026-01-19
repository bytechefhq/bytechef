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
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.KEY_VALUE_OBJECT;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.REGULAR_EXPRESSION;
import static com.bytechef.component.text.helper.util.TextHelperUtils.extractByRegEx;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Špehar
 */
public class TextHelperExtractKeyRegExAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("extractKeyRegEx")
        .title("Extract Key by Regular Expression")
        .description("Extract first string that match a given pattern.")
        .properties(
            object(KEY_VALUE_OBJECT)
                .label("Key-Value Object")
                .description("The object on which regular expression will be used on.")
                .required(true),
            string(REGULAR_EXPRESSION)
                .label("Regular Expression")
                .description("Extract key by regex match.")
                .required(true))
        .output(
            outputSchema(
                array()
                    .description("Keys that match the regular expression.")
                    .items(
                        string()
                            .description("Key that matches the regular expression."))))
        .perform(TextHelperExtractKeyRegExAction::perform);

    private TextHelperExtractKeyRegExAction() {
    }

    public static List<String> perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, Object> keyValueMap = inputParameters.getRequiredMap(KEY_VALUE_OBJECT, Object.class);
        String regex = inputParameters.getRequiredString(REGULAR_EXPRESSION);

        List<String> extractedKeys = new ArrayList<>();

        for (String key : keyValueMap.keySet()) {
            List<String> matches = extractByRegEx(key, regex);

            if (!matches.isEmpty()) {
                extractedKeys.add(key);
            }
        }

        return extractedKeys;
    }
}

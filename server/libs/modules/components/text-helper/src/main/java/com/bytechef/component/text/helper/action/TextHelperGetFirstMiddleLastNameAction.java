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
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.FIRST_NAME;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.FULL_NAME;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.IS_FIRST_NAME_FIRST;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.LAST_NAME;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.MIDDLE_NAME;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * @author Nikolina Špehar
 */
public class TextHelperGetFirstMiddleLastNameAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getFirstMiddleLastName")
        .title("Get First Middle and Last Name")
        .description("From full name extract first, middle and last name.")
        .properties(
            string(FULL_NAME)
                .label("Full Name")
                .description("The full name.")
                .required(true),
            bool(IS_FIRST_NAME_FIRST)
                .label("Is First Name First?")
                .description("Is the first name listed first in the full name?")
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        string(FIRST_NAME)
                            .description("First name."),
                        string(MIDDLE_NAME)
                            .description("Middle name"),
                        string(LAST_NAME)
                            .description("Last name"))))
        .help(
            "",
            "https://docs.bytechef.io/reference/components/text-helper_v1#get-first-middle-and-last-name")
        .perform(TextHelperGetFirstMiddleLastNameAction::perform);

    private TextHelperGetFirstMiddleLastNameAction() {
    }

    public static Map<String, String> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        String fullName = inputParameters.getRequiredString(FULL_NAME);
        String[] fullNameSplit = fullName.split(" ");

        boolean isFirstNameFirst = inputParameters.getRequiredBoolean(IS_FIRST_NAME_FIRST);

        if (fullNameSplit.length == 2) {
            return handleTwoPartName(fullNameSplit, isFirstNameFirst);
        } else {
            return handleThreePartName(fullNameSplit, isFirstNameFirst);
        }
    }

    private static Map<String, String> handleTwoPartName(String[] fullName, boolean isFirstNameFirst) {
        return Map.of(
            FIRST_NAME, isFirstNameFirst ? fullName[0] : fullName[1],
            MIDDLE_NAME, "",
            LAST_NAME, isFirstNameFirst ? fullName[1] : fullName[0]);
    }

    private static Map<String, String> handleThreePartName(String[] fullName, boolean isFirstNameFirst) {
        return Map.of(
            FIRST_NAME, isFirstNameFirst ? fullName[0] : fullName[2],
            MIDDLE_NAME, fullName[1],
            LAST_NAME, isFirstNameFirst ? fullName[2] : fullName[0]);
    }
}

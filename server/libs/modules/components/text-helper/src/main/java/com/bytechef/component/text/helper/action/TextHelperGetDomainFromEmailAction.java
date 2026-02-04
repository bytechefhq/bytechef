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
import static com.bytechef.component.text.helper.util.TextHelperUtils.extractByRegEx;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;

/**
 * @author Nikolina Špehar
 */
public class TextHelperGetDomainFromEmailAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getDomainFromEmail")
        .title("Get Domain From Email Address")
        .description("Extracts domain from the given email address.")
        .properties(
            string(TEXT)
                .description("The email you want to extract domain from.")
                .label("Email")
                .required(true))
        .output(outputSchema(string().description("Extracted domain")))
        .help("", "https://docs.bytechef.io/reference/components/text-helper_v1#get-domain-from-email")
        .perform(TextHelperGetDomainFromEmailAction::perform);

    private TextHelperGetDomainFromEmailAction() {
    }

    public static String perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        String regexStr = "([A-Za-z0-9.-]+\\.[A-Za-z]{2,})";
        String emailAddress = inputParameters.getRequiredString(TEXT);

        return extractByRegEx(emailAddress, regexStr).getFirst();
    }
}

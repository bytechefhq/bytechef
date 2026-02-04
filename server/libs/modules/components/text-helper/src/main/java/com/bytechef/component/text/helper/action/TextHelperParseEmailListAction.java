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
import static com.bytechef.component.text.helper.constant.TextHelperConstants.EMAIL;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.EMAILS;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.PARSED_EMAIL_OBJECT;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.text.helper.util.TextHelperUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Špehar
 */
public class TextHelperParseEmailListAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("parseEmailList")
        .title("Parse Email List")
        .description(
            "Parse emails into structured objects. For example: \"Name <name@domain.com>\" into {displayName: " +
                "'Name', localPart: 'name', domain: 'domain.com'}.")
        .properties(
            array(EMAILS)
                .label("Emails")
                .description("A list of emails that will be turned into structured data.")
                .required(true)
                .items(
                    string(EMAIL)
                        .label("Email")
                        .description("The email that will be turned into structured data.")
                        .required(true)))
        .output(
            outputSchema(
                array()
                    .description(
                        "List of parsed email objects. Each object contains the display name, local part, and " +
                            "domain of the email.")
                    .items(PARSED_EMAIL_OBJECT)))
        .perform(TextHelperParseEmailListAction::perform);

    private TextHelperParseEmailListAction() {
    }

    public static List<Map<String, String>> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        List<String> emailList = inputParameters.getRequiredList(EMAILS, String.class);

        return emailList.stream()
            .map(TextHelperUtils::parseEmail)
            .toList();
    }
}

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

package com.bytechef.component.loops.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.loops.constant.LoopsConstants.EMAIL;
import static com.bytechef.component.loops.constant.LoopsConstants.FIRST_NAME;
import static com.bytechef.component.loops.constant.LoopsConstants.LAST_NAME;
import static com.bytechef.component.loops.constant.LoopsConstants.MAILING_LISTS;
import static com.bytechef.component.loops.constant.LoopsConstants.USER_GROUP;
import static com.bytechef.component.loops.constant.LoopsConstants.USER_ID;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.loops.util.LoopsUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class LoopsCreateContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createContact")
        .title("Create Contact")
        .description("Create a new contact with an email address and any other contact properties.")
        .properties(
            string(EMAIL)
                .label("Email Address")
                .description("The contact’s email address.")
                .required(true),
            string(FIRST_NAME)
                .label("First Name")
                .description("The contact’s first name.")
                .required(false),
            string(LAST_NAME)
                .label("Last Name")
                .description("The contact’s last name.")
                .required(false),
            string(USER_GROUP)
                .label("User Group")
                .description(
                    "You can use groups to segment users when sending emails. Currently, a contact can only be in " +
                        "one user group. Groups like “Users”, “VIPs”, “Investors” or “Customers”")
                .required(false),
            string(USER_ID)
                .label("User ID")
                .description("A unique user ID (for example, from an external application).")
                .required(false),
            array(MAILING_LISTS)
                .label("Mailing Lists")
                .description("List of mailing lists the user will be subscribed to.")
                .items(string())
                .options((OptionsFunction<String>) LoopsUtils::getMailingListOptions)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        bool("success")
                            .description("Indicates whether the contact was created successfully."),
                        string("id")
                            .description("The internal ID of the new contact."))))
        .help("", "https://docs.bytechef.io/reference/components/loops_v1#create-contact")
        .perform(LoopsCreateContactAction::perform);

    private LoopsCreateContactAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(http -> http.post("/contacts/create"))
            .body(
                Body.of(
                    EMAIL, inputParameters.getRequiredString(EMAIL),
                    FIRST_NAME, inputParameters.getString(FIRST_NAME),
                    LAST_NAME, inputParameters.getString(LAST_NAME),
                    USER_GROUP, inputParameters.getString(USER_GROUP),
                    USER_ID, inputParameters.getString(USER_ID),
                    MAILING_LISTS, getMailingListObject(inputParameters.getList(MAILING_LISTS, String.class))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private static Map<String, Boolean> getMailingListObject(List<String> mailingLists) {
        Map<String, Boolean> mailingListObject = new HashMap<>();

        if (mailingLists == null) {
            return mailingListObject;
        }

        for (String mailingList : mailingLists) {
            mailingListObject.put(mailingList, true);
        }

        return mailingListObject;
    }
}

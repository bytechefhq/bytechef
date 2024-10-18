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

package com.bytechef.component.google.mail.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.CATEGORY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FROM;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.INCLUDE_SPAM_TRASH;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.LABEL;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.LABEL_IDS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.MAX_RESULTS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.MESSAGES;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.NEXT_PAGE_TOKEN;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.PAGE_TOKEN;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.RESULT_SIZE_ESTIMATE;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SUBJECT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.THREAD_ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.TO;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.mail.util.GoogleMailUtils;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import java.io.IOException;
import java.util.List;

/**
 * @author Monika Kušter
 */
public class GoogleMailSearchEmailAction {
    public static final ModifiableActionDefinition ACTION_DEFINITION = action("searchEmail")
        .title("Search Email")
        .description("Lists the messages in the user's mailbox.")
        .properties(
            number(MAX_RESULTS)
                .label("Max results")
                .description("Maximum number of messages to return.")
                .defaultValue(100)
                .maxValue(500)
                .required(false),
            string(PAGE_TOKEN)
                .label("Page token")
                .description("Page token to retrieve a specific page of results in the list.")
                .required(false),
            string(FROM)
                .label("From")
                .description("The address sending the mail")
                .required(false),
            string(TO)
                .label("To")
                .description("The address receiving the new mail")
                .required(false),
            string(SUBJECT)
                .label("Subject")
                .description("Words in the subject line")
                .required(false),
            string(CATEGORY)
                .label("Category")
                .description("Messages in a certain category")
                .options(
                    option("Primary", "primary"),
                    option("Social", "social"),
                    option("Promotions", "promotions"),
                    option("Updates", "updates"),
                    option("Forums", "forums"),
                    option("Reservations", "reservations"),
                    option("Purchases", "purchases"))
                .required(false),
            string(LABEL)
                .label("Label")
                .options((ActionOptionsFunction<String>) GoogleMailUtils::getLabelOptions)
                .required(false),
            array(LABEL_IDS)
                .label("Labels")
                .description(
                    "Only return messages with labels that match all of the specified label IDs. Messages in a " +
                        "thread might have labels that other messages in the same thread don't have.")
                .items(string())
                .required(false),
            bool(INCLUDE_SPAM_TRASH)
                .label("Include spam trash")
                .description("Include messages from SPAM and TRASH in the results.")
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        array(MESSAGES)
                            .items(
                                object()
                                    .properties(
                                        string(ID),
                                        string(THREAD_ID))),
                        string(NEXT_PAGE_TOKEN),
                        number(RESULT_SIZE_ESTIMATE))))
        .perform(GoogleMailSearchEmailAction::perform);

    private GoogleMailSearchEmailAction() {
    }

    public static ListMessagesResponse perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        Gmail service = GoogleServices.getMail(connectionParameters);

        StringBuilder query = createQuery(inputParameters);

        return service.users()
            .messages()
            .list(ME)
            .setMaxResults(inputParameters.getLong(MAX_RESULTS))
            .setPageToken(inputParameters.getString(PAGE_TOKEN))
            .setQ(query.toString())
            .setLabelIds(inputParameters.getList(LABEL_IDS, String.class, List.of()))
            .setIncludeSpamTrash(inputParameters.getBoolean(INCLUDE_SPAM_TRASH))
            .execute();
    }

    private static StringBuilder createQuery(Parameters inputParameters) {
        StringBuilder query = new StringBuilder();

        if (inputParameters.getString(FROM) != null) {
            query.append(" from:")
                .append(inputParameters.getString(FROM));
        }

        if (inputParameters.getString(TO) != null) {
            query.append(" to:")
                .append(inputParameters.getString(TO));
        }

        if (inputParameters.getString(SUBJECT) != null) {
            query.append(" subject:")
                .append(inputParameters.getString(SUBJECT));
        }

        if (inputParameters.getString(CATEGORY) != null) {
            query.append(" category:")
                .append(inputParameters.getString(CATEGORY));
        }

        if (inputParameters.getString(LABEL) != null) {
            query.append(" label:")
                .append(inputParameters.getString(LABEL));
        }

        return query;
    }
}

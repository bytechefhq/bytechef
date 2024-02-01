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

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.INCLUDE_SPAM_TRASH;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.LABEL_IDS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.MAX_RESULTS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.MESSAGES;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.MESSAGE_PROPERTY_FUNCTION;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.NEXT_PAGE_TOKEN;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.PAGE_TOKEN;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.Q;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.RESULT_SIZE_ESTIMATE;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SEARCH_EMAIL;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.mail.util.GoogleMailUtils;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class GoogleMailSearchEmailAction {
    public static final ModifiableActionDefinition ACTION_DEFINITION = action(SEARCH_EMAIL)
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
            string(Q)
                .label("Q")
                .description(
                    "Only return messages matching the specified query. Supports the same query format as the Gmail " +
                        "search box. For example, \"from:someuser@example.com rfc822msgid:<somemsgid@example.com> " +
                        "is:unread\". Parameter cannot be used when accessing the api using the gmail.metadata scope.")
                .required(false),
            array(LABEL_IDS)
                .label("Label IDs")
                .description(
                    "Only return messages with labels that match all of the specified label IDs. Messages in a " +
                        "thread might have labels that other messages in the same thread don't have.")
                .items(string())
                .required(false),
            bool(INCLUDE_SPAM_TRASH)
                .label("Include spam trash")
                .description("Include messages from SPAM and TRASH in the results.")
                .required(false))
        .outputSchema(
            object("result")
                .properties(
                    array(MESSAGES)
                        .items(MESSAGE_PROPERTY_FUNCTION.apply(null)),
                    string(NEXT_PAGE_TOKEN),
                    number(RESULT_SIZE_ESTIMATE)))
        .perform(GoogleMailSearchEmailAction::perform);

    private GoogleMailSearchEmailAction() {
    }

    public static Map<String, ListMessagesResponse> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) throws IOException {

        Gmail service = GoogleMailUtils.getMail(connectionParameters);

        return Map.of(
            "result",
            service.users()
                .messages()
                .list("me")
                .setMaxResults(inputParameters.getLong(MAX_RESULTS))
                .setPageToken(inputParameters.getString(PAGE_TOKEN))
                .setQ(inputParameters.getString(Q))
                .setLabelIds(inputParameters.getList(LABEL_IDS, String.class, List.of()))
                .setIncludeSpamTrash(inputParameters.getBoolean(INCLUDE_SPAM_TRASH))
                .execute());
    }
}

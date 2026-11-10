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

package com.bytechef.component.slack.action;

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.slack.constant.SlackConstants.COUNT;
import static com.bytechef.component.slack.constant.SlackConstants.ERROR;
import static com.bytechef.component.slack.constant.SlackConstants.ID;
import static com.bytechef.component.slack.constant.SlackConstants.NAME;
import static com.bytechef.component.slack.constant.SlackConstants.OK;
import static com.bytechef.component.slack.constant.SlackConstants.QUERY;
import static com.bytechef.component.slack.constant.SlackConstants.SORT;
import static com.bytechef.component.slack.constant.SlackConstants.SORT_DIRECTION;
import static com.bytechef.component.slack.constant.SlackConstants.TEXT;
import static com.bytechef.component.slack.constant.SlackConstants.TYPE;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class SlackSearchMessagesAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("searchMessages")
        .title("Search Messages")
        .description(
            "Searches for messages matching a query. Requires a user token with the search:read scope.")
        .properties(
            string(QUERY)
                .label("Query")
                .description(
                    "Search query. Supports Slack search modifiers such as in:#channel, from:@user, " +
                        "before:YYYY-MM-DD and after:YYYY-MM-DD.")
                .exampleValue("in:#general after:2025-01-01 report")
                .required(true),
            string(SORT)
                .label("Sort")
                .description("Field to sort the results by.")
                .options(
                    option("Score", "score"),
                    option("Timestamp", "timestamp"))
                .defaultValue("score")
                .required(false),
            string(SORT_DIRECTION)
                .label("Sort Direction")
                .description("Direction to sort the results in.")
                .options(
                    option("Ascending", "asc"),
                    option("Descending", "desc"))
                .defaultValue("desc")
                .required(false),
            integer(COUNT)
                .label("Count")
                .description("Number of results to return. Maximum is 100.")
                .defaultValue(20)
                .minValue(1)
                .maxValue(100)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        integer("total")
                            .description("Total number of messages matching the query."),
                        array("matches")
                            .description("Messages matching the query.")
                            .items(
                                object()
                                    .properties(
                                        object("channel")
                                            .description("Channel where the message was posted.")
                                            .properties(
                                                string(ID)
                                                    .description("ID of the channel."),
                                                string(NAME)
                                                    .description("Name of the channel.")),
                                        string("iid")
                                            .description("Internal ID of the search result."),
                                        string("permalink")
                                            .description("Permanent link to the message."),
                                        string("team")
                                            .description("ID of the workspace."),
                                        string(TEXT)
                                            .description("Text of the message."),
                                        string("ts")
                                            .description("Timestamp of the message."),
                                        string(TYPE)
                                            .description("Type of the message."),
                                        string("user")
                                            .description("ID of the user who posted the message."),
                                        string("username")
                                            .description("Username of the user who posted the message."))),
                        object("pagination")
                            .description("Pagination details of the results.")
                            .properties(
                                integer("first"),
                                integer("last"),
                                integer("page"),
                                integer("page_count"),
                                integer("per_page"),
                                integer("total_count")),
                        object("paging")
                            .description("Paging details of the results.")
                            .properties(
                                integer(COUNT),
                                integer("page"),
                                integer("pages"),
                                integer("total")))))
        .help("", "https://docs.bytechef.io/reference/components/slack_v1#search-messages")
        .perform(SlackSearchMessagesAction::perform);

    private SlackSearchMessagesAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, Object> body = context
            .http(http -> http.get("/search.messages"))
            .queryParameters(
                QUERY, inputParameters.getRequiredString(QUERY),
                SORT, inputParameters.getString(SORT),
                "sort_dir", inputParameters.getString(SORT_DIRECTION),
                COUNT, inputParameters.getInteger(COUNT))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if ((boolean) body.get(OK)) {
            return body.get("messages");
        } else {
            throw new ProviderException((String) body.get(ERROR));
        }
    }
}

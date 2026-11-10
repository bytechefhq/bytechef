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

package com.bytechef.component.sentry.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Ivica Cardic
 */
public class SentryConstants {

    public static final String ASSIGNED_TO = "assignedTo";
    public static final String BASE_URL = "baseUrl";
    public static final String DEFAULT_BASE_URL = "https://sentry.io";
    public static final String ID = "id";
    public static final String ISSUE_ID = "issueId";
    public static final String LIMIT = "limit";
    public static final String NAME = "name";
    public static final String ORGANIZATION = "organization";
    public static final String PROJECT = "project";
    public static final String QUERY = "query";
    public static final String SLUG = "slug";
    public static final String STATS_PERIOD = "statsPeriod";
    public static final String STATUS = "status";

    public static final ModifiableObjectProperty ISSUE_OUTPUT_PROPERTY = object()
        .properties(
            string(ID)
                .description("The ID of the issue."),
            string("shortId")
                .description("The short ID of the issue, for example PROJECT-1A."),
            string("title")
                .description("The title of the issue."),
            string("culprit")
                .description("The location in code where the issue originated."),
            string("permalink")
                .description("The URL of the issue in Sentry."),
            string("level")
                .description("The level of the issue, for example error or warning."),
            string(STATUS)
                .description("The status of the issue: resolved, unresolved or ignored."),
            string("substatus")
                .description("The substatus of the issue, for example ongoing, new or regressed."),
            string("priority")
                .description("The priority of the issue: low, medium or high."),
            string("platform")
                .description("The platform of the issue."),
            string("type")
                .description("The type of the issue."),
            string("count")
                .description("The number of events of the issue."),
            string("userCount")
                .description("The number of users affected by the issue."),
            string("firstSeen")
                .description("ISO 8601 timestamp for when the issue was first seen."),
            string("lastSeen")
                .description("ISO 8601 timestamp for when the issue was last seen."),
            bool("isBookmarked")
                .description("Whether the issue is bookmarked by the requester."),
            bool("hasSeen")
                .description("Whether the requester has seen the issue."),
            object(ASSIGNED_TO)
                .description("The user or team the issue is assigned to.")
                .properties(
                    string(ID),
                    string(NAME),
                    string("type")),
            object(PROJECT)
                .description("The project the issue belongs to.")
                .properties(
                    string(ID),
                    string(NAME),
                    string(SLUG),
                    string("platform")),
            object("metadata")
                .description("The error type, value and location of the issue.")
                .properties(
                    string("type"),
                    string("value"),
                    string("filename"),
                    string("function")));

    public static final ModifiableObjectProperty EVENT_OUTPUT_PROPERTY = object()
        .properties(
            string(ID)
                .description("The ID of the event."),
            string("eventID")
                .description("The ID of the event."),
            string("groupID")
                .description("The ID of the issue the event belongs to."),
            string("projectID")
                .description("The ID of the project the event belongs to."),
            string("title")
                .description("The title of the event."),
            string("message")
                .description("The message of the event."),
            string("culprit")
                .description("The location in code where the event originated."),
            string("location")
                .description("The location of the event."),
            string("platform")
                .description("The platform of the event."),
            string("type")
                .description("The type of the event."),
            string("dateCreated")
                .description("ISO 8601 timestamp for when the event was created."),
            string("dateReceived")
                .description("ISO 8601 timestamp for when Sentry received the event."),
            object("metadata")
                .description("The error type and value of the event.")
                .properties(
                    string("type"),
                    string("value")),
            array("tags")
                .description("The tags of the event, such as browser, environment and level.")
                .items(
                    object()
                        .properties(
                            string("key"),
                            string("value"))),
            object("user")
                .description("The user the event was captured for.")
                .properties(
                    string(ID),
                    string("email"),
                    string("username"),
                    string("ip_address")),
            array("entries")
                .description("The event data, such as exception stack traces, breadcrumbs and request details.")
                .items(
                    object()
                        .properties(
                            string("type"),
                            object("data"))),
            object("contexts")
                .description("The context of the event, such as browser, OS and runtime."),
            string("nextEventID")
                .description("The ID of the next event of the issue."),
            string("previousEventID")
                .description("The ID of the previous event of the issue."));

    private SentryConstants() {
    }
}

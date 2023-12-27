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

package com.bytechef.component.slack.properties;

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.definition.ComponentDSL;

/**
 * @author Mario Cvjetojevic
 */
public final class SlackOutputProperties {
    private static final String OK = "ok";
    private static final String WARNING = "warning";
    private static final String ERROR = "error";
    private static final String NEEDED = "needed";
    private static final String PROVIDED = "provided";
    private static final String HTTP_RESPONSE_HEADERS = "httpResponseHeaders";
    private static final String DEPRECATED_ARGUMENT = "deprecatedArgument";
    private static final String ERRORS = "errors";
    private static final String RESPONSE_METADATA = "responseMetadata";
    private static final String CHANNEL = "channel";
    private static final String TS = "ts";
    private static final String MESSAGE = "message";
    private static final String MESSAGES = "messages";
    private static final String TYPE = "type";
    private static final String SUBTYPE = "subtype";
    private static final String TEAM = "team";
    private static final String USER = "user";
    private static final String USERNAME = "username";
    private static final String TEXT = "text";
    private static final String THREAD_TS = "threadTs";

    public static final ComponentDSL.ModifiableObjectProperty CHAT_POST_MESSAGE_RESPONSE_PROPERTY = object()
        .properties(
            bool(OK),
            string(WARNING),
            string(NEEDED),
            string(PROVIDED),
            array(HTTP_RESPONSE_HEADERS),
            string(DEPRECATED_ARGUMENT),
            array(ERRORS)
                .items(
                    string(ERROR)),
            object(RESPONSE_METADATA)
                .properties(
                    array(MESSAGES)
                        .items(
                            string(MESSAGE))),
            string(CHANNEL),
            string(TS),
            object(MESSAGE)
                .properties(
                    string(TYPE),
                    string(SUBTYPE),
                    string(TEAM),
                    string(CHANNEL),
                    string(USER),
                    string(USERNAME),
                    string(TEXT),
                    string(TS),
                    string(THREAD_TS)));
}

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

package com.bytechef.component.infobip.constant;

import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Monika Kušter
 */
public class InfobipConstants {

    private InfobipConstants() {
    }

    public static final String BASE_URL = "baseUrl";
    public static final String BULK_ID = "bulkId";
    public static final String CONFIGURATION_KEY = "configurationKey";
    public static final String CONTENT = "content";
    public static final String DESCRIPTION = "description";
    public static final String DESTINATIONS = "destinations";
    public static final String FROM = "from";
    public static final String GROUP_ID = "groupId";
    public static final String GROUP_NAME = "groupName";
    public static final String ID = "id";
    public static final String KEYWORD = "keyword";
    public static final String LANGUAGE = "language";
    public static final String MESSAGES = "messages";
    public static final String MESSAGE_COUNT = "messageCount";
    public static final String MESSAGE_ID = "messageId";
    public static final String NAME = "name";
    public static final String NUMBER = "number";
    public static final String PLACEHOLDERS = "placeholders";
    public static final String SENDER = "sender";
    public static final String STATUS = "status";
    public static final String TEMPLATE_NAME = "templateName";
    public static final String TEXT = "text";
    public static final String TO = "to";

    public static final ModifiableObjectProperty WHATSAPP_MESSAGE_OUTPUT_PROPERTY = object()
        .properties(
            string(TO)
                .description("The destination address of the message."),
            integer(MESSAGE_COUNT)
                .description("Number of messages required to deliver."),
            string(MESSAGE_ID)
                .description("ID of the message sent."),
            object(STATUS)
                .description("Status of the message.")
                .properties(
                    integer(GROUP_ID)
                        .description("Status group ID."),
                    string(GROUP_NAME)
                        .description("Status group name."),
                    integer(ID)
                        .description("Status ID."),
                    string(NAME)
                        .description("Status name."),
                    string(DESCRIPTION)
                        .description("Human-readable description of the status."),
                    string("action")
                        .description("Action that should be taken to eliminate error.")));
}

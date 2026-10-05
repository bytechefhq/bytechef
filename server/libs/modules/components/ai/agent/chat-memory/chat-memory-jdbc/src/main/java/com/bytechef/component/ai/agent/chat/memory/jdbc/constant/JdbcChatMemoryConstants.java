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

package com.bytechef.component.ai.agent.chat.memory.jdbc.constant;

import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Property;

/**
 * @author Ivica Cardic
 */
public class JdbcChatMemoryConstants {

    public static final String CONVERSATION_ID = "conversationId";
    public static final String DATABASE = "database";
    public static final String DATABASE_TYPE = "databaseType";
    public static final String DEFAULT_TABLE_NAME = "SPRING_AI_CHAT_MEMORY";
    public static final String HOST = "host";
    public static final String MESSAGES = "messages";
    public static final String MESSAGE_CONTENT = "content";
    public static final String MESSAGE_ROLE = "role";
    public static final String PASSWORD = "password";
    public static final String PORT = "port";
    public static final String SCHEMA = "schema";
    public static final String SERVICE_NAME = "serviceName";
    public static final String TABLE = "table";
    public static final String USERNAME = "username";

    public static final Property SCHEMA_PROPERTY = string(SCHEMA)
        .label("Schema")
        .description(
            "The database schema containing the chat memory table. If empty, the connection's default schema is used.")
        .required(false);

    public static final Property TABLE_PROPERTY = string(TABLE)
        .label("Table")
        .description("The name of the table storing chat memory messages. Created if it does not exist.")
        .defaultValue(DEFAULT_TABLE_NAME)
        .required(false);
}

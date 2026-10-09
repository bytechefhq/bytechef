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

package com.bytechef.component.ai.agent.chat.memory.redis.connection;

import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.DEFAULT_KEY_PREFIX;
import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.HOST;
import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.KEY_PREFIX;
import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.PASSWORD;
import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.PORT;
import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.TIME_TO_LIVE;
import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import com.bytechef.component.definition.Property.ControlType;

/**
 * @author Ivica Cardic
 */
public final class RedisChatMemoryConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .properties(
            string(HOST)
                .label("Host")
                .description("The Redis server host.")
                .defaultValue("localhost")
                .required(true),
            integer(PORT)
                .label("Port")
                .description("The Redis server port.")
                .defaultValue(6379)
                .required(true),
            string(KEY_PREFIX)
                .label("Key Prefix")
                .description("The prefix for Redis keys used to store chat messages.")
                .defaultValue(DEFAULT_KEY_PREFIX)
                .required(false),
            string(TIME_TO_LIVE)
                .label("Time to Live")
                .description(
                    "The time-to-live for chat messages (e.g., '24h', '7d', '30m'). Leave empty for no expiration. " +
                        "Applies to version 1 only.")
                .required(false))
        .authorizations(
            authorization(AuthorizationType.CUSTOM)
                .properties(
                    string(USERNAME)
                        .label("Username")
                        .description("The Redis username (optional, for Redis 6.0+ ACL).")
                        .required(false),
                    string(PASSWORD)
                        .label("Password")
                        .description("The Redis password.")
                        .controlType(ControlType.PASSWORD)
                        .required(false)));

    private RedisChatMemoryConnection() {
    }
}

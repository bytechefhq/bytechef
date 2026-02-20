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

package com.bytechef.component.redis.connection;

import static com.bytechef.component.definition.Authorization.AuthorizationType.CUSTOM;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.redis.constant.RedisConstants.DATABASE;
import static com.bytechef.component.redis.constant.RedisConstants.HOST;
import static com.bytechef.component.redis.constant.RedisConstants.PASSWORD;
import static com.bytechef.component.redis.constant.RedisConstants.PORT;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

/**
 * @author Ivica Cardic
 */
public class RedisConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(CUSTOM)
                .properties(
                    string(HOST)
                        .label("Host")
                        .description("The Redis server hostname or IP address.")
                        .defaultValue("localhost")
                        .required(true),
                    integer(PORT)
                        .label("Port")
                        .description("The Redis server port.")
                        .defaultValue(6379)
                        .required(true),
                    string(PASSWORD)
                        .label("Password")
                        .description("The password for Redis authentication. Leave empty if no authentication.")
                        .required(false),
                    integer(DATABASE)
                        .label("Database")
                        .description("The Redis database index (0-15).")
                        .defaultValue(0)
                        .required(false)));

    private RedisConnection() {
    }
}

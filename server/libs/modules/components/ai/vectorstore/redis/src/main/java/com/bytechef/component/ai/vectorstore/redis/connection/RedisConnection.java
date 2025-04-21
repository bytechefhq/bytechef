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

package com.bytechef.component.ai.vectorstore.redis.connection;

import static com.bytechef.component.ai.vectorstore.redis.constant.RedisConstants.INITIALIZE_SCHEMA;
import static com.bytechef.component.ai.vectorstore.redis.constant.RedisConstants.PUBLIC_ENDPOINT;
import static com.bytechef.component.definition.Authorization.AuthorizationType.CUSTOM;
import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

/**
 * @author Monika Kušter
 */
public class RedisConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(CUSTOM)
                .properties(
                    string(PUBLIC_ENDPOINT)
                        .label("Public Endpoint")
                        .exampleValue("redis-13130.c15.us-east-1-4.ec2.redns.redis-cloud.com:13130")
                        .required(true),
                    string(USERNAME)
                        .label("Username")
                        .defaultValue("default")
                        .required(true),
                    string(PASSWORD)
                        .label("Password")
                        .required(true),
                    bool(INITIALIZE_SCHEMA)
                        .label("Initialize Schema")
                        .description("Whether to initialize the schema.")
                        .defaultValue(false)
                        .required(true)));

    private RedisConnection() {
    }
}

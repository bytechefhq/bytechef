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

package com.bytechef.component.ai.vectorstore.milvus.connection;

import static com.bytechef.component.ai.vectorstore.milvus.constant.MilvusConstants.COLLECTION;
import static com.bytechef.component.ai.vectorstore.milvus.constant.MilvusConstants.DATABASE;
import static com.bytechef.component.ai.vectorstore.milvus.constant.MilvusConstants.HOST;
import static com.bytechef.component.ai.vectorstore.milvus.constant.MilvusConstants.INITIALIZE_SCHEMA;
import static com.bytechef.component.ai.vectorstore.milvus.constant.MilvusConstants.PORT;
import static com.bytechef.component.ai.vectorstore.milvus.constant.MilvusConstants.URI;
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
public class MilvusConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(CUSTOM)
                .properties(
                    string(HOST)
                        .label("Host")
                        .description("The name or address of the host.")
                        .defaultValue("localhost")
                        .required(true),
                    string(PORT)
                        .label("Port")
                        .defaultValue("19530")
                        .description("The connection port.")
                        .required(true),
                    string(URI)
                        .label("Uri")
                        .description("The uri of Milvus instance.")
                        .required(true),
                    string(USERNAME)
                        .label("Username")
                        .description("The username for this connection.")
                        .defaultValue("root")
                        .required(true),
                    string(PASSWORD)
                        .label("Password")
                        .description("The password for this connection.")
                        .defaultValue("milvus")
                        .required(true),
                    string(COLLECTION)
                        .label("Collection Name")
                        .description("Milvus collection name to use.")
                        .defaultValue("vector_store")
                        .required(true),
                    string(DATABASE)
                        .label("Database Name")
                        .description("The name of the Milvus database to use.")
                        .defaultValue("default")
                        .required(true),
                    bool(INITIALIZE_SCHEMA)
                        .label("Initialize Schema")
                        .description("Whether to initialize the schema.")
                        .defaultValue(false)
                        .required(true)));

    private MilvusConnection() {
    }
}

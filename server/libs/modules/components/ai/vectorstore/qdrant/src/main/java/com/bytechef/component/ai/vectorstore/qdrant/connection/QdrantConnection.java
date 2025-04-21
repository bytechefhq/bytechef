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

package com.bytechef.component.ai.vectorstore.qdrant.connection;

import static com.bytechef.component.ai.vectorstore.qdrant.constant.QdrantConstants.API_KEY;
import static com.bytechef.component.ai.vectorstore.qdrant.constant.QdrantConstants.COLLECTION;
import static com.bytechef.component.ai.vectorstore.qdrant.constant.QdrantConstants.HOST;
import static com.bytechef.component.ai.vectorstore.qdrant.constant.QdrantConstants.INITIALIZE_SCHEMA;
import static com.bytechef.component.ai.vectorstore.qdrant.constant.QdrantConstants.PORT;
import static com.bytechef.component.definition.Authorization.AuthorizationType.CUSTOM;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

/**
 * @author Monika Kušter
 */
public class QdrantConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(CUSTOM)
                .properties(
                    string(HOST)
                        .label("Host")
                        .description("The host of the Qdrant server.")
                        .defaultValue("localhost")
                        .required(true),
                    integer(PORT)
                        .label("Port")
                        .description("The gRPC port of the Qdrant server.")
                        .defaultValue(6334)
                        .required(true),
                    string(API_KEY)
                        .label("API Key")
                        .description("The API key to use for authentication withe the server.")
                        .required(true),
                    string(COLLECTION)
                        .label("Collection Name")
                        .description("The name of the collection to use.")
                        .defaultValue("vector_store")
                        .required(true),
                    bool(INITIALIZE_SCHEMA)
                        .label("Initialize Schema")
                        .description("Whether to initialize the schema.")
                        .defaultValue(false)
                        .required(true)));

    private QdrantConnection() {
    }
}

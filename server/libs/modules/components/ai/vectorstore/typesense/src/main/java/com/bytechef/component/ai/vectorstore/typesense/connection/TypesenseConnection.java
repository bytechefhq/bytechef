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

package com.bytechef.component.ai.vectorstore.typesense.connection;

import static com.bytechef.component.ai.vectorstore.typesense.constant.TypesenseConstants.API_KEY;
import static com.bytechef.component.ai.vectorstore.typesense.constant.TypesenseConstants.COLLECTION;
import static com.bytechef.component.ai.vectorstore.typesense.constant.TypesenseConstants.EMBEDDING_DIMENSION;
import static com.bytechef.component.ai.vectorstore.typesense.constant.TypesenseConstants.HOST;
import static com.bytechef.component.ai.vectorstore.typesense.constant.TypesenseConstants.INITIALIZE_SCHEMA;
import static com.bytechef.component.ai.vectorstore.typesense.constant.TypesenseConstants.PORT;
import static com.bytechef.component.ai.vectorstore.typesense.constant.TypesenseConstants.PROTOCOL;
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
public class TypesenseConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(CUSTOM)
                .properties(
                    string(PROTOCOL)
                        .label("Protocol")
                        .description("HTTP Protocol")
                        .defaultValue("http")
                        .required(true),
                    string(HOST)
                        .label("Host")
                        .description("Hostname")
                        .defaultValue("localhost")
                        .required(true),
                    string(PORT)
                        .label("Port")
                        .defaultValue("8108")
                        .required(true),
                    string(API_KEY)
                        .label("Typesense API Key")
                        .description("The API key for the Typesense API.")
                        .required(true),
                    string(COLLECTION)
                        .label("Collection Name")
                        .description("The name of the collection to use.")
                        .defaultValue("vector_store")
                        .required(true),
                    integer(EMBEDDING_DIMENSION)
                        .label("Embedding Dimension")
                        .description("The dimension of the embeddings.")
                        .defaultValue(1536)
                        .required(true),
                    bool(INITIALIZE_SCHEMA)
                        .label("Initialize Schema")
                        .description("Whether to initialize the schema.")
                        .defaultValue(false)
                        .required(true)));

    private TypesenseConnection() {
    }
}

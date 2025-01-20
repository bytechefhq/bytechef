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

package com.bytechef.component.ai.vectorstore.pinecone.connection;

import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.EMBEDDING_API_KEY_PROPERTY;
import static com.bytechef.component.ai.vectorstore.pinecone.constant.PineconeConstants.API_KEY;
import static com.bytechef.component.ai.vectorstore.pinecone.constant.PineconeConstants.HOST;
import static com.bytechef.component.definition.Authorization.AuthorizationType.CUSTOM;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

/**
 * @author Monika Kušter
 */
public class PineconeConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(CUSTOM)
                .properties(
                    EMBEDDING_API_KEY_PROPERTY,
                    string(API_KEY)
                        .label("Pinecone API Key")
                        .description("The API key for the Pinecone API.")
                        .required(true),
                    string(HOST)
                        .label("Host")
                        .description("Url of the host.")
                        .exampleValue("https://indexname-pr0j1d7.svc.aped-1234-a12b.pinecone.io")
                        .required(true)));

    private PineconeConnection() {
    }
}

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

package com.bytechef.component.pinecone.connection;

import static com.bytechef.component.definition.Authorization.AuthorizationType.CUSTOM;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.pinecone.constant.PineconeConstants.API_KEY;
import static com.bytechef.component.pinecone.constant.PineconeConstants.EMBEDDING_API_KEY;
import static com.bytechef.component.pinecone.constant.PineconeConstants.ENVIRONMENT;
import static com.bytechef.component.pinecone.constant.PineconeConstants.INDEX_NAME;
import static com.bytechef.component.pinecone.constant.PineconeConstants.PROJECT_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

/**
 * @author Monika Kušter
 */
public class PineconeConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(CUSTOM)
                .properties(
                    string(EMBEDDING_API_KEY)
                        .label("Open AI API Key")
                        .description("The API key for the OpenAI API which is used to generate embeddings.")
                        .required(true),
                    string(API_KEY)
                        .label("Pinecone API Key")
                        .description("The API key for the Pinecone API.")
                        .required(true),
                    string(ENVIRONMENT)
                        .label("Environment")
                        .description("Pinecone environment.")
                        .defaultValue("gcp-starter")
                        .required(true),
                    string(PROJECT_ID)
                        .label("Project ID")
                        .description("Pinecone project ID.")
                        .required(true),
                    string(INDEX_NAME)
                        .label("Index Name")
                        .description("Pinecone index name.")
                        .required(true)));

    private PineconeConnection() {
    }
}

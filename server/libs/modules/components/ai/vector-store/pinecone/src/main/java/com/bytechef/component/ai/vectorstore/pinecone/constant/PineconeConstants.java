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

package com.bytechef.component.ai.vectorstore.pinecone.constant;

import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.EMBEDDING_API_KEY;

import com.bytechef.component.ai.vectorstore.VectorStore;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.pinecone.PineconeVectorStore;

/**
 * @author Monika Kušter
 */
public class PineconeConstants {

    private PineconeConstants() {
    }

    public static final String API_KEY = "apiKey";
    public static final String ENVIRONMENT = "environment";
    public static final String INDEX_NAME = "indexName";
    public static final String PROJECT_ID = "projectId";
    public static final VectorStore VECTOR_STORE = connectionParameters -> {
        OpenAiEmbeddingModel openAiEmbeddingModel = new OpenAiEmbeddingModel(
            new OpenAiApi(connectionParameters.getRequiredString(EMBEDDING_API_KEY)));

        return PineconeVectorStore
            .builder(
                openAiEmbeddingModel, connectionParameters.getRequiredString(API_KEY),
                connectionParameters.getRequiredString(PROJECT_ID), connectionParameters.getRequiredString(ENVIRONMENT),
                connectionParameters.getRequiredString(INDEX_NAME))
            .build();
    };
}

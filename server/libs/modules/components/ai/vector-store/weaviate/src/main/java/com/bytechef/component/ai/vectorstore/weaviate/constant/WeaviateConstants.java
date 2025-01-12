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

package com.bytechef.component.ai.vectorstore.weaviate.constant;

import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.EMBEDDING_API_KEY;

import com.bytechef.component.ai.vectorstore.VectorStore;
import io.weaviate.client.Config;
import io.weaviate.client.WeaviateAuthClient;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.v1.auth.exception.AuthException;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.weaviate.WeaviateVectorStore;

/**
 * @author Monika Kušter
 */
public class WeaviateConstants {

    private WeaviateConstants() {
    }

    public static final String API_KEY = "apiKey";
    public static final String HOST = "host";
    public static final String SCHEME = "scheme";

    public static final VectorStore VECTOR_STORE = connectionParameters -> {
        OpenAiEmbeddingModel openAiEmbeddingModel = new OpenAiEmbeddingModel(
            new OpenAiApi(connectionParameters.getRequiredString(EMBEDDING_API_KEY)));

        Config config = new Config(
            connectionParameters.getRequiredString(SCHEME), connectionParameters.getRequiredString(HOST));

        WeaviateClient weaviateClient;

        try {
            weaviateClient = WeaviateAuthClient.apiKey(config, connectionParameters.getRequiredString(API_KEY));
        } catch (AuthException authException) {
            throw new RuntimeException("Authentication failed", authException);
        }

        return WeaviateVectorStore.builder(weaviateClient, openAiEmbeddingModel)
            .build();
    };
}

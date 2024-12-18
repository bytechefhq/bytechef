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

package com.bytechef.component.ai.vectorstore.weaviate.action;

import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.DATA_QUERY;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.EMBEDDING_API_KEY;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.QUERY;
import static com.bytechef.component.ai.vectorstore.weaviate.constant.WeaviateConstants.API_KEY;
import static com.bytechef.component.ai.vectorstore.weaviate.constant.WeaviateConstants.HOST;
import static com.bytechef.component.ai.vectorstore.weaviate.constant.WeaviateConstants.SCHEME;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.ai.vectorstore.VectorStore;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import io.weaviate.client.Config;
import io.weaviate.client.WeaviateAuthClient;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.v1.auth.exception.AuthException;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.WeaviateVectorStore;
import org.springframework.ai.vectorstore.WeaviateVectorStore.WeaviateVectorStoreConfig;

/**
 * @author Monika Kušter
 */
public class WeaviateDataQueryAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(DATA_QUERY)
        .title("Data Query")
        .description("Query data from a Weaviate vector store using OpenAI embeddings.")
        .properties(
            string(QUERY)
                .label("Query")
                .description("The query to be executed.")
                .required(true))
        .output()
        .perform(WeaviateDataQueryAction::perform);

    private WeaviateDataQueryAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        return VECTOR_STORE.query(inputParameters, connectionParameters);
    }

    public static final VectorStore VECTOR_STORE = connectionParameters -> {
        OpenAiEmbeddingModel openAiEmbeddingModel = new OpenAiEmbeddingModel(
            new OpenAiApi(connectionParameters.getRequiredString(EMBEDDING_API_KEY)));

        Config config =
            new Config(connectionParameters.getRequiredString(SCHEME), connectionParameters.getRequiredString(HOST));

        try {
            WeaviateClient weaviateClient =
                WeaviateAuthClient.apiKey(config, connectionParameters.getRequiredString(API_KEY));

            WeaviateVectorStoreConfig weaviateVectorStoreConfig = WeaviateVectorStoreConfig.builder()
                .build();

            return new WeaviateVectorStore(weaviateVectorStoreConfig, openAiEmbeddingModel, weaviateClient);
        } catch (AuthException authException) {
            throw new RuntimeException("Authetication failed", authException);
        }
    };
}

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

package com.bytechef.component.ai.vectorstore.pinecone.action;

import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.DOCUMENT_PROPERTY;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.DOCUMENT_TYPE_PROPERTY;
import static com.bytechef.component.ai.vectorstore.constant.VectorStoreConstants.JSON_KEYS_TO_USE_PROPERTY;
import static com.bytechef.component.ai.vectorstore.pinecone.constant.PineconeConstants.API_KEY;
import static com.bytechef.component.ai.vectorstore.pinecone.constant.PineconeConstants.EMBEDDING_API_KEY;
import static com.bytechef.component.ai.vectorstore.pinecone.constant.PineconeConstants.ENVIRONMENT;
import static com.bytechef.component.ai.vectorstore.pinecone.constant.PineconeConstants.INDEX_NAME;
import static com.bytechef.component.ai.vectorstore.pinecone.constant.PineconeConstants.PROJECT_ID;
import static com.bytechef.component.definition.ComponentDsl.action;

import com.bytechef.component.ai.vectorstore.VectorStore;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.PineconeVectorStore;
import org.springframework.ai.vectorstore.PineconeVectorStore.PineconeVectorStoreConfig;

/**
 * @author Monika Kušter
 */
public class PineconeLoadDataAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("loadData")
        .title("Load Data")
        .description("Loads data into a Pinecone vector store using OpenAI embeddings.")
        .properties(
            DOCUMENT_TYPE_PROPERTY,
            JSON_KEYS_TO_USE_PROPERTY,
            DOCUMENT_PROPERTY)
        .perform(PineconeLoadDataAction::perform);

    private PineconeLoadDataAction() {
    }

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        VECTOR_STORE.loadData(inputParameters, connectionParameters, actionContext);

        return null;
    }

    public static final VectorStore VECTOR_STORE = connectionParameters -> {
        OpenAiEmbeddingModel openAiEmbeddingModel = new OpenAiEmbeddingModel(
            new OpenAiApi(connectionParameters.getRequiredString(EMBEDDING_API_KEY)));

        PineconeVectorStoreConfig pineconeVectorStoreConfig = PineconeVectorStoreConfig.builder()
            .withApiKey(connectionParameters.getRequiredString(API_KEY))
            .withEnvironment(connectionParameters.getRequiredString(ENVIRONMENT))
            .withProjectId(connectionParameters.getRequiredString(PROJECT_ID))
            .withIndexName(connectionParameters.getRequiredString(INDEX_NAME))
            .build();

        return new PineconeVectorStore(pineconeVectorStoreConfig, openAiEmbeddingModel);
    };
}

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

package com.bytechef.component.ai.vectorstore.qdrant.constant;

import com.bytechef.component.ai.vectorstore.VectorStore;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;

/**
 * @author Monika Kušter
 */
public class QdrantConstants {

    public static final String API_KEY = "apiKey";
    public static final String COLLECTION = "collection";
    public static final String HOST = "host";
    public static final String INITIALIZE_SCHEMA = "initializeSchema";
    public static final String PORT = "port";
    public static final String QDRANT = "qdrant";

    public static final VectorStore VECTOR_STORE = (connectionParameters, embeddingModel) -> {
        QdrantClient qdrantClient = new QdrantClient(
            QdrantGrpcClient.newBuilder(
                connectionParameters.getRequiredString(HOST),
                connectionParameters.getRequiredInteger(PORT))
                .withApiKey(connectionParameters.getRequiredString(API_KEY))
                .build());

        return QdrantVectorStore.builder(qdrantClient, embeddingModel)
            .collectionName(connectionParameters.getRequiredString(COLLECTION))
            .initializeSchema(connectionParameters.getRequiredBoolean(INITIALIZE_SCHEMA))
            .build();
    };

    private QdrantConstants() {
    }
}

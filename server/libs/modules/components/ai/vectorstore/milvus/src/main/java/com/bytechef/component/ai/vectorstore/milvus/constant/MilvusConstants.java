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

package com.bytechef.component.ai.vectorstore.milvus.constant;

import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;

import com.bytechef.component.ai.vectorstore.VectorStore;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;

/**
 * @author Monika Kušter
 */
public class MilvusConstants {

    public static final String COLLECTION = "collection";
    public static final String DATABASE = "database";
    public static final String HOST = "host";
    public static final String INITIALIZE_SCHEMA = "initializeSchema";
    public static final String MILVUS = "milvus";
    public static final String PORT = "port";
    public static final String URI = "uri";

    public static final VectorStore VECTOR_STORE = (connectionParameters, embeddingModel) -> {
        MilvusServiceClient milvusServiceClient = new MilvusServiceClient(ConnectParam.newBuilder()
            .withAuthorization(connectionParameters.getRequiredString(USERNAME),
                connectionParameters.getRequiredString(PASSWORD))
            .withUri(connectionParameters.getRequiredString(URI))
            .build());

        return MilvusVectorStore.builder(milvusServiceClient, embeddingModel)
            .collectionName(connectionParameters.getRequiredString(COLLECTION))
            .databaseName(connectionParameters.getRequiredString(DATABASE))
            .initializeSchema(connectionParameters.getRequiredBoolean(INITIALIZE_SCHEMA))
            .build();
    };

    private MilvusConstants() {
    }
}

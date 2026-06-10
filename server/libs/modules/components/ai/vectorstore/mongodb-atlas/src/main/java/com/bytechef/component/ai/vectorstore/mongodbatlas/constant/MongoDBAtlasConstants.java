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

package com.bytechef.component.ai.vectorstore.mongodbatlas.constant;

import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;

import com.bytechef.component.ai.vectorstore.VectorStore;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.MongoDriverInformation;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.ai.vectorstore.mongodb.atlas.MongoDBAtlasVectorStore;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

public class MongoDBAtlasConstants {

    public static final String COLLECTION_NAME = "collectionName";
    public static final String CONNECTION_STRING = "connectionString";
    public static final String DATABASE_NAME = "databaseName";
    public static final String INDEX_NAME = "indexName";
    public static final String INITIALIZE_SCHEMA = "initializeSchema";
    public static final String MONGODB_ATLAS = "mongodbAtlas";
    public static final String NUM_CANDIDATES = "numCandidates";
    public static final String PATH_NAME = "pathName";

    private static final MongoDriverInformation DRIVER_INFORMATION = MongoDriverInformation.builder()
        .driverName("ByteChef")
        .build();

    public static final VectorStore VECTOR_STORE = (inputParameters, connectionParameters, embeddingModel) -> {
        ConnectionString connectionString = new ConnectionString(
            connectionParameters.getRequiredString(CONNECTION_STRING));

        MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
            .applyConnectionString(connectionString);

        String username = connectionParameters.getString(USERNAME);
        String password = connectionParameters.getString(PASSWORD);
        String databaseName = connectionParameters.getRequiredString(DATABASE_NAME);

        if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            settingsBuilder.credential(
                MongoCredential.createCredential(username, databaseName, password.toCharArray()));
        }

        MongoClient mongoClient = MongoClients.create(settingsBuilder.build(), DRIVER_INFORMATION);

        MongoTemplate mongoTemplate = new MongoTemplate(
            new SimpleMongoClientDatabaseFactory(mongoClient, databaseName));

        MongoDBAtlasVectorStore vectorStore = MongoDBAtlasVectorStore.builder(mongoTemplate, embeddingModel)
            .collectionName(connectionParameters.getString(COLLECTION_NAME, "vector_store"))
            .vectorIndexName(connectionParameters.getString(INDEX_NAME, "vector_index"))
            .pathName(connectionParameters.getString(PATH_NAME, "embedding"))
            .numCandidates(connectionParameters.getInteger(NUM_CANDIDATES, 200))
            .initializeSchema(connectionParameters.getBoolean(INITIALIZE_SCHEMA, false))
            .build();

        try {
            vectorStore.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize MongoDB Atlas vector store", e);
        }

        return vectorStore;
    };

    private MongoDBAtlasConstants() {
    }
}

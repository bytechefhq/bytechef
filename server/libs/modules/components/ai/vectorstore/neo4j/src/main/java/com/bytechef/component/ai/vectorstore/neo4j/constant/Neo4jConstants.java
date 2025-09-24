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

package com.bytechef.component.ai.vectorstore.neo4j.constant;

import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;

import com.bytechef.component.ai.vectorstore.VectorStore;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.neo4j.Neo4jVectorStore;
import org.springframework.ai.vectorstore.neo4j.Neo4jVectorStore.Neo4jDistanceType;

/**
 * @author Monika Kušter
 */
public class Neo4jConstants {

    public static final String DATABASE_NAME = "databaseName";
    public static final String DISTANCE_TYPE = "distanceType";
    public static final String EMBEDDING_DIMENSION = "embeddingDimension";
    public static final String EMBEDDING_PROPERTY = "embeddingProperty";
    public static final String INDEX_NAME = "indexName";
    public static final String INITIALIZE_SCHEMA = "initializeSchema";
    public static final String LABEL = "label";
    public static final String NEO4J = "neo4j";
    public static final String URI = "uri";

    public static final VectorStore VECTOR_STORE = (connectionParameters, embeddingModel) -> {
        Driver driver = GraphDatabase.driver(
            connectionParameters.getRequiredString(URI),
            AuthTokens.basic(
                connectionParameters.getRequiredString(USERNAME), connectionParameters.getRequiredString(PASSWORD)));

        return Neo4jVectorStore.builder(driver, embeddingModel)
            .databaseName(connectionParameters.getString(DATABASE_NAME, "neo4j"))
            .distanceType(connectionParameters.get(DISTANCE_TYPE, Neo4jDistanceType.class, Neo4jDistanceType.COSINE))
            .embeddingDimension(connectionParameters.getInteger(EMBEDDING_DIMENSION, 1536))
            .label(connectionParameters.getString(LABEL, "Document"))
            .embeddingProperty(connectionParameters.getString(EMBEDDING_PROPERTY, "embedding"))
            .indexName(connectionParameters.getString(INDEX_NAME))
            .initializeSchema(connectionParameters.getBoolean(INITIALIZE_SCHEMA, false))
            .batchingStrategy(new TokenCountBatchingStrategy())
            .build();
    };

    private Neo4jConstants() {
    }
}

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

package com.bytechef.component.ai.vectorstore.couchbase.constant;

import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;

import com.bytechef.component.ai.vectorstore.VectorStore;
import com.couchbase.client.java.Cluster;
import org.springframework.ai.vectorstore.couchbase.CouchbaseIndexOptimization;
import org.springframework.ai.vectorstore.couchbase.CouchbaseSearchVectorStore;
import org.springframework.ai.vectorstore.couchbase.CouchbaseSimilarityFunction;

/**
 * @author Monika Kušter
 */
public class CouchbaseConstants {

    public static final String BUCKET_NAME = "bucketName";
    public static final String COLLECTION_NAME = "collectionName";
    public static final String CONNECTION_STRING = "connectionString";
    public static final String COUCHBASE = "couchbase";
    public static final String DIMENSIONS = "dimensions";
    public static final String INDEX_NAME = "indexName";
    public static final String INITIALIZE_SCHEMA = "initializeSchema";
    public static final String OPTIMIZATION = "optimization";
    public static final String SCOPE_NAME = "scopeName";
    public static final String SIMILARITY = "similarity";

    public static final VectorStore VECTOR_STORE = (connectionParameters, embeddingModel) -> {
        Cluster cluster = Cluster.connect(
            connectionParameters.getRequiredString(CONNECTION_STRING),
            connectionParameters.getRequiredString(USERNAME),
            connectionParameters.getRequiredString(PASSWORD));

        return CouchbaseSearchVectorStore
            .builder(cluster, embeddingModel)
            .vectorIndexName(connectionParameters.getRequiredString(INDEX_NAME))
            .bucketName(connectionParameters.getString(BUCKET_NAME, "default"))
            .scopeName(connectionParameters.getString(SCOPE_NAME, "_default"))
            .collectionName(connectionParameters.getString(COLLECTION_NAME, "_default"))
            .initializeSchema(connectionParameters.getBoolean(INITIALIZE_SCHEMA, true))
            .dimensions(connectionParameters.getInteger(DIMENSIONS, 1536))
            .similarityFunction(connectionParameters.getRequired(SIMILARITY, CouchbaseSimilarityFunction.class))
            .indexOptimization(connectionParameters.get(
                OPTIMIZATION, CouchbaseIndexOptimization.class, CouchbaseIndexOptimization.recall))
            .build();
    };

    private CouchbaseConstants() {
    }
}

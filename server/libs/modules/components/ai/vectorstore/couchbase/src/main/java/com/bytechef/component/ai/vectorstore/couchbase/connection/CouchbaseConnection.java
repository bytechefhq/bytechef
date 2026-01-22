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

package com.bytechef.component.ai.vectorstore.couchbase.connection;

import static com.bytechef.component.ai.vectorstore.couchbase.constant.CouchbaseConstants.BUCKET_NAME;
import static com.bytechef.component.ai.vectorstore.couchbase.constant.CouchbaseConstants.COLLECTION_NAME;
import static com.bytechef.component.ai.vectorstore.couchbase.constant.CouchbaseConstants.CONNECTION_STRING;
import static com.bytechef.component.ai.vectorstore.couchbase.constant.CouchbaseConstants.DIMENSIONS;
import static com.bytechef.component.ai.vectorstore.couchbase.constant.CouchbaseConstants.INDEX_NAME;
import static com.bytechef.component.ai.vectorstore.couchbase.constant.CouchbaseConstants.INITIALIZE_SCHEMA;
import static com.bytechef.component.ai.vectorstore.couchbase.constant.CouchbaseConstants.OPTIMIZATION;
import static com.bytechef.component.ai.vectorstore.couchbase.constant.CouchbaseConstants.SCOPE_NAME;
import static com.bytechef.component.ai.vectorstore.couchbase.constant.CouchbaseConstants.SIMILARITY;
import static com.bytechef.component.definition.Authorization.AuthorizationType.CUSTOM;
import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;
import org.springframework.ai.vectorstore.couchbase.CouchbaseIndexOptimization;
import org.springframework.ai.vectorstore.couchbase.CouchbaseSimilarityFunction;

/**
 * @author Monika Kušter
 */
public class CouchbaseConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(CUSTOM)
                .properties(
                    string(CONNECTION_STRING)
                        .label("Connection String")
                        .description("A couchbase connection string")
                        .defaultValue("couchbase://localhost")
                        .required(true),
                    string(USERNAME)
                        .label("Username")
                        .description("Username for authentication with Couchbase.")
                        .required(true),
                    string(PASSWORD)
                        .label("Password")
                        .description("Password for authentication with Couchbase.")
                        .required(true),
                    string(INDEX_NAME)
                        .label("Index Name")
                        .description("The name of the index to store the vectors.")
                        .defaultValue("spring-ai-document-index")
                        .required(false),
                    string(BUCKET_NAME)
                        .label("Bucket Name")
                        .description("The name of the Couchbase Bucket, parent of the scope.")
                        .defaultValue("default")
                        .required(false),
                    string(SCOPE_NAME)
                        .label("Scope Name")
                        .description(
                            "The name of the Couchbase scope, parent of the collection. Search queries will be " +
                                "executed in the scope context.")
                        .defaultValue("_default")
                        .required(false),
                    string(COLLECTION_NAME)
                        .label("Collection Name")
                        .description("The name of the Couchbase collection to store the Documents.")
                        .defaultValue("_default")
                        .required(false),
                    integer(DIMENSIONS)
                        .label("Dimensions")
                        .description("The number of dimensions in the vector.")
                        .defaultValue(1536)
                        .required(false),
                    string(SIMILARITY)
                        .label("Similarity")
                        .description("The similarity function to use.")
                        .options(
                            option("L2 norm", CouchbaseSimilarityFunction.l2_norm.name()),
                            option("Dot product", CouchbaseSimilarityFunction.dot_product.name()))
                        .defaultValue(CouchbaseSimilarityFunction.dot_product.name())
                        .required(true),
                    string(OPTIMIZATION)
                        .label("Optimization")
                        .description("The index optimization strategy to use.")
                        .options(
                            option("Latency", CouchbaseIndexOptimization.latency.name()),
                            option("Recall", CouchbaseIndexOptimization.recall.name()))
                        .defaultValue(CouchbaseIndexOptimization.recall.name())
                        .required(false),
                    bool(INITIALIZE_SCHEMA)
                        .label("Initialize Schema")
                        .description("Whether to initialize the schema.")
                        .defaultValue(true)
                        .required(false)));

    private CouchbaseConnection() {
    }
}

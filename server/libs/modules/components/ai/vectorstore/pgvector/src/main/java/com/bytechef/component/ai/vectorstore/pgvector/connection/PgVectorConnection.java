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

package com.bytechef.component.ai.vectorstore.pgvector.connection;

import static com.bytechef.component.ai.vectorstore.pgvector.constant.PgVectorConstants.DIMENSIONS;
import static com.bytechef.component.ai.vectorstore.pgvector.constant.PgVectorConstants.DISTANCE_TYPE;
import static com.bytechef.component.ai.vectorstore.pgvector.constant.PgVectorConstants.INDEX_TYPE;
import static com.bytechef.component.ai.vectorstore.pgvector.constant.PgVectorConstants.INITIALIZE_SCHEMA;
import static com.bytechef.component.ai.vectorstore.pgvector.constant.PgVectorConstants.MAX_DOCUMENT_BATCH_SIZE;
import static com.bytechef.component.ai.vectorstore.pgvector.constant.PgVectorConstants.SCHEMA_NAME;
import static com.bytechef.component.ai.vectorstore.pgvector.constant.PgVectorConstants.TABLE_NAME;
import static com.bytechef.component.ai.vectorstore.pgvector.constant.PgVectorConstants.URL;
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

/**
 * @author Marko Krišković
 */
public class PgVectorConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(CUSTOM)
                .properties(
                    string(URL)
                        .label("URL")
                        .description(
                            "The JDBC URL of the PostgreSQL instance (e.g. jdbc:postgresql://localhost:5432/postgres).")
                        .defaultValue("jdbc:postgresql://localhost:5432/postgres")
                        .required(true),
                    string(USERNAME)
                        .label("Username")
                        .description("The username for this connection.")
                        .defaultValue("postgres")
                        .required(true),
                    string(PASSWORD)
                        .label("Password")
                        .description("The password for this connection.")
                        .required(true),
                    string(SCHEMA_NAME)
                        .label("Schema Name")
                        .description("The name of the PostgreSQL schema that contains the vector store table.")
                        .defaultValue("public")
                        .required(true),
                    string(TABLE_NAME)
                        .label("Table Name")
                        .description("The name of the table to use for storing vectors.")
                        .defaultValue("vector_store")
                        .required(true),
                    integer(DIMENSIONS)
                        .label("Dimensions")
                        .description("The number of dimensions in the embedding vector.")
                        .defaultValue(1536)
                        .required(true),
                    string(DISTANCE_TYPE)
                        .label("Distance Type")
                        .description("The distance function to use for similarity search.")
                        .options(
                            option("Cosine Distance", "COSINE_DISTANCE"),
                            option("Euclidean Distance", "EUCLIDEAN_DISTANCE"),
                            option("Negative Inner Product", "NEGATIVE_INNER_PRODUCT"))
                        .defaultValue("COSINE_DISTANCE")
                        .required(true),
                    string(INDEX_TYPE)
                        .label("Index Type")
                        .description("The index algorithm to use for approximate nearest neighbor search.")
                        .options(
                            option("HNSW", "HNSW"),
                            option("IVFFlat", "IVFFLAT"),
                            option("None", "NONE"))
                        .defaultValue("HNSW")
                        .required(true),
                    bool(INITIALIZE_SCHEMA)
                        .label("Initialize Schema")
                        .description("Whether to initialize the schema on startup.")
                        .defaultValue(false)
                        .required(true),
                    integer(MAX_DOCUMENT_BATCH_SIZE)
                        .label("Max Document Batch Size")
                        .description("The maximum number of documents to process in a single batch.")
                        .defaultValue(10000)
                        .required(true)));

    private PgVectorConnection() {
    }
}

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
import static com.bytechef.component.ai.vectorstore.pgvector.constant.PgVectorConstants.PASSWORD;
import static com.bytechef.component.ai.vectorstore.pgvector.constant.PgVectorConstants.REMOVE_EXISTING_TABLE;
import static com.bytechef.component.ai.vectorstore.pgvector.constant.PgVectorConstants.SCHEMA_NAME;
import static com.bytechef.component.ai.vectorstore.pgvector.constant.PgVectorConstants.TABLE_NAME;
import static com.bytechef.component.ai.vectorstore.pgvector.constant.PgVectorConstants.URL;
import static com.bytechef.component.ai.vectorstore.pgvector.constant.PgVectorConstants.USERNAME;
import static com.bytechef.component.definition.Authorization.AuthorizationType.CUSTOM;
import static com.bytechef.component.definition.ComponentDsl.authorization;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.connection;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableConnectionDefinition;

/**
 * @author Ivica Cardic
 */
public class PgVectorConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(CUSTOM)
                .properties(
                    string(URL)
                        .label("JDBC URL")
                        .description("The PostgreSQL JDBC connection URL.")
                        .exampleValue("jdbc:postgresql://localhost:5432/vectordb")
                        .required(true),
                    string(USERNAME)
                        .label("Username")
                        .description("The database username.")
                        .required(true),
                    string(PASSWORD)
                        .label("Password")
                        .description("The database password.")
                        .required(true),
                    string(SCHEMA_NAME)
                        .label("Schema Name")
                        .description("The database schema name.")
                        .defaultValue("public")
                        .advancedOption(true)
                        .required(false),
                    string(TABLE_NAME)
                        .label("Table Name")
                        .description("The name of the vector table.")
                        .defaultValue("vector_store")
                        .advancedOption(true)
                        .required(false),
                    integer(DIMENSIONS)
                        .label("Dimensions")
                        .description(
                            "The vector dimensions. If not specified, it will be auto-detected from the embedding " +
                                "model.")
                        .advancedOption(true)
                        .required(false),
                    string(DISTANCE_TYPE)
                        .label("Distance Type")
                        .description("The distance function to use for similarity search.")
                        .options(
                            option("Cosine Distance", "COSINE_DISTANCE"),
                            option("Euclidean Distance", "EUCLIDEAN_DISTANCE"),
                            option("Negative Inner Product", "NEGATIVE_INNER_PRODUCT"))
                        .defaultValue("COSINE_DISTANCE")
                        .advancedOption(true)
                        .required(false),
                    string(INDEX_TYPE)
                        .label("Index Type")
                        .description("The index type for vector search optimization.")
                        .options(
                            option("HNSW", "HNSW"),
                            option("IVFFlat", "IVFFLAT"),
                            option("None", "NONE"))
                        .defaultValue("HNSW")
                        .advancedOption(true)
                        .required(false),
                    bool(INITIALIZE_SCHEMA)
                        .label("Initialize Schema")
                        .description("Whether to create the vector table if it does not exist.")
                        .defaultValue(false)
                        .advancedOption(true)
                        .required(false),
                    bool(REMOVE_EXISTING_TABLE)
                        .label("Remove Existing Table")
                        .description("Whether to drop and recreate the vector table on startup.")
                        .defaultValue(false)
                        .advancedOption(true)
                        .required(false)));

    private PgVectorConnection() {
    }
}

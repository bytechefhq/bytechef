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

package com.bytechef.component.ai.vectorstore.oracle.connection;

import static com.bytechef.component.ai.vectorstore.oracle.constant.OracleConstants.DIMENSIONS;
import static com.bytechef.component.ai.vectorstore.oracle.constant.OracleConstants.DISTANCE_TYPE;
import static com.bytechef.component.ai.vectorstore.oracle.constant.OracleConstants.INDEX_TYPE;
import static com.bytechef.component.ai.vectorstore.oracle.constant.OracleConstants.INITIALIZE_SCHEMA;
import static com.bytechef.component.ai.vectorstore.oracle.constant.OracleConstants.TABLE_NAME;
import static com.bytechef.component.ai.vectorstore.oracle.constant.OracleConstants.URL;
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
import com.bytechef.component.definition.Property.ControlType;
import org.springframework.ai.vectorstore.oracle.OracleVectorStore;

/**
 * @author Marko Krišković
 */
public class OracleConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(CUSTOM)
                .properties(
                    string(URL)
                        .label("JDBC URL")
                        .description(
                            "Oracle JDBC connection URL (e.g., jdbc:oracle:thin:@localhost:1521/FREEPDB1).")
                        .required(true),
                    string(USERNAME)
                        .label("Username")
                        .description("Oracle database username.")
                        .required(true),
                    string(PASSWORD)
                        .label("Password")
                        .description("Oracle database password.")
                        .controlType(ControlType.PASSWORD)
                        .required(true),
                    string(TABLE_NAME)
                        .label("Table Name")
                        .description("Name of the database table used to store vector embeddings.")
                        .defaultValue(OracleVectorStore.DEFAULT_TABLE_NAME)
                        .required(false),
                    string(INDEX_TYPE)
                        .label("Index Type")
                        .description("Vector index type to use for similarity search.")
                        .options(
                            option("None", OracleVectorStore.OracleVectorStoreIndexType.NONE.name()),
                            option("HNSW", OracleVectorStore.OracleVectorStoreIndexType.HNSW.name()),
                            option("IVF", OracleVectorStore.OracleVectorStoreIndexType.IVF.name()))
                        .defaultValue(OracleVectorStore.DEFAULT_INDEX_TYPE.name())
                        .required(false),
                    string(DISTANCE_TYPE)
                        .label("Distance Type")
                        .description("Distance function used for vector similarity comparison.")
                        .options(
                            option("Cosine", OracleVectorStore.OracleVectorStoreDistanceType.COSINE.name()),
                            option("Dot Product", OracleVectorStore.OracleVectorStoreDistanceType.DOT.name()),
                            option("Euclidean", OracleVectorStore.OracleVectorStoreDistanceType.EUCLIDEAN.name()),
                            option(
                                "Euclidean Squared",
                                OracleVectorStore.OracleVectorStoreDistanceType.EUCLIDEAN_SQUARED.name()),
                            option("Manhattan", OracleVectorStore.OracleVectorStoreDistanceType.MANHATTAN.name()))
                        .defaultValue(OracleVectorStore.DEFAULT_DISTANCE_TYPE.name())
                        .required(false),
                    integer(DIMENSIONS)
                        .label("Dimensions")
                        .description(
                            "Number of dimensions for the vector embeddings. Use -1 to infer from the embedding " +
                                "model.")
                        .defaultValue(OracleVectorStore.DEFAULT_DIMENSIONS)
                        .required(false),
                    bool(INITIALIZE_SCHEMA)
                        .label("Initialize Schema")
                        .description("Whether to create the vector store table automatically if it does not exist.")
                        .defaultValue(false)
                        .required(false)));

    private OracleConnection() {
    }
}

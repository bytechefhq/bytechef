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

package com.bytechef.component.ai.vectorstore.mariadb.connection;

import static com.bytechef.component.ai.vectorstore.mariadb.constant.MariaDBVectorStoreConstants.DIMENSIONS;
import static com.bytechef.component.ai.vectorstore.mariadb.constant.MariaDBVectorStoreConstants.DISTANCE_TYPE;
import static com.bytechef.component.ai.vectorstore.mariadb.constant.MariaDBVectorStoreConstants.INITIALIZE_SCHEMA;
import static com.bytechef.component.ai.vectorstore.mariadb.constant.MariaDBVectorStoreConstants.SCHEMA_NAME;
import static com.bytechef.component.ai.vectorstore.mariadb.constant.MariaDBVectorStoreConstants.TABLE_NAME;
import static com.bytechef.component.ai.vectorstore.mariadb.constant.MariaDBVectorStoreConstants.URL;
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
import org.springframework.ai.vectorstore.mariadb.MariaDBVectorStore;

/**
 * @author Marko Krišković
 */
public class MariaDBVectorStoreConnection {

    public static final ModifiableConnectionDefinition CONNECTION_DEFINITION = connection()
        .authorizations(
            authorization(CUSTOM)
                .properties(
                    string(URL)
                        .label("JDBC URL")
                        .description(
                            "MariaDB JDBC connection URL (e.g., jdbc:mariadb://localhost:3306/mydb).")
                        .required(true),
                    string(USERNAME)
                        .label("Username")
                        .description("MariaDB database username.")
                        .required(true),
                    string(PASSWORD)
                        .label("Password")
                        .description("MariaDB database password.")
                        .required(true),
                    string(TABLE_NAME)
                        .label("Table Name")
                        .description("Name of the database table used to store vector embeddings.")
                        .defaultValue(MariaDBVectorStore.DEFAULT_TABLE_NAME)
                        .required(false),
                    string(SCHEMA_NAME)
                        .label("Schema Name")
                        .description("Database schema name. If not specified, the default schema is used.")
                        .required(false),
                    string(DISTANCE_TYPE)
                        .label("Distance Type")
                        .description("Distance function used for vector similarity comparison.")
                        .options(
                            option("Cosine", MariaDBVectorStore.MariaDBDistanceType.COSINE.name()),
                            option("Euclidean", MariaDBVectorStore.MariaDBDistanceType.EUCLIDEAN.name()))
                        .defaultValue(MariaDBVectorStore.MariaDBDistanceType.COSINE.name())
                        .required(false),
                    integer(DIMENSIONS)
                        .label("Dimensions")
                        .description(
                            "Number of dimensions for the vector embeddings. If not specified, inferred from the " +
                                "embedding model.")
                        .defaultValue(MariaDBVectorStore.OPENAI_EMBEDDING_DIMENSION_SIZE)
                        .required(false),
                    bool(INITIALIZE_SCHEMA)
                        .label("Initialize Schema")
                        .description("Whether to create the vector store table automatically if it does not exist.")
                        .defaultValue(false)
                        .required(false)));

    private MariaDBVectorStoreConnection() {
    }
}

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

package com.bytechef.component.ai.vectorstore.mariadb.constant;

import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;

import com.bytechef.component.ai.vectorstore.VectorStore;
import com.bytechef.component.definition.Parameters;
import org.springframework.ai.vectorstore.mariadb.MariaDBVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * @author Marko Krišković
 */
public class MariaDBVectorStoreConstants {

    public static final String DIMENSIONS = "dimensions";
    public static final String DISTANCE_TYPE = "distanceType";
    public static final String INITIALIZE_SCHEMA = "initializeSchema";
    public static final String MARIA_DB_VECTOR_STORE = "mariaDbVectorStore";
    public static final String SCHEMA_NAME = "schemaName";
    public static final String TABLE_NAME = "tableName";
    public static final String URL = "url";

    public static final VectorStore VECTOR_STORE = (connectionParameters, embeddingModel) -> {
        JdbcTemplate jdbcTemplate = createJdbcTemplate(connectionParameters);

        MariaDBVectorStore vectorStore = MariaDBVectorStore.builder(jdbcTemplate, embeddingModel)
            .vectorTableName(
                connectionParameters.getString(TABLE_NAME, MariaDBVectorStore.DEFAULT_TABLE_NAME))
            .schemaName(connectionParameters.getString(SCHEMA_NAME, null))
            .distanceType(
                connectionParameters.get(DISTANCE_TYPE, MariaDBVectorStore.MariaDBDistanceType.class,
                    MariaDBVectorStore.MariaDBDistanceType.COSINE))
            .dimensions(connectionParameters.getInteger(DIMENSIONS, MariaDBVectorStore.INVALID_EMBEDDING_DIMENSION))
            .initializeSchema(connectionParameters.getBoolean(INITIALIZE_SCHEMA, false))
            .build();

        try {
            vectorStore.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize MariaDB vector store", e);
        }

        return vectorStore;
    };

    private static JdbcTemplate createJdbcTemplate(Parameters connectionParameters) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();

        dataSource.setDriverClassName("org.mariadb.jdbc.Driver");
        dataSource.setUrl(connectionParameters.getRequiredString(URL));
        dataSource.setUsername(connectionParameters.getRequiredString(USERNAME));
        dataSource.setPassword(connectionParameters.getRequiredString(PASSWORD));

        return new JdbcTemplate(dataSource);
    }

    private MariaDBVectorStoreConstants() {
    }
}

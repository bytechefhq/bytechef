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

package com.bytechef.component.ai.vectorstore.oracle.constant;

import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;

import com.bytechef.component.ai.vectorstore.VectorStore;
import com.bytechef.component.definition.Parameters;
import org.springframework.ai.vectorstore.oracle.OracleVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * @author Marko Krišković
 */
public class OracleVectorStoreConstants {

    public static final String DIMENSIONS = "dimensions";
    public static final String DISTANCE_TYPE = "distanceType";
    public static final String INDEX_TYPE = "indexType";
    public static final String INITIALIZE_SCHEMA = "initializeSchema";
    public static final String ORACLE_VECTOR_STORE = "oracleVectorStore";
    public static final String TABLE_NAME = "tableName";
    public static final String URL = "url";

    public static final VectorStore VECTOR_STORE = (connectionParameters, embeddingModel) -> {
        JdbcTemplate jdbcTemplate = createJdbcTemplate(connectionParameters);

        OracleVectorStore vectorStore = OracleVectorStore.builder(jdbcTemplate, embeddingModel)
            .tableName(connectionParameters.getString(TABLE_NAME, OracleVectorStore.DEFAULT_TABLE_NAME))
            .indexType(
                connectionParameters.get(INDEX_TYPE, OracleVectorStore.OracleVectorStoreIndexType.class,
                    OracleVectorStore.DEFAULT_INDEX_TYPE))
            .distanceType(
                connectionParameters.get(DISTANCE_TYPE, OracleVectorStore.OracleVectorStoreDistanceType.class,
                    OracleVectorStore.DEFAULT_DISTANCE_TYPE))
            .dimensions(connectionParameters.getInteger(DIMENSIONS, OracleVectorStore.DEFAULT_DIMENSIONS))
            .initializeSchema(connectionParameters.getBoolean(INITIALIZE_SCHEMA, false))
            .build();

        try {
            vectorStore.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Oracle vector store", e);
        }

        return vectorStore;
    };

    private static JdbcTemplate createJdbcTemplate(Parameters connectionParameters) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();

        dataSource.setDriverClassName("oracle.jdbc.OracleDriver");
        dataSource.setUrl(connectionParameters.getRequiredString(URL));
        dataSource.setUsername(connectionParameters.getRequiredString(USERNAME));
        dataSource.setPassword(connectionParameters.getRequiredString(PASSWORD));

        return new JdbcTemplate(dataSource);
    }

    private OracleVectorStoreConstants() {
    }
}

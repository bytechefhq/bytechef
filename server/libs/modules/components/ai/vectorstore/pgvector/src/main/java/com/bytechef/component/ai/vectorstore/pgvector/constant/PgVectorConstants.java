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

package com.bytechef.component.ai.vectorstore.pgvector.constant;

import com.bytechef.component.ai.vectorstore.VectorStore;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
public class PgVectorConstants {

    public static final String DIMENSIONS = "dimensions";
    public static final String DISTANCE_TYPE = "distanceType";
    public static final String INDEX_TYPE = "indexType";
    public static final String INITIALIZE_SCHEMA = "initializeSchema";
    public static final String PASSWORD = "password";
    public static final String PGVECTOR = "pgvector";
    public static final String REMOVE_EXISTING_TABLE = "removeExistingTable";
    public static final String SCHEMA_NAME = "schemaName";
    public static final String TABLE_NAME = "tableName";
    public static final String URL = "url";
    public static final String USERNAME = "username";

    public static final VectorStore VECTOR_STORE = (connectionParameters, embeddingModel) -> {
        HikariDataSource dataSource = new HikariDataSource();

        dataSource.setJdbcUrl(connectionParameters.getRequiredString(URL));
        dataSource.setUsername(connectionParameters.getRequiredString(USERNAME));
        dataSource.setPassword(connectionParameters.getRequiredString(PASSWORD));

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        PgVectorStore.PgVectorStoreBuilder builder = PgVectorStore.builder(jdbcTemplate, embeddingModel);

        String schemaName = connectionParameters.getString(SCHEMA_NAME);

        if (schemaName != null) {
            builder.schemaName(schemaName);
        }

        String tableName = connectionParameters.getString(TABLE_NAME);

        if (tableName != null) {
            builder.vectorTableName(tableName);
        }

        Integer dimensions = connectionParameters.getInteger(DIMENSIONS);

        if (dimensions != null) {
            builder.dimensions(dimensions);
        }

        String distanceType = connectionParameters.getString(DISTANCE_TYPE);

        if (distanceType != null) {
            builder.distanceType(PgVectorStore.PgDistanceType.valueOf(distanceType));
        }

        String indexType = connectionParameters.getString(INDEX_TYPE);

        if (indexType != null) {
            builder.indexType(PgVectorStore.PgIndexType.valueOf(indexType));
        }

        Boolean initializeSchema = connectionParameters.getBoolean(INITIALIZE_SCHEMA);

        if (initializeSchema != null) {
            builder.initializeSchema(initializeSchema);
        }

        Boolean removeExistingTable = connectionParameters.getBoolean(REMOVE_EXISTING_TABLE);

        if (removeExistingTable != null) {
            builder.removeExistingVectorStoreTable(removeExistingTable);
        }

        return builder.build();
    };

    private PgVectorConstants() {
    }
}

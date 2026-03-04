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

import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;

import com.bytechef.component.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * @author Marko Krišković
 */
public class PGVectorConstants {

    public static final String DIMENSIONS = "dimensions";
    public static final String DISTANCE_TYPE = "distanceType";
    public static final String INDEX_TYPE = "indexType";
    public static final String INITIALIZE_SCHEMA = "initializeSchema";
    public static final String MAX_DOCUMENT_BATCH_SIZE = "maxDocumentBatchSize";
    public static final String PGVECTOR = "pgVector";
    public static final String SCHEMA_NAME = "schemaName";
    public static final String TABLE_NAME = "tableName";
    public static final String URL = "url";

    public static final VectorStore VECTOR_STORE = (connectionParameters, embeddingModel) -> {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();

        dataSource.setUrl(connectionParameters.getRequiredString(URL));
        dataSource.setUsername(connectionParameters.getRequiredString(USERNAME));
        dataSource.setPassword(connectionParameters.getRequiredString(PASSWORD));

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
            .dimensions(connectionParameters.getRequiredInteger(DIMENSIONS))
            .distanceType(
                PgVectorStore.PgDistanceType.valueOf(connectionParameters.getRequiredString(DISTANCE_TYPE)))
            .indexType(PgVectorStore.PgIndexType.valueOf(connectionParameters.getRequiredString(INDEX_TYPE)))
            .initializeSchema(connectionParameters.getRequiredBoolean(INITIALIZE_SCHEMA))
            .schemaName(connectionParameters.getRequiredString(SCHEMA_NAME))
            .vectorTableName(connectionParameters.getRequiredString(TABLE_NAME))
            .maxDocumentBatchSize(connectionParameters.getRequiredInteger(MAX_DOCUMENT_BATCH_SIZE))
            .build();
    };

    private PGVectorConstants() {
    }
}

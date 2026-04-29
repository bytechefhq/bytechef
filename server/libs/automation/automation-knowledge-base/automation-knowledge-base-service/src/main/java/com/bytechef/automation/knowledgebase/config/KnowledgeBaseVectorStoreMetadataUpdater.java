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

package com.bytechef.automation.knowledgebase.config;

import static com.bytechef.automation.knowledgebase.constant.KnowledgeBaseConstants.METADATA_TAG_IDS;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Updates tag metadata in the vector store without triggering re-embedding.
 *
 * @author Marko Kriskovic
 */
public class KnowledgeBaseVectorStoreMetadataUpdater {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseVectorStoreMetadataUpdater.class);

    private final JdbcTemplate pgVectorJdbcTemplate;
    private final ObjectMapper objectMapper;
    private final String fullTableName;

    public KnowledgeBaseVectorStoreMetadataUpdater(
        JdbcTemplate pgVectorJdbcTemplate, ObjectMapper objectMapper, String fullTableName) {

        this.pgVectorJdbcTemplate = pgVectorJdbcTemplate;
        this.objectMapper = objectMapper;
        this.fullTableName = fullTableName;
    }

    /**
     * Updates only the {@code tag_ids} field in the vector store metadata for the given entry. Preserves all other
     * existing metadata fields (e.g., {@code source}, {@code charset}) and avoids re-embedding the content.
     *
     * @param vectorStoreId the vector store document ID
     * @param tagIds        the new tag IDs to set; an empty list removes the field
     */
    public void updateTagIds(String vectorStoreId, List<Long> tagIds) {
        List<String> rows = pgVectorJdbcTemplate.queryForList(
            "SELECT metadata::text FROM " + fullTableName + " WHERE id = ?::uuid",
            String.class, vectorStoreId);

        if (rows.isEmpty()) {
            logger.warn("Vector store entry not found for id={}, skipping tag metadata update", vectorStoreId);

            return;
        }

        Map<String, Object> metadata = objectMapper.readValue(rows.get(0), new TypeReference<>() {});

        // preserve insertion order so tag_ids ends up at the tail
        Map<String, Object> updatedMetadata = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String key = entry.getKey();

            // drop old boolean-flag format (tag_ids_1055: true) and old array format
            if (!key.startsWith(METADATA_TAG_IDS + "_") && !key.equals(METADATA_TAG_IDS)) {
                updatedMetadata.put(key, entry.getValue());
            }
        }

        if (!tagIds.isEmpty()) {
            updatedMetadata.put(METADATA_TAG_IDS, tagIds);
        }

        pgVectorJdbcTemplate.update(
            "UPDATE " + fullTableName + " SET metadata = ?::jsonb WHERE id = ?::uuid",
            objectMapper.writeValueAsString(updatedMetadata), vectorStoreId);
    }
}

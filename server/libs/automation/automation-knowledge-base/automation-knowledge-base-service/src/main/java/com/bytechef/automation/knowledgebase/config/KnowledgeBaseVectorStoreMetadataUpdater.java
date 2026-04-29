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

import static com.bytechef.automation.knowledgebase.constant.KnowledgeBaseConstants.METADATA_TAG_NAMES;

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
     * Updates tag metadata in the vector store for the given entry. Preserves all other existing metadata fields
     * (e.g., {@code source}, {@code charset}) and avoids re-embedding the content. Stores both
     * {@code tag_names: [list]} (human-readable) and {@code tag_names_NAME: true} boolean flags (used for filtering).
     *
     * @param vectorStoreId the vector store document ID
     * @param tagNames      the new tag names to set; an empty list removes all tag fields
     */
    public void updateTagNames(String vectorStoreId, List<String> tagNames) {
        List<String> rows = pgVectorJdbcTemplate.queryForList(
            "SELECT metadata::text FROM " + fullTableName + " WHERE id = ?::uuid",
            String.class, vectorStoreId);

        if (rows.isEmpty()) {
            logger.warn("Vector store entry not found for id={}, skipping tag metadata update", vectorStoreId);

            return;
        }

        Map<String, Object> metadata = objectMapper.readValue(rows.get(0), new TypeReference<>() {});

        // preserve insertion order so tag_names ends up at the tail
        Map<String, Object> updatedMetadata = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String key = entry.getKey();

            // drop existing tag entries so they can be rebuilt fresh
            if (!key.startsWith(METADATA_TAG_NAMES + "_") && !key.equals(METADATA_TAG_NAMES)) {
                updatedMetadata.put(key, entry.getValue());
            }
        }

        if (!tagNames.isEmpty()) {
            updatedMetadata.put(METADATA_TAG_NAMES, tagNames);

            for (String tagName : tagNames) {
                updatedMetadata.put(METADATA_TAG_NAMES + "_" + tagName, true);
            }
        }

        pgVectorJdbcTemplate.update(
            "UPDATE " + fullTableName + " SET metadata = ?::jsonb WHERE id = ?::uuid",
            objectMapper.writeValueAsString(updatedMetadata), vectorStoreId);
    }
}

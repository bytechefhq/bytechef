/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.service;

import com.bytechef.ee.ai.copilot.domain.VectorStore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Marko Kriskovic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class VectorStoreService {

    private final VectorStoreRowMapper rowMapper;
    private final JdbcTemplate jdbcTemplate;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public VectorStoreService(@Qualifier("pgVectorJdbcTemplate") JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = new VectorStoreRowMapper(objectMapper);
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vector_store", Long.class);

        return count != null ? count : 0L;
    }

    public List<VectorStore> findAll() {
        return jdbcTemplate.query(
            "SELECT id, content, metadata, embedding::text as embedding FROM vector_store", rowMapper);
    }

    private static class VectorStoreRowMapper implements RowMapper<VectorStore> {

        private final ObjectMapper objectMapper;

        public VectorStoreRowMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public VectorStore mapRow(ResultSet rs, int rowNum) throws SQLException {
            UUID id = (UUID) rs.getObject("id");
            String content = rs.getString("content");
            Map<String, Object> metadata = parseMetadata(rs.getObject("metadata"));
            List<Double> embedding = parseEmbedding(rs.getString("embedding"));

            return new VectorStore(id, content, metadata, embedding);
        }

        private Map<String, Object> parseMetadata(Object metadataObj) throws SQLException {
            if (metadataObj == null) {
                return Map.of();
            }

            try {
                String metadataJson;

                if (metadataObj instanceof PGobject pgObject) {
                    metadataJson = pgObject.getValue();
                } else {
                    metadataJson = metadataObj.toString();
                }

                return objectMapper.readValue(metadataJson, new TypeReference<>() {});
            } catch (Exception e) {
                throw new SQLException("Failed to parse metadata JSON", e);
            }
        }

        private List<Double> parseEmbedding(String embeddingStr) {
            if (embeddingStr == null || embeddingStr.isEmpty()) {
                return List.of();
            }

            // pgvector format: [1.0,2.0,3.0]
            String cleaned = embeddingStr.trim()
                .replaceAll("^\\[", "")
                .replaceAll("]$", "");

            if (cleaned.isEmpty()) {
                return List.of();
            }

            return Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .map(Double::parseDouble)
                .collect(Collectors.toList());
        }
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.toolsearch;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.toolsearch.ToolReference;
import org.springframework.ai.tool.toolsearch.index.vectorstore.VectorToolIndex;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ToolSearchCatalogFeeder {

    public static final String CATALOG_SESSION_ID = "ai_hub_tool_catalog";

    public static final String GLOBAL_ASK_SESSION_ID = CATALOG_SESSION_ID + ":global:ask";

    public static final String GLOBAL_BUILD_SESSION_ID = CATALOG_SESSION_ID + ":global:build";

    static final String META_TABLE_NAME = "ai_hub_tool_search_catalog_meta";

    private static final Logger log = LoggerFactory.getLogger(ToolSearchCatalogFeeder.class);

    private final VectorToolIndex vectorToolIndex;
    private final JdbcTemplate pgVectorJdbcTemplate;
    private final String qualifiedMetaTableName;
    private final String qualifiedVectorTableName;

    private volatile boolean metaTableEnsured;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ToolSearchCatalogFeeder(
        VectorToolIndex vectorToolIndex, JdbcTemplate pgVectorJdbcTemplate, String schemaName,
        String vectorTableName) {

        this.vectorToolIndex = vectorToolIndex;
        this.pgVectorJdbcTemplate = pgVectorJdbcTemplate;
        this.qualifiedVectorTableName = schemaName + "." + vectorTableName;
        this.qualifiedMetaTableName = schemaName + "." + META_TABLE_NAME;
    }

    @SuppressFBWarnings("UNSAFE_HASH_EQUALS")
    public void populateGlobalTools(String sessionId, List<ToolCallback> toolCallbacks) {
        List<CatalogEntry> entries = new ArrayList<>();

        for (ToolCallback toolCallback : toolCallbacks) {
            ToolDefinition toolDefinition = toolCallback.getToolDefinition();
            String summary = toolDefinition.description();

            if (summary == null || summary.isBlank()) {
                continue;
            }

            entries.add(new CatalogEntry(toolDefinition.name(), summary));
        }

        String currentHash = computeCatalogHash(entries);

        ensureMetaTable();

        Optional<String> storedHash = readStoredHash(sessionId);

        if (storedHash.isPresent() && storedHash.get()
            .equals(currentHash)) {

            log.info(
                "Global tool session unchanged (hash matches {} entries under session {}); skipping re-embedding",
                entries.size(), sessionId);

            return;
        }

        int indexed = indexEntries(sessionId, entries);

        writeStoredHash(sessionId, currentHash, indexed);

        log.info(
            "Global tool session populated: indexed {} of {} static tools under session {}",
            indexed, toolCallbacks.size(), sessionId);
    }

    private int indexEntries(String sessionId, List<CatalogEntry> entries) {
        clearSession(sessionId);

        if (entries.isEmpty()) {
            return 0;
        }

        List<ToolReference> references = entries.stream()
            .map(entry -> ToolReference.builder()
                .toolName(entry.toolName())
                .summary(entry.summary())
                .build())
            .toList();

        vectorToolIndex.indexTools(sessionId, references);

        return references.size();
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void clearSession(String sessionId) {
        vectorToolIndex.clearIndex(sessionId);

        int deleted = pgVectorJdbcTemplate.update(
            "DELETE FROM " + qualifiedVectorTableName + " WHERE metadata->>'sessionId' = ?", sessionId);

        if (deleted > 0 && log.isDebugEnabled()) {
            log.debug("Cleared {} stale tool search rows for session {}", deleted, sessionId);
        }
    }

    record CatalogEntry(String toolName, String summary) {
    }

    static String computeCatalogHash(List<CatalogEntry> entries) {
        List<CatalogEntry> sorted = new ArrayList<>(entries);

        sorted.sort(Comparator.comparing(CatalogEntry::toolName));

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            for (CatalogEntry entry : sorted) {
                digest.update(entry.toolName()
                    .getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(entry.summary()
                    .getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
            }

            return HexFormat.of()
                .formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void ensureMetaTable() {
        if (metaTableEnsured) {
            return;
        }

        pgVectorJdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS " + qualifiedMetaTableName + " ("
                + "session_id TEXT PRIMARY KEY,"
                + "catalog_hash TEXT NOT NULL,"
                + "tool_count INTEGER NOT NULL,"
                + "last_indexed_at TIMESTAMPTZ NOT NULL DEFAULT NOW())");

        metaTableEnsured = true;
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private Optional<String> readStoredHash(String sessionId) {
        try {
            String hash = pgVectorJdbcTemplate.queryForObject(
                "SELECT catalog_hash FROM " + qualifiedMetaTableName + " WHERE session_id = ?",
                String.class, sessionId);

            return Optional.ofNullable(hash);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void writeStoredHash(String sessionId, String hash, int toolCount) {
        pgVectorJdbcTemplate.update(
            "INSERT INTO " + qualifiedMetaTableName + " (session_id, catalog_hash, tool_count, last_indexed_at)"
                + " VALUES (?, ?, ?, NOW())"
                + " ON CONFLICT (session_id) DO UPDATE"
                + " SET catalog_hash = EXCLUDED.catalog_hash,"
                + "     tool_count = EXCLUDED.tool_count,"
                + "     last_indexed_at = NOW()",
            sessionId, hash, toolCount);
    }
}

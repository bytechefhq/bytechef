/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat.repository;

import com.bytechef.ee.ai.hub.chat.AiHubChatAssetFile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data JDBC repository for {@link AiHubChatAssetFile} rows.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubChatAssetFileRepository
    extends CrudRepository<AiHubChatAssetFile, Long> {

    /**
     * Returns the join rows for the given chat, newest first. The same asset_file may appear multiple times here when
     * the chat has more than one relationship to it (e.g. AUTHORED + READ_BY_TOOL); de-duplication is the caller's
     * responsibility based on listing intent.
     */
    List<AiHubChatAssetFile> findByChatIdOrderByCreatedAtDesc(long chatId);

    /**
     * Idempotent existence probe used by the service layer before INSERT to avoid relying on the unique constraint as a
     * primary control-flow signal. The constraint stays as defense in depth; this query keeps the service path
     * exception-free for the common "already linked" case.
     */
    Optional<AiHubChatAssetFile> findByChatIdAndAssetFileIdAndRelationship(
        long chatId, long assetFileId, int relationship);

    /**
     * Returns the asset_file ids referenced (any relationship type) by the chat, deduplicated. Powers the chat files
     * panel which shows each file once regardless of how many relationship rows exist.
     */
    @Query("SELECT DISTINCT asset_file_id FROM ai_hub_chat_asset_file " +
        "WHERE chat_id = :chatId ORDER BY asset_file_id")
    List<Long> findDistinctAssetFileIdsByChatId(long chatId);

    /**
     * Returns the AUTHORED row for the given asset_file, if any. The partial unique index guarantees zero or one match
     * — using {@code Optional} rather than a list makes that contract explicit at the call site. A second AUTHORED row
     * cannot reach the database without bypassing the index.
     */
    @Query("SELECT * FROM ai_hub_chat_asset_file " +
        "WHERE asset_file_id = :assetFileId AND relationship = 0")
    Optional<AiHubChatAssetFile> findAuthoredByAssetFileId(long assetFileId);
}

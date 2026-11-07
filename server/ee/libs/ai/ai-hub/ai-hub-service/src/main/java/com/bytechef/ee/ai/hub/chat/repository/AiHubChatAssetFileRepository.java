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
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubChatAssetFileRepository extends CrudRepository<AiHubChatAssetFile, Long> {

    @Modifying
    @Query("""
        DELETE FROM ai_hub_chat_asset_file
        WHERE chat_id = :chatId AND asset_file_id = :assetFileId AND relationship = :relationship
        """)
    void deleteByChatIdAndAssetFileIdAndRelationship(long chatId, long assetFileId, int relationship);

    @Query("SELECT * FROM ai_hub_chat_asset_file WHERE asset_file_id = :assetFileId AND relationship = 0")
    Optional<AiHubChatAssetFile> findAuthoredByAssetFileId(long assetFileId);

    Optional<AiHubChatAssetFile> findByChatIdAndAssetFileIdAndRelationship(
        long chatId, long assetFileId, int relationship);

    List<AiHubChatAssetFile> findByChatIdOrderByCreatedAtDesc(long chatId);

    @Query("SELECT DISTINCT asset_file_id FROM ai_hub_chat_asset_file WHERE chat_id = :chatId ORDER BY asset_file_id")
    List<Long> findDistinctAssetFileIdsByChatId(long chatId);
}

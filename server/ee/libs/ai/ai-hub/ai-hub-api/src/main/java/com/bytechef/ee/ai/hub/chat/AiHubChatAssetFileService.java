/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import java.util.List;
import java.util.Optional;

/**
 * Records and queries the join rows linking AI Hub chats to {@code asset_file} rows.
 *
 * <p>
 * The service treats {@link #recordRelationship(long, long, AiHubChatAssetFileRelationship)} as idempotent: calling it
 * twice for the same triple is a no-op. The dedicated {@link #recordAuthorship(long, long)} method exists because
 * AUTHORED has a partial unique constraint and surfaces a different exception
 * ({@link com.bytechef.ee.ai.hub.chat.AuthorshipAlreadyAssignedException}) when violated, which the caller can act on.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubChatAssetFileService {

    /**
     * Records that {@code chatId} authored {@code assetFileId}. Throws
     * {@link com.bytechef.ee.ai.hub.chat.AuthorshipAlreadyAssignedException} when the asset_file already has a
     * different authoring chat.
     *
     * <p>
     * Idempotent for the same chat: if {@code chatId} is already the recorded author, returns the existing row without
     * inserting a duplicate.
     */
    AiHubChatAssetFile recordAuthorship(long chatId, long assetFileId);

    /**
     * Records a non-authorship relationship (ATTACHED, MENTIONED, READ_BY_TOOL). Idempotent — re-recording the same
     * relationship returns the existing row instead of throwing on the unique constraint.
     *
     * @throws IllegalArgumentException when called with {@link AiHubChatAssetFileRelationship#AUTHORED} — use
     *                                  {@link #recordAuthorship(long, long)} for that.
     */
    AiHubChatAssetFile recordRelationship(
        long chatId, long assetFileId, AiHubChatAssetFileRelationship relationship);

    /**
     * Returns the join rows for the chat, newest first, including duplicates if a file has multiple relationship types.
     * Use {@link #findDistinctAssetFileIdsByChatId(long)} when the listing intent is "files this chat has touched" with
     * one row per file.
     */
    List<AiHubChatAssetFile> findByChatId(long chatId);

    /**
     * Returns deduplicated asset_file ids referenced (any relationship type) by the chat. Drives the chat files panel;
     * the caller hydrates the {@code AssetFile} rows separately to keep this service decoupled from the
     * {@code automation-asset-file} module's facade.
     */
    List<Long> findDistinctAssetFileIdsByChatId(long chatId);

    /**
     * Returns the AUTHORED row for the asset_file, if any. The partial unique index guarantees zero or one match.
     */
    Optional<AiHubChatAssetFile> findAuthoredByAssetFileId(long assetFileId);
}

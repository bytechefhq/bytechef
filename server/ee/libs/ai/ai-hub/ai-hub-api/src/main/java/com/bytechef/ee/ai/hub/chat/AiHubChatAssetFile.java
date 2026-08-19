/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import com.bytechef.ee.ai.hub.util.EnumOrdinals;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Persistent join row linking a AI Hub chat to an {@code asset_file} the chat either authored or referenced (attached,
 * mentioned, or read via a tool call). Mothership-style "files panel" listings for a chat are computed by joining this
 * table to {@code asset_file}.
 *
 * <p>
 * <b>One-author invariant.</b> An asset_file may have at most one row with {@link AiHubChatAssetFileRelationship}
 * {@code AUTHORED} across all chats — the chat that originally produced it. The constraint is enforced at the database
 * level by the partial unique index {@code uk_asset_file_authored_once} (pinned to ordinal 0). A service-side check
 * would not survive raw-SQL writers or future code paths that bypass the service.
 *
 * <p>
 * <b>Idempotency.</b> The unique constraint on {@code (chat_id, asset_file_id, relationship)} makes inserting an
 * already-existing relationship a no-op rather than producing duplicate rows — callers can safely upsert when in doubt.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("ai_hub_chat_asset_file")
public class AiHubChatAssetFile {

    @Id
    private Long id;

    private long chatId;

    private long assetFileId;

    private int relationship;

    private LocalDateTime createdAt;

    public AiHubChatAssetFile() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public long getAssetFileId() {
        return assetFileId;
    }

    public void setAssetFileId(long assetFileId) {
        this.assetFileId = assetFileId;
    }

    public AiHubChatAssetFileRelationship getRelationship() {
        return EnumOrdinals.fromOrdinal(relationship, AiHubChatAssetFileRelationship.class);
    }

    public void setRelationship(AiHubChatAssetFileRelationship relationship) {
        this.relationship = relationship.ordinal();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        AiHubChatAssetFile that = (AiHubChatAssetFile) other;

        // Two transient (id == null) instances must not collide in a Set before persistence.
        if (id == null) {
            return this == that;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "AiHubChatAssetFile{" +
            "id=" + id +
            ", chatId=" + chatId +
            ", assetFileId=" + assetFileId +
            ", relationship=" + relationship +
            ", createdAt=" + createdAt +
            '}';
    }
}

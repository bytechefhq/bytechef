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

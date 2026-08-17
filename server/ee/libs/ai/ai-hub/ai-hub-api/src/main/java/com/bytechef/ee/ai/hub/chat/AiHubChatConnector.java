/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Whether one user-global connector participates in one chat. Availability is the user-global
 * {@link AiHubChatComponent} row (chat_id NULL); this row only says whether an available connector acts here.
 *
 * <p>
 * Absence of a row means participating, so ordinary chats carry none of these and need no backfill.
 * </p>
 *
 * <p>
 * This is deliberately NOT a chat-scoped {@link AiHubChatComponent} row, which is the obvious reuse and is wrong: that
 * entity's {@code enabled} flag is honored by {@code listUserTools} but ignored by {@code listChatTools} — chat-scoped
 * tools are always live there, because the user attached them to this chat explicitly. Overloading such a row with
 * participation would collide with the autonomous attach flow (whose {@code findByChatAndComponentIgnoringConnection}
 * lookup would rebind onto it and hang tools off it) and leave those tools live while the composer switch, reading the
 * same row, showed OFF.
 * </p>
 *
 * <p>
 * Keyed by component NAME with no version: participation is a decision about a connector, not about one pinned version
 * of it, so a version upgrade must not silently re-enable something the user switched off.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("ai_hub_chat_connector")
public class AiHubChatConnector {

    @Id
    private Long id;

    @Column("chat_id")
    private long chatId;

    @Column("component_name")
    private String componentName;

    @Column("enabled")
    private boolean enabled = true;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Version
    private int version;

    public AiHubChatConnector() {
    }

    public AiHubChatConnector(long chatId, String componentName, boolean enabled) {
        this.chatId = chatId;
        this.componentName = componentName;
        this.enabled = enabled;
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

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AiHubChatConnector that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AiHubChatConnector{" +
            "id=" + id +
            ", chatId=" + chatId +
            ", componentName='" + componentName + '\'' +
            ", enabled=" + enabled +
            ", version=" + version +
            '}';
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Per-chat binding of a ByteChef component to an optional connection. Mirrors the {@code McpComponent} shape but scopes
 * lifetime to a single AI Hub chat.
 *
 * <p>
 * One row per (chat, component, version, connection, environment) tuple — the unique constraint
 * {@code uk_ccc_conv_comp_conn_env} makes attach idempotent: re-issuing the chat command "add Slack to this chat" hits
 * the existing row instead of creating a duplicate. Tools that don't need a connection (e.g. {@code logic/v1}) hold a
 * permanently-null {@code connectionId}.
 * </p>
 *
 * <p>
 * The plain-{@code long} FK style (rather than {@link org.springframework.data.jdbc.core.mapping.AggregateReference})
 * matches the surrounding ai-hub chat domain (see {@link AiHubChatAssetFile}); McpComponent's AggregateReference style
 * isn't used here because the local module is consistent the other way and switching styles for one entity would
 * surprise readers.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("ai_hub_chat_component")
public class AiHubChatComponent {

    @Id
    private Long id;

    // Nullable: NULL marks a user-global "added connector" (scoped by userId/workspaceId) instead of a
    // chat-scoped binding. See the 20260701000001 migration.
    @Column("chat_id")
    private @Nullable Long chatId;

    @Column("user_id")
    private @Nullable Long userId;

    @Column("workspace_id")
    private @Nullable Long workspaceId;

    // Component-level on/off for the Connectors page. Defaults to enabled.
    @Column("enabled")
    private boolean enabled = true;

    @Column("component_name")
    private String componentName;

    @Column("component_version")
    private int componentVersion;

    @Column("connection_id")
    private @Nullable Long connectionId;

    @Column
    private int environment;

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

    public AiHubChatComponent() {
    }

    public AiHubChatComponent(
        long chatId, String componentName, int componentVersion, @Nullable Long connectionId,
        int environment) {

        this.chatId = chatId;
        this.componentName = componentName;
        this.componentVersion = componentVersion;
        this.connectionId = connectionId;
        this.environment = environment;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @Nullable Long getChatId() {
        return chatId;
    }

    public void setChatId(@Nullable Long chatId) {
        this.chatId = chatId;
    }

    public @Nullable Long getUserId() {
        return userId;
    }

    public void setUserId(@Nullable Long userId) {
        this.userId = userId;
    }

    public @Nullable Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(@Nullable Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public int getComponentVersion() {
        return componentVersion;
    }

    public void setComponentVersion(int componentVersion) {
        this.componentVersion = componentVersion;
    }

    public @Nullable Long getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(@Nullable Long connectionId) {
        this.connectionId = connectionId;
    }

    public int getEnvironment() {
        return environment;
    }

    public void setEnvironment(int environment) {
        this.environment = environment;
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

        if (!(o instanceof AiHubChatComponent that)) {
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
        return "AiHubChatComponent{" +
            "id=" + id +
            ", chatId=" + chatId +
            ", userId=" + userId +
            ", workspaceId=" + workspaceId +
            ", enabled=" + enabled +
            ", componentName='" + componentName + '\'' +
            ", componentVersion=" + componentVersion +
            ", connectionId=" + connectionId +
            ", environment=" + environment +
            ", version=" + version +
            '}';
    }
}

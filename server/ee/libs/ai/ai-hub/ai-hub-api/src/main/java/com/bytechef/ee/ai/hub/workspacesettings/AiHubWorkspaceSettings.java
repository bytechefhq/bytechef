/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.workspacesettings;

import java.time.LocalDateTime;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Per-workspace AI Hub configuration. Stores settings that are scoped to a single workspace's AI Hub experience and not
 * appropriate for the workflow / project domain.
 *
 * <p>
 * <b>Current scope</b>: voice webhook URL (Path A). The row exists per (workspace_id) — DB enforces uniqueness via
 * {@code uk_ai_hub_workspace_settings_workspace_id}. The service exposes upsert semantics so callers don't worry about
 * whether a row already exists.
 *
 * <p>
 * <b>Why a dedicated table</b> rather than tacking onto an existing workspace_* table: AI Hub config is governed
 * separately from project/workflow settings (different admin surface, different feature flags) and bundling them would
 * complicate the admin authorization rules. Per the project's workspace_id-placement convention, AI Hub is an
 * automation-package feature so {@code workspaceId} lives directly on the entity (no relation table).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("ai_hub_workspace_settings")
public class AiHubWorkspaceSettings {

    @Id
    private Long id;

    @Column("workspace_id")
    private long workspaceId;

    /**
     * Path A — base URL of the browser-voice webhook the AI Hub composer uses for voice sessions. Format:
     * {@code https://host/api/automation/webhooks/<webhookId>}. Null = Path A not configured.
     */
    @Column("voice_webhook_url")
    private @Nullable String voiceWebhookUrl;

    @CreatedDate
    @Column("created_date")
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column("last_modified_date")
    private LocalDateTime lastModifiedDate;

    @Version
    private int version;

    public AiHubWorkspaceSettings() {
    }

    public AiHubWorkspaceSettings(long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public @Nullable String getVoiceWebhookUrl() {
        return voiceWebhookUrl;
    }

    public void setVoiceWebhookUrl(@Nullable String voiceWebhookUrl) {
        this.voiceWebhookUrl = voiceWebhookUrl;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getLastModifiedDate() {
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

        if (!(o instanceof AiHubWorkspaceSettings that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_observability_notification_channel")
public class AiObservabilityNotificationChannel {

    @Column
    private String config;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column
    private boolean enabled;

    @Id
    private Long id;

    @Column("last_error")
    private String lastError;

    @Column("last_error_date")
    private Instant lastErrorDate;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column
    private String name;

    @Column
    private int type;

    @Version
    private int version;

    @Column("workspace_id")
    private Long workspaceId;

    private AiObservabilityNotificationChannel() {
    }

    public AiObservabilityNotificationChannel(
        Long workspaceId, String name, AiObservabilityNotificationChannelType type, String config) {

        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notBlank(name, "name must not be blank");
        Validate.notNull(type, "type must not be null");
        Validate.notBlank(config, "config must not be blank");

        this.config = config;
        this.enabled = true;
        this.name = name;
        this.type = type.ordinal();
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiObservabilityNotificationChannel aiObservabilityNotificationChannel)) {
            return false;
        }

        return Objects.equals(id, aiObservabilityNotificationChannel.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public String getConfig() {
        return config;
    }

    /**
     * Typed view of the channel-specific config JSON. The dispatcher switch on {@link #getType()} maps cleanly onto the
     * sealed {@link AiObservabilityNotificationChannelConfig} subtypes so a missing per-type branch becomes a
     * compile-time error instead of a runtime "unsupported type" fallthrough.
     */
    public AiObservabilityNotificationChannelConfig getTypedConfig() {
        return AiObservabilityNotificationChannelConfig.fromJson(config);
    }

    public void setTypedConfig(AiObservabilityNotificationChannelConfig typedConfig) {
        Validate.notNull(typedConfig, "typedConfig must not be null");

        if (typedConfig.channelType()
            .ordinal() != this.type) {

            throw new IllegalArgumentException(
                "typedConfig channelType " + typedConfig.channelType() + " must match channel type " + getType());
        }

        this.config = AiObservabilityNotificationChannelConfig.toJson(typedConfig);
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Long getId() {
        return id;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getLastErrorDate() {
        return lastErrorDate;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getName() {
        return name;
    }

    public AiObservabilityNotificationChannelType getType() {
        return AiObservabilityNotificationChannelType.values()[type];
    }

    public int getVersion() {
        return version;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setConfig(String config) {
        Validate.notBlank(config, "config must not be blank");

        // Round-trip through the sealed AiObservabilityNotificationChannelConfig hierarchy so the per-variant
        // invariants (webhook url non-blank, email recipients non-empty, Slack XOR bot/webhook) are validated at
        // write time. This prevents a malformed config row from making it to the dispatcher, where the only
        // feedback is a generic runtime error while admins see the channel as "healthy".
        AiObservabilityNotificationChannelConfig parsed = AiObservabilityNotificationChannelConfig.fromJson(config);

        if (parsed.channelType()
            .ordinal() != this.type) {

            throw new IllegalArgumentException(
                "config channelType " + parsed.channelType() + " must match channel type " + getType());
        }

        this.config = AiObservabilityNotificationChannelConfig.toJson(parsed);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Records a delivery failure on the channel so operators can see which integrations are broken. Clear by passing
     * {@code null} after a successful delivery.
     */
    public void setLastError(String lastError, Instant lastErrorDate) {
        this.lastError = lastError;
        this.lastErrorDate = lastErrorDate;
    }

    public void setName(String name) {
        Validate.notBlank(name, "name must not be blank");

        this.name = name;
    }

    /**
     * Changes the channel type atomically with its config so a WEBHOOK config JSON cannot coexist with type SLACK. Use
     * this instead of {@link #setType} + {@link #setTypedConfig}, which could leave the row in a partial state between
     * calls.
     */
    public void setTypeAndConfig(
        AiObservabilityNotificationChannelType type, AiObservabilityNotificationChannelConfig typedConfig) {

        Validate.notNull(type, "type must not be null");
        Validate.notNull(typedConfig, "typedConfig must not be null");

        if (typedConfig.channelType() != type) {
            throw new IllegalArgumentException(
                "typedConfig channelType " + typedConfig.channelType() + " must match supplied type " + type);
        }

        this.type = type.ordinal();
        this.config = AiObservabilityNotificationChannelConfig.toJson(typedConfig);
    }

    public void setType(AiObservabilityNotificationChannelType type) {
        Validate.notNull(type, "type must not be null");

        if (this.config != null) {
            AiObservabilityNotificationChannelConfig parsed = AiObservabilityNotificationChannelConfig.fromJson(config);

            if (parsed.channelType() != type) {
                throw new IllegalArgumentException(
                    "Cannot change type to " + type + " while config is for " + parsed.channelType()
                        + " — use setTypeAndConfig to change both atomically");
            }
        }

        this.type = type.ordinal();
    }

    @Override
    public String toString() {
        return "AiObservabilityNotificationChannel{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", name='" + name + '\'' +
            ", type=" + getType() +
            ", enabled=" + enabled +
            '}';
    }
}

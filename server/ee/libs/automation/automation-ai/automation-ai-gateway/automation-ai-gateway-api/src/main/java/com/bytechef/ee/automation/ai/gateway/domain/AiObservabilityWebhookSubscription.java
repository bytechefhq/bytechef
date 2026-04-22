/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import com.bytechef.commons.data.jdbc.wrapper.EncryptedStringWrapper;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;
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
@Table("ai_observability_webhook_subscription")
public class AiObservabilityWebhookSubscription {

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column
    private boolean enabled;

    @Column
    private String events;

    @Id
    private Long id;

    @Column("last_delivery_error")
    private String lastDeliveryError;

    @Column("last_delivery_error_date")
    private Instant lastDeliveryErrorDate;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column("last_triggered_date")
    private Instant lastTriggeredDate;

    @Column
    private String name;

    @Column("project_id")
    private Long projectId;

    @Column
    private EncryptedStringWrapper secret;

    @Column
    private String url;

    @Version
    private int version;

    @Column("workspace_id")
    private Long workspaceId;

    private AiObservabilityWebhookSubscription() {
    }

    public AiObservabilityWebhookSubscription(Long workspaceId, String name, String url, String events) {
        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notNull(name, "name must not be null");
        Validate.notNull(url, "url must not be null");
        Validate.notNull(events, "events must not be null");

        this.enabled = true;
        this.events = events;
        this.name = name;
        this.url = url;
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiObservabilityWebhookSubscription aiObservabilityWebhookSubscription)) {
            return false;
        }

        return Objects.equals(id, aiObservabilityWebhookSubscription.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getEvents() {
        return events;
    }

    /**
     * Typed view of the events JSON array. An unknown wire name throws at parse time rather than producing a
     * subscription that silently never fires.
     */
    public List<AiObservabilityWebhookEventType> getTypedEvents() {
        return AiObservabilityWebhookEventType.parseList(events);
    }

    public void setTypedEvents(List<AiObservabilityWebhookEventType> events) {
        this.events = AiObservabilityWebhookEventType.toJson(events);
    }

    public Long getId() {
        return id;
    }

    public String getLastDeliveryError() {
        return lastDeliveryError;
    }

    public Instant getLastDeliveryErrorDate() {
        return lastDeliveryErrorDate;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Instant getLastTriggeredDate() {
        return lastTriggeredDate;
    }

    public String getName() {
        return name;
    }

    public Long getProjectId() {
        return projectId;
    }

    /**
     * Returns the decrypted webhook HMAC secret. This is a plaintext-leak surface — grep for {@code getSecret} to
     * enumerate callers. Currently read only by {@code AiObservabilityWebhookDeliveryServiceImpl} for HMAC signing. Not
     * serialized to JSON responses.
     */
    @JsonIgnore
    public String getSecret() {
        return secret == null ? null : secret.getValue();
    }

    public String getUrl() {
        return url;
    }

    public int getVersion() {
        return version;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setEvents(String events) {
        if (events != null && !events.isBlank()) {
            AiObservabilityWebhookEventType.parseList(events);
        }

        this.events = events;
    }

    /**
     * Records the most recent delivery failure so operators see broken subscriptions in the UI instead of having to
     * trawl logs. Pass {@code null}/{@code null} after a successful delivery to clear.
     */
    public void setLastDeliveryError(String lastDeliveryError, Instant lastDeliveryErrorDate) {
        this.lastDeliveryError = lastDeliveryError;
        this.lastDeliveryErrorDate = lastDeliveryErrorDate;
    }

    public void setLastTriggeredDate(Instant lastTriggeredDate) {
        this.lastTriggeredDate = lastTriggeredDate;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setSecret(String secret) {
        this.secret = secret == null ? null : new EncryptedStringWrapper(secret);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "AiObservabilityWebhookSubscription{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", name='" + name + '\'' +
            ", url='" + url + '\'' +
            ", enabled=" + enabled +
            ", createdDate=" + createdDate +
            '}';
    }
}

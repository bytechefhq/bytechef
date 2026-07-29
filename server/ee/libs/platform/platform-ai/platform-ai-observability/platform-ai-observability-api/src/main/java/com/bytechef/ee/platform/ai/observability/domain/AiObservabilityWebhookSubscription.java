/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.domain;

import com.bytechef.commons.data.jdbc.wrapper.EncryptedStringWrapper;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Persisted webhook subscription for AI-observability events. The {@link #lastDeliveryError} column is capped at
 * {@value #LAST_DELIVERY_ERROR_MAX_LENGTH} characters in the schema (Liquibase: VARCHAR(2000)); the
 * {@link #setLastDeliveryError(String, Instant)} setter truncates longer payloads at the type level so a delivery with
 * a verbose upstream error never blows the column constraint and rolls back the subscription update. Truncation
 * preserves the head of the message (where the actionable cause normally lives) and appends a short ellipsis marker so
 * operators can see the message was clipped instead of mistaking the cap for the real terminus.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Table("ai_observability_webhook_subscription")
public class AiObservabilityWebhookSubscription {

    /**
     * Maximum stored length of {@link #lastDeliveryError} — must match the {@code VARCHAR} cap in the Liquibase
     * changelog ({@code 00000000000011_ai_observability_webhook_subscription_add_last_delivery_error.xml}). Drift
     * between the constant and the schema would re-introduce the rollback-on-overflow bug the truncation guards
     * against.
     */
    static final int LAST_DELIVERY_ERROR_MAX_LENGTH = 2000;

    private static final String LAST_DELIVERY_ERROR_TRUNCATION_MARKER = "…[truncated]";

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column
    private boolean enabled;

    @Column
    private String events;

    @Id
    private Long id;

    /**
     * Free-form summary of the most recent webhook delivery failure (typically the output of
     * {@code AiGatewayThrowables.summarize(...)} on the underlying transport / HMAC / response-parse exception). Schema
     * cap: {@value #LAST_DELIVERY_ERROR_MAX_LENGTH} characters; {@link #setLastDeliveryError(String, Instant)}
     * truncates oversized inputs in-process so the column constraint never rolls back the surrounding update. Not
     * sanitized further — the upstream summarizer already strips body content; downstream code that surfaces this field
     * in the UI must treat it as untrusted operator-readable text.
     */
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

    /**
     * The owning workspace, or {@code null} when the subscription belongs to no workspace. Boxed on purpose — a
     * primitive would coerce the "no workspace" case into workspace {@code 0}, which is a real workspace id.
     */
    @Column("workspace_id")
    private @Nullable Long workspaceId;

    private AiObservabilityWebhookSubscription() {
    }

    public AiObservabilityWebhookSubscription(String name, String url, String events) {
        Validate.notNull(name, "name must not be null");
        Validate.notNull(url, "url must not be null");
        Validate.notNull(events, "events must not be null");

        this.enabled = true;
        this.events = events;
        this.name = name;
        this.url = url;
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

    public @Nullable Long getWorkspaceId() {
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
     *
     * <p>
     * Inputs longer than {@value #LAST_DELIVERY_ERROR_MAX_LENGTH} characters are truncated to that cap (preserving the
     * head of the message and appending an ellipsis marker), matching the column's {@code VARCHAR(2000)} schema.
     * Without truncation a verbose upstream error (e.g., a multi-kilobyte stack-frame summary or a large response body
     * echoed by a misconfigured proxy) would overflow the column and roll back the subscription update, leaving the
     * operator with no visible failure record at all.
     */
    public void setLastDeliveryError(String lastDeliveryError, Instant lastDeliveryErrorDate) {
        this.lastDeliveryError = truncateLastDeliveryError(lastDeliveryError);
        this.lastDeliveryErrorDate = lastDeliveryErrorDate;
    }

    private static String truncateLastDeliveryError(String message) {
        if (message == null || message.length() <= LAST_DELIVERY_ERROR_MAX_LENGTH) {
            return message;
        }

        int headLength = LAST_DELIVERY_ERROR_MAX_LENGTH - LAST_DELIVERY_ERROR_TRUNCATION_MARKER.length();

        return message.substring(0, headLength) + LAST_DELIVERY_ERROR_TRUNCATION_MARKER;
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

    public void setWorkspaceId(@Nullable Long workspaceId) {
        this.workspaceId = workspaceId;
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

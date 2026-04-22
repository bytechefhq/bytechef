/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.domain;

import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Workspace ↔ {@link AiObservabilityWebhookSubscription} membership row.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("workspace_ai_observability_webhook_subscription")
public class WorkspaceAiObservabilityWebhookSubscription {

    @Column("ai_observability_webhook_subscription_id")
    private Long aiObservabilityWebhookSubscriptionId;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Id
    private Long id;

    @Column("last_modified_by")
    @LastModifiedBy
    private String lastModifiedBy;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Version
    private int version;

    @Column("workspace_id")
    private Long workspaceId;

    private WorkspaceAiObservabilityWebhookSubscription() {
    }

    public WorkspaceAiObservabilityWebhookSubscription(
        Long aiObservabilityWebhookSubscriptionId, Long workspaceId) {

        Validate.notNull(
            aiObservabilityWebhookSubscriptionId, "aiObservabilityWebhookSubscriptionId must not be null");
        Validate.notNull(workspaceId, "workspaceId must not be null");

        this.aiObservabilityWebhookSubscriptionId = aiObservabilityWebhookSubscriptionId;
        this.workspaceId = workspaceId;
    }

    public Long getAiObservabilityWebhookSubscriptionId() {
        return aiObservabilityWebhookSubscriptionId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Long getId() {
        return id;
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

    public Long getWorkspaceId() {
        return workspaceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof WorkspaceAiObservabilityWebhookSubscription that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "WorkspaceAiObservabilityWebhookSubscription{" +
            "id=" + id +
            ", aiObservabilityWebhookSubscriptionId=" + aiObservabilityWebhookSubscriptionId +
            ", workspaceId=" + workspaceId +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}

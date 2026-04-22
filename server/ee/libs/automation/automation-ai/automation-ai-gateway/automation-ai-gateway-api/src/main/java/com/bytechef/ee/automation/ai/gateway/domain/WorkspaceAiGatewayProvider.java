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
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Domain class representing the relationship between a workspace and an AI LLM Gateway provider.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("workspace_ai_gateway_provider")
public class WorkspaceAiGatewayProvider {

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

    @Column("provider_id")
    private Long providerId;

    @Version
    private int version;

    @Column("workspace_id")
    private Long workspaceId;

    private WorkspaceAiGatewayProvider() {
    }

    public WorkspaceAiGatewayProvider(Long providerId, Long workspaceId) {
        Validate.notNull(providerId, "providerId must not be null");
        Validate.notNull(workspaceId, "workspaceId must not be null");

        this.providerId = providerId;
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof WorkspaceAiGatewayProvider workspaceAiGatewayProvider)) {
            return false;
        }

        return Objects.equals(id, workspaceAiGatewayProvider.id);
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

    public Long getProviderId() {
        return providerId;
    }

    public int getVersion() {
        return version;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "WorkspaceAiGatewayProvider{" +
            "id=" + id +
            ", providerId=" + providerId +
            ", workspaceId=" + workspaceId +
            ", createdBy='" + createdBy + '\'' +
            ", createdDate=" + createdDate +
            ", lastModifiedBy='" + lastModifiedBy + '\'' +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}

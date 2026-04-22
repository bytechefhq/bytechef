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
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_observability_session")
public class AiObservabilitySession {

    @CreatedDate
    private Instant createdDate;

    @Id
    private Long id;

    @LastModifiedDate
    private Instant lastModifiedDate;

    private String name;

    private String externalSessionId;

    private Long projectId;

    private String userId;

    @Version
    private int version;

    private Long workspaceId;

    private AiObservabilitySession() {
    }

    public AiObservabilitySession(Long workspaceId) {
        Validate.notNull(workspaceId, "workspaceId must not be null");

        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiObservabilitySession aiObservabilitySession)) {
            return false;
        }

        return Objects.equals(id, aiObservabilitySession.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Long getId() {
        return id;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getName() {
        return name;
    }

    public String getExternalSessionId() {
        return externalSessionId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getUserId() {
        return userId;
    }

    public int getVersion() {
        return version;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setExternalSessionId(String externalSessionId) {
        this.externalSessionId = externalSessionId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "AiObservabilitySession{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", name='" + name + '\'' +
            ", userId='" + userId + '\'' +
            ", createdDate=" + createdDate +
            '}';
    }
}

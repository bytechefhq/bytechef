/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.domain;

import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A session belongs to at most one workspace: the association is the nullable {@code workspace_id} column, which is
 * null where no workspace applies. Callers that only hold an id go through
 * {@code WorkspaceAiObservabilitySessionService.getWorkspaceId(id)}.
 *
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

    /**
     * The owning workspace, or {@code null} when the session belongs to no workspace. Boxed on purpose — a primitive
     * would coerce the "no workspace" case into workspace {@code 0}, which is a real workspace id.
     */
    private @Nullable Long workspaceId;

    public AiObservabilitySession() {
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

    public @Nullable Long getWorkspaceId() {
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

    public void setWorkspaceId(@Nullable Long workspaceId) {
        this.workspaceId = workspaceId;
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

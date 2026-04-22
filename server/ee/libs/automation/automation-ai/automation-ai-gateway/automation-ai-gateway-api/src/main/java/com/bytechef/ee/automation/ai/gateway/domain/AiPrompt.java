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
@Table("ai_prompt")
public class AiPrompt {

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column
    private String description;

    @Id
    private Long id;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column
    private String name;

    @Column("project_id")
    private Long projectId;

    @Version
    private int version;

    @Column("workspace_id")
    private Long workspaceId;

    private AiPrompt() {
    }

    public AiPrompt(Long workspaceId, String name) {
        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notBlank(name, "name must not be blank");

        this.name = name;
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiPrompt aiPrompt)) {
            return false;
        }

        return Objects.equals(id, aiPrompt.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getDescription() {
        return description;
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

    public Long getProjectId() {
        return projectId;
    }

    public int getVersion() {
        return version;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setName(String name) {
        Validate.notBlank(name, "name must not be blank");

        this.name = name;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    @Override
    public String toString() {
        return "AiPrompt{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", name='" + name + '\'' +
            ", createdDate=" + createdDate +
            '}';
    }
}

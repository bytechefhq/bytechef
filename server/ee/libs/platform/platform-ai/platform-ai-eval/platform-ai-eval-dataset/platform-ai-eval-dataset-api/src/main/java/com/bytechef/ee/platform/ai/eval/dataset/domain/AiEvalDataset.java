/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.domain;

import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
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
 * @author Ivica Cardic
 * @version ee
 */
@Table("ai_eval_dataset")
public final class AiEvalDataset {

    @Column("archived_date")
    private Instant archivedDate;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate;

    private String description;

    @Id
    private Long id;

    @LastModifiedBy
    @Column("last_modified_by")
    private String lastModifiedBy;

    @LastModifiedDate
    @Column("last_modified_date")
    private Instant lastModifiedDate;

    private String name;

    @Column("project_id")
    private Long projectId;

    private String tags;

    @Version
    private int version;

    @Column("workspace_id")
    private @Nullable Long workspaceId;

    private AiEvalDataset() {
    }

    public AiEvalDataset(String name) {
        Validate.isTrue(StringUtils.isNotBlank(name), "name must not be blank");

        this.name = name;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiEvalDataset aiEvalDataset)) {
            return false;
        }

        return Objects.equals(id, aiEvalDataset.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public Instant getArchivedDate() {
        return archivedDate;
    }

    public String getCreatedBy() {
        return createdBy;
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

    public String getLastModifiedBy() {
        return lastModifiedBy;
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

    public String getTags() {
        return tags;
    }

    public int getVersion() {
        return version;
    }

    public @Nullable Long getWorkspaceId() {
        return workspaceId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setName(String name) {
        Validate.isTrue(StringUtils.isNotBlank(name), "name must not be blank");

        this.name = name;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public void setWorkspaceId(@Nullable Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public void archive() {
        if (archivedDate == null) {
            this.archivedDate = Instant.now();
        }
    }

    public void unarchive() {
        if (archivedDate != null) {
            this.archivedDate = null;
        }
    }

    public boolean isArchived() {
        return archivedDate != null;
    }

    void setArchivedDate(Instant archivedDate) {
        this.archivedDate = archivedDate;
    }

    @Override
    public String toString() {
        return "AiEvalDataset{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", projectId=" + projectId +
            ", name='" + name + '\'' +
            ", archivedDate=" + archivedDate +
            ", version=" + version +
            '}';
    }
}

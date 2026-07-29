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
 * Workspace-scoped dataset grouping {@link AiEvalDatasetVersion} snapshots and their {@link AiEvalDatasetItem} rows.
 * {@code tags} stores raw JSON; decoding is deferred to the service layer.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Table("ai_eval_dataset")
public class AiEvalDataset {

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

    /**
     * The owning workspace, or {@code null} when the dataset belongs to no workspace. Boxed on purpose — a primitive
     * would coerce the "no workspace" case into workspace {@code 0}, which is a real workspace id.
     */
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

    /**
     * Marks this dataset as archived (soft delete). Idempotent — calling on an already-archived dataset is a no-op
     * preserving the original archive timestamp, matching {@link AiEvalDatasetVersion#freeze()}'s one-way idempotent
     * lifecycle pattern. Public mutation of {@code archivedDate} is forbidden via the package-private setter so callers
     * cannot manually reach back in time or unset the archive timestamp without going through {@link #unarchive()},
     * which has its own audit-relevant semantics.
     */
    public void archive() {
        if (archivedDate == null) {
            this.archivedDate = Instant.now();
        }
    }

    /**
     * Restores an archived dataset. Idempotent on an unarchived dataset — the archivedDate write is guarded so calling
     * unarchive() on a non-archived dataset is a true no-op (no field write, no dirty marker for the next save).
     * Without this guard, the symmetry the doc claims with {@link #archive()} was illusory: an audit listener wired to
     * the archivedDate column would have fired on every redundant unarchive call as if a transition occurred. Distinct
     * from {@link #archive()} so audit appenders / event listeners can fire on the explicit lifecycle transition rather
     * than inferring it from a setArchivedDate(null) write.
     */
    public void unarchive() {
        if (archivedDate != null) {
            this.archivedDate = null;
        }
    }

    public boolean isArchived() {
        return archivedDate != null;
    }

    /**
     * Package-private. Direct mutation of {@code archivedDate} bypasses the {@link #archive()} / {@link #unarchive()}
     * lifecycle pair — application callers must use those methods so audit listeners observe the explicit transition.
     * Kept for Spring Data JDBC reflective hydration only.
     */
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

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.experiment.domain;

import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Table("ai_eval_experiment")
public final class AiEvalExperiment {

    private static final AiEvalExperimentStatus[] STATUS_VALUES = AiEvalExperimentStatus.values();

    @Column("completed_date")
    private Instant completedDate;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate;

    @Column("dataset_version_id")
    private Long datasetVersionId;

    @Id
    private Long id;

    private String metadata;

    private String model;

    @Column("prompt_version_id")
    private Long promptVersionId;

    @Column("started_date")
    private Instant startedDate;

    private int status;

    @Column("stop_requested")
    private boolean stopRequested;

    @Column("stop_requested_date")
    private Instant stopRequestedDate;

    @Column("workspace_id")
    private @Nullable Long workspaceId;

    private AiEvalExperiment() {
    }

    public AiEvalExperiment(Long datasetVersionId) {
        Validate.notNull(datasetVersionId, "datasetVersionId must not be null");

        this.datasetVersionId = datasetVersionId;
        this.status = AiEvalExperimentStatus.PENDING.ordinal();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiEvalExperiment aiEvalExperiment)) {
            return false;
        }

        return Objects.equals(id, aiEvalExperiment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public Instant getCompletedDate() {
        return completedDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Long getDatasetVersionId() {
        return datasetVersionId;
    }

    public Long getId() {
        return id;
    }

    public String getMetadata() {
        return metadata;
    }

    public String getModel() {
        return model;
    }

    public Long getPromptVersionId() {
        return promptVersionId;
    }

    public Instant getStartedDate() {
        return startedDate;
    }

    public AiEvalExperimentStatus getStatus() {
        if (status < 0 || status >= STATUS_VALUES.length) {
            throw new IllegalStateException(
                "AiEvalExperiment id=" + id + " has corrupt status ordinal " + status +
                    " (valid range 0.." + (STATUS_VALUES.length - 1) + ")");
        }

        return STATUS_VALUES[status];
    }

    public Instant getStopRequestedDate() {
        return stopRequestedDate;
    }

    public @Nullable Long getWorkspaceId() {
        return workspaceId;
    }

    public boolean isStopRequested() {
        return stopRequested;
    }

    public void setDatasetVersionId(Long datasetVersionId) {
        assertPending("datasetVersionId");

        this.datasetVersionId = datasetVersionId;
    }

    public void setMetadata(String metadata) {
        assertPending("metadata");

        this.metadata = metadata;
    }

    public void setModel(String model) {
        assertPending("model");

        this.model = model;
    }

    public void setPromptVersionId(Long promptVersionId) {
        assertPending("promptVersionId");

        this.promptVersionId = promptVersionId;
    }

    public void setWorkspaceId(@Nullable Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    private void assertPending(String fieldName) {
        if (this.status != AiEvalExperimentStatus.PENDING.ordinal()) {
            throw new IllegalStateException(
                "AiEvalExperiment id=" + id + " is in state " + getStatus() + "; cannot mutate '" + fieldName +
                    "' once the experiment has started");
        }
    }

    void setStopRequested(boolean stopRequested) {
        this.stopRequested = stopRequested;
    }

    void setStatus(AiEvalExperimentStatus status) {
        Validate.notNull(status, "status must not be null");

        this.status = status.ordinal();
    }

    void setStartedDate(Instant startedDate) {
        this.startedDate = startedDate;
    }

    void setCompletedDate(Instant completedDate) {
        this.completedDate = completedDate;
    }

    void setStopRequestedDate(Instant stopRequestedDate) {
        this.stopRequestedDate = stopRequestedDate;
    }

    public void start() {
        if (getStatus() != AiEvalExperimentStatus.PENDING) {
            throw new IllegalStateException("Cannot start experiment from " + getStatus());
        }

        this.status = AiEvalExperimentStatus.RUNNING.ordinal();
        this.startedDate = Instant.now();
    }

    public void complete() {
        if (getStatus() != AiEvalExperimentStatus.RUNNING) {
            throw new IllegalStateException("Cannot complete experiment from " + getStatus());
        }

        if (stopRequested) {
            throw new IllegalStateException(
                "Cannot complete experiment " + id + " — stop was requested at " + stopRequestedDate +
                    "; converge to FAILED via fail() instead");
        }

        this.status = AiEvalExperimentStatus.COMPLETED.ordinal();
        this.completedDate = Instant.now();
    }

    public void fail() {
        AiEvalExperimentStatus current = getStatus();

        if (current != AiEvalExperimentStatus.PENDING && current != AiEvalExperimentStatus.RUNNING) {
            throw new IllegalStateException("Cannot fail experiment from " + current);
        }

        this.status = AiEvalExperimentStatus.FAILED.ordinal();
        this.completedDate = Instant.now();
    }

    public void requestStop() {
        this.stopRequested = true;
        this.stopRequestedDate = Instant.now();
    }

    @Override
    public String toString() {
        return "AiEvalExperiment{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", datasetVersionId=" + datasetVersionId +
            ", promptVersionId=" + promptVersionId +
            ", model='" + model + '\'' +
            ", status=" + getStatus() +
            ", startedDate=" + startedDate +
            ", completedDate=" + completedDate +
            '}';
    }
}

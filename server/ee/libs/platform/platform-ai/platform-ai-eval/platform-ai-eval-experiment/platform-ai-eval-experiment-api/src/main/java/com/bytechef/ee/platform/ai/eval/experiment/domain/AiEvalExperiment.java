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
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Workspace-scoped batch that replays a {@link com.bytechef.ee.platform.ai.eval.dataset.domain.AiEvalDatasetVersion}
 * through a model (optionally pinned to a prompt version) and produces per-item {@link AiEvalExperimentRun} rows.
 * {@code metadata} stores raw JSON; decoding is deferred to the service layer.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Table("ai_eval_experiment")
public class AiEvalExperiment {

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

    /**
     * The instant {@link #requestStop()} was called. Note this is the moment the user signalled the interrupt — the
     * experiment may continue running for one more iteration boundary before the executor observes the flag and
     * converges to {@link AiEvalExperimentStatus#FAILED}. The terminal-transition timestamp is
     * {@link #getCompletedDate()}.
     */
    public Instant getStopRequestedDate() {
        return stopRequestedDate;
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

    /**
     * Guards configuration setters: once the experiment has left {@link AiEvalExperimentStatus#PENDING}, mutating
     * {@code datasetVersionId} / {@code model} / {@code promptVersionId} / {@code metadata} silently invalidates every
     * already-recorded {@link AiEvalExperimentRun}. Reject so the data stays internally consistent. Spring Data JDBC
     * hydration calls these setters during read-back; that path passes because the in-flight entity has not yet had any
     * FSM transition applied, so {@code status} is whatever the row carried (PENDING for newly created rows). For
     * non-PENDING rows hydrated from the DB, callers must not mutate config fields anyway — recreate the experiment row
     * instead.
     */
    private void assertPending(String fieldName) {
        if (this.status != AiEvalExperimentStatus.PENDING.ordinal()) {
            throw new IllegalStateException(
                "AiEvalExperiment id=" + id + " is in state " + getStatus() + "; cannot mutate '" + fieldName +
                    "' once the experiment has started");
        }
    }

    /**
     * Package-private. {@code stopRequested} pairs with {@code stopRequestedDate} via {@link #requestStop()} — exposing
     * a public boolean setter would let callers desync the pair (e.g., {@code stopRequested=true} with
     * {@code stopRequestedDate=null}, breaking the "request was timestamped" invariant the orphan reaper relies on).
     * Kept package-visible for Spring Data JDBC reflective hydration on read.
     */
    void setStopRequested(boolean stopRequested) {
        this.stopRequested = stopRequested;
    }

    /**
     * Package-private. Status is owned by the FSM methods ({@link #start()}, {@link #complete()}, {@link #fail()}); a
     * direct setter would let callers bypass the legal-transition check. Kept package-visible for Spring Data JDBC
     * reflective hydration on read.
     */
    void setStatus(AiEvalExperimentStatus status) {
        Validate.notNull(status, "status must not be null");

        this.status = status.ordinal();
    }

    /**
     * Package-private. Lifecycle dates are stamped by the FSM methods alongside the corresponding status transition; an
     * external setter would let the date drift out of sync with status.
     */
    void setStartedDate(Instant startedDate) {
        this.startedDate = startedDate;
    }

    /**
     * Package-private. Lifecycle dates are stamped by the FSM methods alongside the corresponding status transition; an
     * external setter would let the date drift out of sync with status.
     */
    void setCompletedDate(Instant completedDate) {
        this.completedDate = completedDate;
    }

    /**
     * Package-private. {@code stopRequestedDate} is stamped by {@link #requestStop()} when the user signals an
     * interrupt; an external setter would let the field drift out of sync with the {@code stopRequested} flag.
     */
    void setStopRequestedDate(Instant stopRequestedDate) {
        this.stopRequestedDate = stopRequestedDate;
    }

    /**
     * Marks the experiment as running and stamps {@code startedDate} with the current instant.
     *
     * @throws IllegalStateException if the experiment is not currently {@link AiEvalExperimentStatus#PENDING}.
     */
    public void start() {
        if (getStatus() != AiEvalExperimentStatus.PENDING) {
            throw new IllegalStateException("Cannot start experiment from " + getStatus());
        }

        this.status = AiEvalExperimentStatus.RUNNING.ordinal();
        this.startedDate = Instant.now();
    }

    /**
     * Marks the experiment as completed and stamps {@code completedDate} with the current instant.
     *
     * <p>
     * Rejects when a stop has been requested: the executor should converge the experiment to
     * {@link AiEvalExperimentStatus#FAILED} via {@link #fail()} once it observes the flag, not silently complete it. A
     * {@code stopRequested=true} co-existing with {@code status=COMPLETED} would lie to operators about whether the
     * experiment finished naturally or was stopped.
     *
     * @throws IllegalStateException if the experiment is not currently {@link AiEvalExperimentStatus#RUNNING} or if a
     *                               stop was requested.
     */
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

    /**
     * Marks the experiment as failed and stamps {@code completedDate} with the current instant. Permitted from
     * {@link AiEvalExperimentStatus#PENDING} as well as {@link AiEvalExperimentStatus#RUNNING} to support the T2.13
     * markRunning-failure path where the executor must converge a still-PENDING experiment to FAILED.
     *
     * @throws IllegalStateException if the experiment is already in a terminal state.
     */
    public void fail() {
        AiEvalExperimentStatus current = getStatus();

        if (current != AiEvalExperimentStatus.PENDING && current != AiEvalExperimentStatus.RUNNING) {
            throw new IllegalStateException("Cannot fail experiment from " + current);
        }

        this.status = AiEvalExperimentStatus.FAILED.ordinal();
        this.completedDate = Instant.now();
    }

    /**
     * Flags the experiment to stop at its next iteration boundary and stamps {@code stopRequestedDate} with the moment
     * the request was made. Does NOT transition status — the executor will converge the experiment to
     * {@link AiEvalExperimentStatus#FAILED} when it observes the flag on its next check, at which point
     * {@link #getCompletedDate()} carries the actual terminal timestamp.
     */
    public void requestStop() {
        this.stopRequested = true;
        this.stopRequestedDate = Instant.now();
    }

    @Override
    public String toString() {
        return "AiEvalExperiment{" +
            "id=" + id +
            ", datasetVersionId=" + datasetVersionId +
            ", promptVersionId=" + promptVersionId +
            ", model='" + model + '\'' +
            ", status=" + getStatus() +
            ", startedDate=" + startedDate +
            ", completedDate=" + completedDate +
            '}';
    }
}

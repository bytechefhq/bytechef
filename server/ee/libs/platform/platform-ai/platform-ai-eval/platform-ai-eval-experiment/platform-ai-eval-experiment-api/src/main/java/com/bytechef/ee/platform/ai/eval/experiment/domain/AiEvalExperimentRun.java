/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.experiment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A single replay of one dataset item within an {@link AiEvalExperiment}. {@code traceId} is nullable until the replay
 * produces a trace; {@code latencyMs}, {@code cost}, and {@code errorMessage} are populated when the run reaches a
 * terminal state.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Table("ai_eval_experiment_run")
public final class AiEvalExperimentRun {

    private static final AiEvalExperimentRunStatus[] STATUS_VALUES = AiEvalExperimentRunStatus.values();

    private BigDecimal cost;

    @CreatedDate
    @Column("created_date")
    private Instant createdDate;

    @Column("dataset_item_id")
    private Long datasetItemId;

    @Column("error_message")
    private String errorMessage;

    @Column("experiment_id")
    private Long experimentId;

    @Id
    private Long id;

    @Column("latency_ms")
    private Integer latencyMs;

    private int status;

    @Column("trace_id")
    private Long traceId;

    private AiEvalExperimentRun() {
    }

    public AiEvalExperimentRun(Long experimentId, Long datasetItemId) {
        Validate.notNull(experimentId, "experimentId must not be null");
        Validate.notNull(datasetItemId, "datasetItemId must not be null");

        this.experimentId = experimentId;
        this.datasetItemId = datasetItemId;
        this.status = AiEvalExperimentRunStatus.PENDING.ordinal();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiEvalExperimentRun aiEvalExperimentRun)) {
            return false;
        }

        return Objects.equals(id, aiEvalExperimentRun.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public BigDecimal getCost() {
        return cost;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Long getDatasetItemId() {
        return datasetItemId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Long getExperimentId() {
        return experimentId;
    }

    public Long getId() {
        return id;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public AiEvalExperimentRunStatus getStatus() {
        // Defensive ordinal lookup mirrors the discipline applied to AiObservabilitySpan / AiEvalScore /
        // AiEvalExecution / AiEvalExperiment. A corrupted column (caused by enum reordering or a manual DB edit)
        // surfaces as a precise diagnostic with the row id and field name rather than ArrayIndexOutOfBoundsException
        // from a raw values[ordinal] access.
        if (status < 0 || status >= STATUS_VALUES.length) {
            throw new IllegalStateException(
                "AiEvalExperimentRun id=" + id + " has corrupt status ordinal " + status +
                    " (valid range 0.." + (STATUS_VALUES.length - 1) + ")");
        }

        return STATUS_VALUES[status];
    }

    public Long getTraceId() {
        return traceId;
    }

    /**
     * Package-private. Re-parenting a run to a different dataset item would silently invalidate every aggregation
     * downstream (per-item summary rows, dataset-item replay counts, dashboards). Kept package-visible only for Spring
     * Data JDBC reflective hydration on read.
     */
    void setDatasetItemId(Long datasetItemId) {
        this.datasetItemId = datasetItemId;
    }

    /**
     * Package-private. Re-parenting a run to a different experiment would silently invalidate every aggregation
     * downstream (per-experiment progress, success/failure counts, dashboards). Kept package-visible only for Spring
     * Data JDBC reflective hydration on read.
     */
    void setExperimentId(Long experimentId) {
        this.experimentId = experimentId;
    }

    /**
     * Package-private. Status is owned by {@link #markRunning()}, {@link #complete}, and {@link #fail}; a direct setter
     * would let callers bypass the legal-transition check. Kept package-visible for Spring Data JDBC reflective
     * hydration on read.
     */
    void setStatus(AiEvalExperimentRunStatus status) {
        Validate.notNull(status, "status must not be null");

        this.status = status.ordinal();
    }

    /**
     * Package-private. {@code cost} is recorded atomically with the COMPLETED transition by {@link #complete}; an
     * external setter would let the field drift out of sync with status.
     */
    void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    /**
     * Package-private. {@code latencyMs} is recorded atomically with the COMPLETED transition by {@link #complete}.
     */
    void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }

    /**
     * Package-private. {@code traceId} is recorded atomically with the COMPLETED transition by {@link #complete}.
     */
    void setTraceId(Long traceId) {
        this.traceId = traceId;
    }

    /**
     * Package-private. {@code errorMessage} is recorded atomically with the FAILED transition by {@link #fail}.
     */
    void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Transitions the run to {@link AiEvalExperimentRunStatus#RUNNING}.
     *
     * @throws IllegalStateException if the run is not currently {@link AiEvalExperimentRunStatus#PENDING}.
     */
    public void markRunning() {
        if (getStatus() != AiEvalExperimentRunStatus.PENDING) {
            throw new IllegalStateException("Cannot mark run as RUNNING from " + getStatus());
        }

        this.status = AiEvalExperimentRunStatus.RUNNING.ordinal();
    }

    /**
     * Marks the run as completed and records the produced trace, latency, and cost.
     *
     * @throws IllegalStateException if the run is not currently {@link AiEvalExperimentRunStatus#RUNNING}.
     */
    public void complete(Long traceId, Integer latencyMs, BigDecimal cost) {
        if (getStatus() != AiEvalExperimentRunStatus.RUNNING) {
            throw new IllegalStateException("Cannot complete run from " + getStatus());
        }

        this.status = AiEvalExperimentRunStatus.COMPLETED.ordinal();
        this.traceId = traceId;
        this.latencyMs = latencyMs;
        this.cost = cost;
    }

    /**
     * Marks the run as failed and records the error message. Permitted from {@link AiEvalExperimentRunStatus#PENDING}
     * as well as {@link AiEvalExperimentRunStatus#RUNNING} so the executor can cancel pre-started runs (e.g. when the
     * user requests stop) without first transitioning them to RUNNING.
     *
     * @throws IllegalStateException if the run is already in a terminal state.
     */
    public void fail(String errorMessage) {
        AiEvalExperimentRunStatus current = getStatus();

        if (current != AiEvalExperimentRunStatus.PENDING && current != AiEvalExperimentRunStatus.RUNNING) {
            throw new IllegalStateException("Cannot fail run from " + current);
        }

        this.status = AiEvalExperimentRunStatus.FAILED.ordinal();
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "AiEvalExperimentRun{" +
            "id=" + id +
            ", experimentId=" + experimentId +
            ", datasetItemId=" + datasetItemId +
            ", traceId=" + traceId +
            ", status=" + getStatus() +
            ", latencyMs=" + latencyMs +
            ", cost=" + cost +
            '}';
    }
}

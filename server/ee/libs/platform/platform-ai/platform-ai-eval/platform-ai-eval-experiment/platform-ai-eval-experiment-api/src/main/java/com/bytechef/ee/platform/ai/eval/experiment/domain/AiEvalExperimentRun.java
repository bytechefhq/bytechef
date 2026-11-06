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

    void setDatasetItemId(Long datasetItemId) {
        this.datasetItemId = datasetItemId;
    }

    void setExperimentId(Long experimentId) {
        this.experimentId = experimentId;
    }

    void setStatus(AiEvalExperimentRunStatus status) {
        Validate.notNull(status, "status must not be null");

        this.status = status.ordinal();
    }

    void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }

    void setTraceId(Long traceId) {
        this.traceId = traceId;
    }

    void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void markRunning() {
        if (getStatus() != AiEvalExperimentRunStatus.PENDING) {
            throw new IllegalStateException("Cannot mark run as RUNNING from " + getStatus());
        }

        this.status = AiEvalExperimentRunStatus.RUNNING.ordinal();
    }

    public void complete(Long traceId, Integer latencyMs, BigDecimal cost) {
        if (getStatus() != AiEvalExperimentRunStatus.RUNNING) {
            throw new IllegalStateException("Cannot complete run from " + getStatus());
        }

        this.status = AiEvalExperimentRunStatus.COMPLETED.ordinal();
        this.traceId = traceId;
        this.latencyMs = latencyMs;
        this.cost = cost;
    }

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

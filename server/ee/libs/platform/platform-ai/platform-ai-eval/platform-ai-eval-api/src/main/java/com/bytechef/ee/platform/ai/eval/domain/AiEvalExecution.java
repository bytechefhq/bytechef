/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.domain;

import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Table("ai_eval_execution")
public final class AiEvalExecution {

    @CreatedDate
    private Instant createdDate;

    private String errorMessage;

    private Long evalRuleId;

    private Integer failureReason;

    @Id
    private Long id;

    private Long scoreId;

    private int status;

    private Long traceId;

    private AiEvalExecution() {
    }

    public AiEvalExecution(Long evalRuleId, Long traceId) {
        Validate.notNull(evalRuleId, "evalRuleId must not be null");
        Validate.notNull(traceId, "traceId must not be null");

        this.evalRuleId = evalRuleId;
        this.status = AiEvalExecutionStatus.PENDING.ordinal();
        this.traceId = traceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiEvalExecution aiEvalExecution)) {
            return false;
        }

        return Objects.equals(id, aiEvalExecution.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Long getEvalRuleId() {
        return evalRuleId;
    }

    public AiEvalExecutionFailureReason getFailureReason() {
        return failureReason == null
            ? null
            : safeOrdinalLookup("failureReason", failureReason, AiEvalExecutionFailureReason.values());
    }

    public Long getId() {
        return id;
    }

    public Long getScoreId() {
        return scoreId;
    }

    public AiEvalExecutionStatus getStatus() {
        return safeOrdinalLookup("status", status, AiEvalExecutionStatus.values());
    }

    private <E extends Enum<E>> E safeOrdinalLookup(String field, int ordinal, E[] values) {
        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalStateException(
                "AiEvalExecution." + field + " ordinal " + ordinal + " is out of range [0, " + values.length
                    + ") — row may be corrupt (id=" + id + ")");
        }

        return values[ordinal];
    }

    public Long getTraceId() {
        return traceId;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setFailureReason(AiEvalExecutionFailureReason failureReason) {
        this.failureReason = failureReason == null ? null : failureReason.ordinal();
    }

    public void setScoreId(Long scoreId) {
        this.scoreId = scoreId;
    }

    void setStatus(AiEvalExecutionStatus status) {
        Validate.notNull(status, "status must not be null");

        this.status = status.ordinal();
    }

    public void markCompleted() {
        AiEvalExecutionStatus current = getStatus();

        Validate.isTrue(
            current == AiEvalExecutionStatus.PENDING,
            "Illegal FSM transition: markCompleted requires status=PENDING but was %s (id=%s)",
            current, id);

        this.status = AiEvalExecutionStatus.COMPLETED.ordinal();
        this.failureReason = null;
        this.errorMessage = null;
    }

    public void markErrored(AiEvalExecutionFailureReason failureReason, String errorMessage) {
        Validate.notNull(failureReason, "failureReason must not be null");

        AiEvalExecutionStatus current = getStatus();

        Validate.isTrue(
            current == AiEvalExecutionStatus.PENDING,
            "Illegal FSM transition: markErrored requires status=PENDING but was %s (id=%s)",
            current, id);

        this.status = AiEvalExecutionStatus.ERROR.ordinal();
        this.failureReason = failureReason.ordinal();
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "AiEvalExecution{" +
            "id=" + id +
            ", evalRuleId=" + evalRuleId +
            ", traceId=" + traceId +
            ", status=" + getStatus() +
            '}';
    }
}

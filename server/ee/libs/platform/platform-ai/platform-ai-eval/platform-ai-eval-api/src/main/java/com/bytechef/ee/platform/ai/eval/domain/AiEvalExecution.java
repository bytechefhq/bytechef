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

    /**
     * Persisted as INT ordinal — a corrupt or out-of-range value surfaces with the row id and field name instead of as
     * a raw {@link ArrayIndexOutOfBoundsException} at the call site, matching the hardening on {@link AiEvalScore}.
     */
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

    /**
     * Persisted as INT ordinal — a corrupt or out-of-range value surfaces with the row id and field name instead of as
     * a raw {@link ArrayIndexOutOfBoundsException} at the call site, matching the hardening on {@link AiEvalScore}.
     */
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

    /**
     * Package-private hydration setter retained for Spring Data JDBC's reflective row mapping, which requires a setter
     * for every persisted column and runs at any visibility. Production code must transition status via
     * {@link #markCompleted()} or {@link #markErrored(AiEvalExecutionFailureReason, String)} so the FSM check fires —
     * making this method public would let any caller silently overwrite a terminal status (the bug the FSM methods
     * exist to prevent).
     */
    void setStatus(AiEvalExecutionStatus status) {
        Validate.notNull(status, "status must not be null");

        this.status = status.ordinal();
    }

    /**
     * Atomically transitions to {@link AiEvalExecutionStatus#COMPLETED}. Prefer this over {@link #setStatus} so
     * dashboards and audit subscribers can distinguish "the executor flipped the status" from "Spring read it from the
     * database". Mirrors the discipline applied to {@link AiExperimentRun#complete}, where every legal transition has a
     * named method that throws on illegal source state.
     *
     * <p>
     * Only legal source state is {@link AiEvalExecutionStatus#PENDING}. A double-call from {@code COMPLETED} or
     * {@code ERROR} would silently overwrite the prior outcome (clearing {@code failureReason} and {@code errorMessage}
     * on a row that was already ERROR), which is exactly the FSM-invariant breach the typed transition method exists to
     * prevent.
     */
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

    /**
     * Atomically transitions to {@link AiEvalExecutionStatus#ERROR} and records the typed failure reason plus a
     * human-readable error message. Prefer this over the three separate setters; bundling the writes under one method
     * makes it impossible to ship an ERROR row with a missing reason or a drifted message.
     *
     * <p>
     * Only legal source state is {@link AiEvalExecutionStatus#PENDING}. A markErrored call from {@code COMPLETED} would
     * silently invalidate every downstream aggregation (the score row already exists and points at this execution); a
     * re-call from {@code ERROR} would overwrite the original failure context.
     */
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

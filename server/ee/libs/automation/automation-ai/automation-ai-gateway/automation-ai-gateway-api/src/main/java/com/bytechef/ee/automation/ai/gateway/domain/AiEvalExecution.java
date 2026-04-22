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
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_eval_execution")
public class AiEvalExecution {

    @CreatedDate
    private Instant createdDate;

    private String errorMessage;

    private Long evalRuleId;

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

    public Long getId() {
        return id;
    }

    public Long getScoreId() {
        return scoreId;
    }

    public AiEvalExecutionStatus getStatus() {
        return AiEvalExecutionStatus.values()[status];
    }

    public Long getTraceId() {
        return traceId;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setScoreId(Long scoreId) {
        this.scoreId = scoreId;
    }

    public void setStatus(AiEvalExecutionStatus status) {
        Validate.notNull(status, "status must not be null");

        this.status = status.ordinal();
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

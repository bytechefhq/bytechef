/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.execution.cost.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * One row per terminal workflow execution: {@code totalCost = baseRunCharge + aiCost} in USD (Sim-modeled cost formula;
 * tool cost joins the sum once workflow tool metering exists). Written by the terminal-status listener, idempotent per
 * {@code jobId}. A cost row belongs to at most one workspace, carried by the nullable {@link #workspaceId} column; the
 * column is null for runs that have no workspace (editor runs, embedded executions).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("workflow_execution_cost")
public class WorkflowExecutionCost {

    @Id
    private Long id;

    @Version
    private int version;

    @Column("job_id")
    private Long jobId;

    /**
     * Workspace that owns this cost row, or {@code null} for an execution that has none — editor runs and embedded
     * executions. Boxed on purpose: a primitive would collapse "no workspace" into workspace 0, which is a real
     * workspace id.
     */
    @Column("workspace_id")
    private @Nullable Long workspaceId;

    @Column("base_run_charge")
    private BigDecimal baseRunCharge;

    @Column("ai_cost")
    private BigDecimal aiCost;

    @Column("total_cost")
    private BigDecimal totalCost;

    @Column("currency")
    private String currency = "USD";

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    public WorkflowExecutionCost() {
    }

    public WorkflowExecutionCost(long jobId, BigDecimal baseRunCharge, BigDecimal aiCost) {
        this.jobId = jobId;
        this.baseRunCharge = baseRunCharge;
        this.aiCost = aiCost;
        this.totalCost = baseRunCharge.add(aiCost);
    }

    public Long getId() {
        return id;
    }

    public Long getJobId() {
        return jobId;
    }

    public @Nullable Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(@Nullable Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public BigDecimal getBaseRunCharge() {
        return baseRunCharge;
    }

    public BigDecimal getAiCost() {
        return aiCost;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof WorkflowExecutionCost that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "WorkflowExecutionCost{id=%s, jobId=%s, workspaceId=%s, totalCost=%s %s}".formatted(
            id, jobId, workspaceId, totalCost, currency);
    }
}

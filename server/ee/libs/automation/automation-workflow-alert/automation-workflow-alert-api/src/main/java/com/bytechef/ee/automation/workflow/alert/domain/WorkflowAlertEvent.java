/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.alert.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * One row per fired alert — the history the alerts UI lists and the audit trail for what was delivered when. Rows
 * cascade-delete with their rule.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("workflow_alert_event")
public class WorkflowAlertEvent {

    @Id
    private Long id;

    @Column("workflow_alert_rule_id")
    private Long workflowAlertRuleId;

    @Column("job_id")
    private Long jobId;

    @Column("triggered_value")
    private BigDecimal triggeredValue;

    @Column
    private String message;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    public WorkflowAlertEvent() {
    }

    public WorkflowAlertEvent(Long workflowAlertRuleId, Long jobId, BigDecimal triggeredValue, String message) {
        this.workflowAlertRuleId = workflowAlertRuleId;
        this.jobId = jobId;
        this.triggeredValue = triggeredValue;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public Long getWorkflowAlertRuleId() {
        return workflowAlertRuleId;
    }

    public Long getJobId() {
        return jobId;
    }

    public BigDecimal getTriggeredValue() {
        return triggeredValue;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof WorkflowAlertEvent that)) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

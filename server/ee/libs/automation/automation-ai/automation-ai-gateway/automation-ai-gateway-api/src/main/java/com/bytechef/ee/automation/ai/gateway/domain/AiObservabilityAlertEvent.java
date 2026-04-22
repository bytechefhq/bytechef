/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_observability_alert_event")
public class AiObservabilityAlertEvent {

    @Column("alert_rule_id")
    private Long alertRuleId;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Id
    private Long id;

    @Column
    private String message;

    @Column("resolved_date")
    private Instant resolvedDate;

    @Column
    private int status;

    @Column("triggered_value")
    private BigDecimal triggeredValue;

    private AiObservabilityAlertEvent() {
    }

    public AiObservabilityAlertEvent(Long alertRuleId, BigDecimal triggeredValue, String message) {
        Validate.notNull(alertRuleId, "alertRuleId must not be null");

        this.alertRuleId = alertRuleId;
        this.message = message;
        this.status = AiObservabilityAlertEventStatus.TRIGGERED.ordinal();
        this.triggeredValue = triggeredValue;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiObservabilityAlertEvent aiObservabilityAlertEvent)) {
            return false;
        }

        return Objects.equals(id, aiObservabilityAlertEvent.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public Long getAlertRuleId() {
        return alertRuleId;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public Instant getResolvedDate() {
        return resolvedDate;
    }

    public AiObservabilityAlertEventStatus getStatus() {
        return AiObservabilityAlertEventStatus.values()[status];
    }

    public BigDecimal getTriggeredValue() {
        return triggeredValue;
    }

    public void setResolvedDate(Instant resolvedDate) {
        this.resolvedDate = resolvedDate;
    }

    public void setStatus(AiObservabilityAlertEventStatus status) {
        Validate.notNull(status, "status must not be null");

        this.status = status.ordinal();
    }

    @Override
    public String toString() {
        return "AiObservabilityAlertEvent{" +
            "id=" + id +
            ", alertRuleId=" + alertRuleId +
            ", triggeredValue=" + triggeredValue +
            ", status=" + getStatus() +
            ", createdDate=" + createdDate +
            '}';
    }
}

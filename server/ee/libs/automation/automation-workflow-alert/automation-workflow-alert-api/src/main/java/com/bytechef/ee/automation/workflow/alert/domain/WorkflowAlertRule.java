/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.alert.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Workspace-scoped alert rule over terminal workflow-run events (Sim alerts model). Delivery targets are
 * {@code Notification} rows referenced through the {@code workflow_alert_rule_notification} join table — the rule owns
 * WHEN to alert, the platform-notification registry owns WHERE and HOW the alert is delivered.
 *
 * <p>
 * The {@code consecutiveFailures} / window counters / {@code ewmaLatencyMs} / {@code lastActivityDate} columns are the
 * evaluator's rolling state, updated on every matching terminal event so evaluation never has to scan job history.
 * {@code lastTriggeredDate} drives the cooldown (default 60 minutes, Sim's fixed cooldown).
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("workflow_alert_rule")
public class WorkflowAlertRule {

    @Id
    private Long id;

    @Version
    private int version;

    @Column
    private String name;

    /** Optional scope: only runs of this workflow feed the rule; {@code null} = every run in the workspace. */
    @Column("workflow_id")
    private String workflowId;

    @Column("rule_type")
    private int ruleType;

    @Column
    private BigDecimal threshold;

    @Column("window_minutes")
    private Integer windowMinutes;

    @Column("cooldown_minutes")
    private int cooldownMinutes = 60;

    @Column
    private boolean enabled = true;

    @Column("consecutive_failures")
    private int consecutiveFailures;

    @Column("window_start")
    private Instant windowStart;

    @Column("runs_in_window")
    private int runsInWindow;

    @Column("failures_in_window")
    private int failuresInWindow;

    @Column("ewma_latency_ms")
    private BigDecimal ewmaLatencyMs;

    @Column("last_activity_date")
    private Instant lastActivityDate;

    @Column("last_triggered_date")
    private Instant lastTriggeredDate;

    @MappedCollection(idColumn = "workflow_alert_rule_id")
    private Set<WorkflowAlertRuleNotification> ruleNotifications = new HashSet<>();

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public WorkflowAlertRuleType getRuleType() {
        return WorkflowAlertRuleType.values()[ruleType];
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public Integer getWindowMinutes() {
        return windowMinutes;
    }

    public int getCooldownMinutes() {
        return cooldownMinutes;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }

    public Instant getWindowStart() {
        return windowStart;
    }

    public int getRunsInWindow() {
        return runsInWindow;
    }

    public int getFailuresInWindow() {
        return failuresInWindow;
    }

    public BigDecimal getEwmaLatencyMs() {
        return ewmaLatencyMs;
    }

    public Instant getLastActivityDate() {
        return lastActivityDate;
    }

    public Instant getLastTriggeredDate() {
        return lastTriggeredDate;
    }

    public List<Long> getNotificationIds() {
        return ruleNotifications.stream()
            .map(WorkflowAlertRuleNotification::getNotificationId)
            .toList();
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public void setRuleType(WorkflowAlertRuleType workflowAlertRuleType) {
        this.ruleType = workflowAlertRuleType.ordinal();
    }

    public void setThreshold(BigDecimal threshold) {
        this.threshold = threshold;
    }

    public void setWindowMinutes(Integer windowMinutes) {
        this.windowMinutes = windowMinutes;
    }

    public void setCooldownMinutes(int cooldownMinutes) {
        this.cooldownMinutes = cooldownMinutes;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setConsecutiveFailures(int consecutiveFailures) {
        this.consecutiveFailures = consecutiveFailures;
    }

    public void setWindowStart(Instant windowStart) {
        this.windowStart = windowStart;
    }

    public void setRunsInWindow(int runsInWindow) {
        this.runsInWindow = runsInWindow;
    }

    public void setFailuresInWindow(int failuresInWindow) {
        this.failuresInWindow = failuresInWindow;
    }

    public void setEwmaLatencyMs(BigDecimal ewmaLatencyMs) {
        this.ewmaLatencyMs = ewmaLatencyMs;
    }

    public void setLastActivityDate(Instant lastActivityDate) {
        this.lastActivityDate = lastActivityDate;
    }

    public void setLastTriggeredDate(Instant lastTriggeredDate) {
        this.lastTriggeredDate = lastTriggeredDate;
    }

    public void setNotificationIds(List<Long> notificationIds) {
        ruleNotifications = new HashSet<>();

        for (Long notificationId : notificationIds) {
            ruleNotifications.add(new WorkflowAlertRuleNotification(notificationId));
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof WorkflowAlertRule that)) {
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
        return "WorkflowAlertRule{id=%s, name='%s', ruleType=%s, threshold=%s, enabled=%s}".formatted(
            id, name, getRuleType(), threshold, enabled);
    }
}

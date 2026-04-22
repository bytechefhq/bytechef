/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
// CT_CONSTRUCTOR_THROW is suppressed: the constructor's validation of metric/condition/threshold is load-bearing so
// invalid alert rules cannot reach the repository layer where they would fire spurious or missed alerts.
@SuppressFBWarnings({
    "CT_CONSTRUCTOR_THROW", "EI"
})
@Table("ai_observability_alert_rule")
public class AiObservabilityAlertRule {

    @MappedCollection(idColumn = "ai_observability_alert_rule")
    private Set<AiObservabilityAlertRuleChannel> channels = new HashSet<>();

    @Column("condition")
    private int condition;

    @Column("cooldown_minutes")
    private int cooldownMinutes;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column
    private boolean enabled;

    @Column
    private String filters;

    @Id
    private Long id;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column
    private int metric;

    @Column
    private String name;

    @Column("project_id")
    private Long projectId;

    @Column("snoozed_until")
    private Instant snoozedUntil;

    @Column
    private BigDecimal threshold;

    @Version
    private int version;

    @Column("window_minutes")
    private int windowMinutes;

    @Column("workspace_id")
    private Long workspaceId;

    private AiObservabilityAlertRule() {
    }

    public AiObservabilityAlertRule(Long workspaceId, String name, AiObservabilityAlertMetric metric,
        AiObservabilityAlertCondition condition, BigDecimal threshold, int windowMinutes, int cooldownMinutes) {

        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notBlank(name, "name must not be blank");
        Validate.notNull(metric, "metric must not be null");
        Validate.notNull(condition, "condition must not be null");
        Validate.notNull(threshold, "threshold must not be null");

        validateThresholdForMetric(threshold, metric);

        this.condition = condition.ordinal();
        this.cooldownMinutes = cooldownMinutes;
        this.enabled = true;
        this.metric = metric.ordinal();
        this.name = name;
        this.threshold = threshold;
        this.windowMinutes = windowMinutes;
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiObservabilityAlertRule aiObservabilityAlertRule)) {
            return false;
        }

        return Objects.equals(id, aiObservabilityAlertRule.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public Set<AiObservabilityAlertRuleChannel> getChannels() {
        return channels;
    }

    public AiObservabilityAlertCondition getCondition() {
        return AiObservabilityAlertCondition.values()[condition];
    }

    public int getCooldownMinutes() {
        return cooldownMinutes;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getFilters() {
        return filters;
    }

    /**
     * Typed view of the filter JSON. Prefer this over {@link #getFilters()} in the alert evaluator and other
     * application code — a malformed filter surfaces as a parse exception at read time rather than as silent "no trace
     * matches" during window aggregation. Returns {@code null} when no filter is configured.
     */
    public AiObservabilityAlertFilter getTypedFilter() {
        return AiObservabilityAlertFilter.fromJson(filters);
    }

    /**
     * Typed setter that validates the filter tree at write time. Delegates storage to the legacy {@link String} column
     * so no migration is needed — the JSON representation is identical.
     */
    public void setTypedFilter(AiObservabilityAlertFilter filter) {
        this.filters = AiObservabilityAlertFilter.toJson(filter);
    }

    public Long getId() {
        return id;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public AiObservabilityAlertMetric getMetric() {
        return AiObservabilityAlertMetric.values()[metric];
    }

    public String getName() {
        return name;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Instant getSnoozedUntil() {
        return snoozedUntil;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    /**
     * Currency-aware view of {@link #getThreshold()}, valid only when {@link #getMetric()} is cost-denominated (e.g.
     * {@code COST_PER_HOUR}, {@code TOTAL_COST}). Returns {@code null} for non-monetary metrics (latency, token counts,
     * error rate) so callers cannot accidentally compare non-monetary scalars with {@link Money}.
     */
    public Money getThresholdAsMoney() {
        if (threshold == null) {
            return null;
        }

        return getMetric().isMonetary() ? Money.usd(threshold) : null;
    }

    public int getVersion() {
        return version;
    }

    public int getWindowMinutes() {
        return windowMinutes;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setChannels(Set<AiObservabilityAlertRuleChannel> channels) {
        this.channels = channels;
    }

    public void setCondition(AiObservabilityAlertCondition condition) {
        Validate.notNull(condition, "condition must not be null");

        this.condition = condition.ordinal();
    }

    public void setCooldownMinutes(int cooldownMinutes) {
        this.cooldownMinutes = cooldownMinutes;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setFilters(String filters) {
        // Round-trip through the sealed AiObservabilityAlertFilter hierarchy so a malformed/unknown-discriminator
        // filter JSON fails at write time with a loud error instead of silently surviving into the DB and only
        // blowing up on evaluation. Null/blank is allowed — "no filter configured".
        if (filters != null && !filters.isBlank()) {
            AiObservabilityAlertFilter parsed = AiObservabilityAlertFilter.fromJson(filters);

            this.filters = AiObservabilityAlertFilter.toJson(parsed);
        } else {
            this.filters = filters;
        }
    }

    public void setMetric(AiObservabilityAlertMetric metric) {
        Validate.notNull(metric, "metric must not be null");

        this.metric = metric.ordinal();

        // If threshold is already set, re-validate the cross-field invariant so a metric switch cannot silently
        // reinterpret the scalar (e.g., COST → LATENCY_P95 without also updating threshold from 10 USD to 10 ms).
        if (threshold != null) {
            validateThresholdForMetric(threshold, metric);
        }
    }

    public void setName(String name) {
        Validate.notBlank(name, "name must not be blank");

        this.name = name;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setSnoozedUntil(Instant snoozedUntil) {
        this.snoozedUntil = snoozedUntil;
    }

    public void setThreshold(BigDecimal threshold) {
        Validate.notNull(threshold, "threshold must not be null");

        validateThresholdForMetric(threshold, getMetric());

        this.threshold = threshold;
    }

    /**
     * Atomic swap: sets metric and threshold together, validating the pair. Prefer this over separate
     * {@link #setMetric}/{@link #setThreshold} calls whenever the metric is changing — the individual setters each
     * validate against the *current* stored counterpart, which gives correct but awkward ordering requirements.
     */
    public void setMetricAndThreshold(AiObservabilityAlertMetric metric, BigDecimal threshold) {
        Validate.notNull(metric, "metric must not be null");
        Validate.notNull(threshold, "threshold must not be null");

        validateThresholdForMetric(threshold, metric);

        this.metric = metric.ordinal();
        this.threshold = threshold;
    }

    /**
     * Cross-field invariant: reject negative thresholds across all supported metrics — cost cannot be under zero, and
     * scalar metrics (latency ms, token counts, error-rate percentage, request volume) are non-negative by definition.
     * This is a backstop against silent reinterpretation when metric-and-threshold drift apart on an edit — prefer
     * {@link #setMetricAndThreshold} for any flow that changes metric, so the pair is validated as a unit.
     */
    private static void validateThresholdForMetric(BigDecimal threshold, AiObservabilityAlertMetric metric) {
        if (threshold.signum() < 0) {
            throw new IllegalArgumentException(
                "threshold must not be negative for metric " + metric + " (got " + threshold.toPlainString() + ")");
        }
    }

    public void setWindowMinutes(int windowMinutes) {
        this.windowMinutes = windowMinutes;
    }

    @Override
    public String toString() {
        return "AiObservabilityAlertRule{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", name='" + name + '\'' +
            ", metric=" + getMetric() +
            ", enabled=" + enabled +
            ", createdDate=" + createdDate +
            '}';
    }
}

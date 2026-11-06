/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.domain;

import com.bytechef.ee.platform.ai.llm.usage.Money;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
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
    private @Nullable Long workspaceId;

    private AiObservabilityAlertRule() {
    }

    public AiObservabilityAlertRule(String name, AiObservabilityAlertMetric metric,
        AiObservabilityAlertCondition condition, BigDecimal threshold, int windowMinutes, int cooldownMinutes) {

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

    public AiObservabilityAlertFilter getTypedFilter() {
        return AiObservabilityAlertFilter.fromJson(filters);
    }

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

    public @Nullable Long getWorkspaceId() {
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

    public void setMetricAndThreshold(AiObservabilityAlertMetric metric, BigDecimal threshold) {
        Validate.notNull(metric, "metric must not be null");
        Validate.notNull(threshold, "threshold must not be null");

        validateThresholdForMetric(threshold, metric);

        this.metric = metric.ordinal();
        this.threshold = threshold;
    }

    private static void validateThresholdForMetric(BigDecimal threshold, AiObservabilityAlertMetric metric) {
        if (threshold.signum() < 0) {
            throw new IllegalArgumentException(
                "threshold must not be negative for metric " + metric + " (got " + threshold.toPlainString() + ")");
        }
    }

    public void setWindowMinutes(int windowMinutes) {
        this.windowMinutes = windowMinutes;
    }

    public void setWorkspaceId(@Nullable Long workspaceId) {
        this.workspaceId = workspaceId;
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

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.domain;

import com.bytechef.ee.platform.ai.llm.usage.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("ai_gateway_budget")
public final class AiGatewayBudget {

    private static final AiGatewayBudgetEnforcementMode[] ENFORCEMENT_MODES = AiGatewayBudgetEnforcementMode.values();
    private static final AiGatewayBudgetPeriod[] PERIODS = AiGatewayBudgetPeriod.values();

    @Column("alert_threshold")
    private int alertThreshold;

    @Column
    private BigDecimal amount;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column
    private boolean enabled;

    @Column("enforcement_mode")
    private int enforcementMode;

    @Id
    private Long id;

    @Column("project_id")
    private Long projectId;

    @Column("last_modified_date")
    @LastModifiedDate
    private Instant lastModifiedDate;

    @Column
    private int period;

    @Version
    private int version;

    @Column("week_starts_on")
    private int weekStartsOn;

    @Column("workspace_id")
    private @Nullable Long workspaceId;

    private AiGatewayBudget() {
    }

    public AiGatewayBudget(
        BigDecimal amount, AiGatewayBudgetPeriod period, AiGatewayBudgetEnforcementMode enforcementMode) {
        Validate.notNull(amount, "amount must not be null");
        Validate.isTrue(amount.compareTo(BigDecimal.ZERO) >= 0, "amount must not be negative");
        Validate.notNull(period, "period must not be null");
        Validate.notNull(enforcementMode, "enforcementMode must not be null");

        this.alertThreshold = 80;
        this.amount = amount;
        this.enabled = true;
        this.enforcementMode = enforcementMode.ordinal();
        this.period = period.ordinal();
        this.weekStartsOn = 1;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiGatewayBudget aiGatewayBudget)) {
            return false;
        }

        return Objects.equals(id, aiGatewayBudget.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public int getAlertThreshold() {
        return alertThreshold;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Money getAmountAsMoney() {
        return Money.usd(amount);
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public AiGatewayBudgetEnforcementMode getEnforcementMode() {
        return ENFORCEMENT_MODES[enforcementMode];
    }

    public Long getId() {
        return id;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Long getProjectId() {
        return projectId;
    }

    public AiGatewayBudgetPeriod getPeriod() {
        return PERIODS[period];
    }

    public int getVersion() {
        return version;
    }

    public int getWeekStartsOn() {
        return weekStartsOn;
    }

    public @Nullable Long getWorkspaceId() {
        return workspaceId;
    }

    public void setAlertThreshold(int alertThreshold) {
        Validate.inclusiveBetween(0, 100, alertThreshold, "alertThreshold must be between 0 and 100");

        this.alertThreshold = alertThreshold;
    }

    public void setAmount(BigDecimal amount) {
        Validate.notNull(amount, "amount must not be null");
        Validate.isTrue(amount.compareTo(BigDecimal.ZERO) >= 0, "amount must not be negative");

        this.amount = amount;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setEnforcementMode(AiGatewayBudgetEnforcementMode enforcementMode) {
        Validate.notNull(enforcementMode, "enforcementMode must not be null");

        this.enforcementMode = enforcementMode.ordinal();
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setPeriod(AiGatewayBudgetPeriod period) {
        Validate.notNull(period, "period must not be null");

        this.period = period.ordinal();
    }

    public void setWeekStartsOn(int weekStartsOn) {
        Validate.inclusiveBetween(1, 7, weekStartsOn, "weekStartsOn must be between 1 (Monday) and 7 (Sunday)");

        this.weekStartsOn = weekStartsOn;
    }

    public void setWorkspaceId(@Nullable Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    @Override
    public String toString() {
        return "AiGatewayBudget{" +
            "id=" + id +
            ", amount=" + amount +
            ", period=" + getPeriod() +
            ", enforcementMode=" + getEnforcementMode() +
            ", alertThreshold=" + alertThreshold +
            ", enabled=" + enabled +
            ", createdDate=" + createdDate +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            ", workspaceId=" + workspaceId +
            '}';
    }
}

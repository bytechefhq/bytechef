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
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_gateway_budget")
public class AiGatewayBudget {

    // Enum.values() allocates a fresh array per call. These domain objects are hot on read (budget check happens per
    // request); caching the immutable snapshots avoids the allocation and matches the pattern already used by
    // AiGatewayProviderType.fromOrdinal.
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

    // Stored as the enum's ordinal — APPEND-ONLY for AiGatewayBudgetPeriod (see enforcementMode note above).
    @Column
    private int period;

    @Version
    private int version;

    @Column("week_starts_on")
    private int weekStartsOn;

    @Column("workspace_id")
    private Long workspaceId;

    private AiGatewayBudget() {
    }

    public AiGatewayBudget(
        Long workspaceId, BigDecimal amount, AiGatewayBudgetPeriod period,
        AiGatewayBudgetEnforcementMode enforcementMode) {

        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notNull(amount, "amount must not be null");
        // Zero is a valid sentinel: AiGatewayBudgetChecker.checkBudget treats amount <= 0 as "block everything"
        // regardless of enforcement mode, so admins can hard-freeze spend on a workspace without deleting the
        // budget row. Negative amounts remain invalid — they have no meaningful interpretation.
        Validate.isTrue(amount.compareTo(BigDecimal.ZERO) >= 0, "amount must not be negative");
        Validate.notNull(period, "period must not be null");
        Validate.notNull(enforcementMode, "enforcementMode must not be null");

        this.alertThreshold = 80;
        this.amount = amount;
        this.enabled = true;
        this.enforcementMode = enforcementMode.ordinal();
        this.period = period.ordinal();
        this.weekStartsOn = 1;
        this.workspaceId = workspaceId;
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

    /**
     * Returns the budget amount as a {@link Money} in USD. Budget persistence is still single-currency USD — this
     * accessor lets callers opt into currency-safe arithmetic ({@link Money#add}, {@link Money#compareTo}) so that
     * mixed-currency comparisons can never silently succeed when multi-currency support is introduced later.
     */
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

    /**
     * Day-of-week on which WEEKLY budget periods start. Uses {@link java.time.DayOfWeek#getValue()} values: 1 = Monday
     * (ISO default), 7 = Sunday. Meaningful only when {@link #getPeriod()} is {@code WEEKLY}; ignored otherwise.
     */
    public int getWeekStartsOn() {
        return weekStartsOn;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setAlertThreshold(int alertThreshold) {
        Validate.inclusiveBetween(0, 100, alertThreshold, "alertThreshold must be between 0 and 100");

        this.alertThreshold = alertThreshold;
    }

    public void setAmount(BigDecimal amount) {
        Validate.notNull(amount, "amount must not be null");
        // Zero is a valid sentinel: AiGatewayBudgetChecker.checkBudget treats amount <= 0 as "block everything"
        // regardless of enforcement mode, so admins can hard-freeze spend on a workspace without deleting the
        // budget row. Negative amounts remain invalid — they have no meaningful interpretation.
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

    @Override
    public String toString() {
        return "AiGatewayBudget{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", amount=" + amount +
            ", period=" + getPeriod() +
            ", enforcementMode=" + getEnforcementMode() +
            ", alertThreshold=" + alertThreshold +
            ", enabled=" + enabled +
            ", createdDate=" + createdDate +
            ", lastModifiedDate=" + lastModifiedDate +
            ", version=" + version +
            '}';
    }
}

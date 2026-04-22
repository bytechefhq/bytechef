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
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_gateway_spend_summary")
public class AiGatewaySpendSummary {

    @Column("api_key_id")
    private Long apiKeyId;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    // ISO 4217 currency code (e.g. "USD"). Pairs with totalCost — a BigDecimal alone can't tell USD $10 from EUR €10;
    // aggregation across mismatched currencies silently sums apples and oranges. Defaults to "USD" on construction.
    @Column
    private String currency;

    @Id
    private Long id;

    @Column
    private String model;

    @Column("period_end")
    private Instant periodEnd;

    @Column("period_start")
    private Instant periodStart;

    @Column("project_id")
    private Long projectId;

    @Column
    private String provider;

    @Column("request_count")
    private int requestCount;

    @Column("total_cost")
    private BigDecimal totalCost;

    @Column("total_input_tokens")
    private long totalInputTokens;

    @Column("total_output_tokens")
    private long totalOutputTokens;

    @Version
    private int version;

    @Column("workspace_id")
    private Long workspaceId;

    private AiGatewaySpendSummary() {
    }

    public AiGatewaySpendSummary(Instant periodStart, Instant periodEnd) {
        Validate.notNull(periodStart, "periodStart must not be null");
        Validate.notNull(periodEnd, "periodEnd must not be null");
        Validate.isTrue(!periodEnd.isBefore(periodStart), "periodEnd must not be before periodStart");

        this.currency = "USD";
        this.periodEnd = periodEnd;
        this.periodStart = periodStart;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiGatewaySpendSummary aiGatewaySpendSummary)) {
            return false;
        }

        return Objects.equals(id, aiGatewaySpendSummary.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public Long getApiKeyId() {
        return apiKeyId;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getCurrency() {
        return currency;
    }

    public Long getId() {
        return id;
    }

    public String getModel() {
        return model;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }

    public Instant getPeriodStart() {
        return periodStart;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getProvider() {
        return provider;
    }

    public int getRequestCount() {
        return requestCount;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    /**
     * Currency-aware view of {@link #getTotalCost()} paired with the row's {@link #getCurrency()} code. Spend summaries
     * already carry a per-row currency column (defaults to USD), so this accessor uses it directly rather than assuming
     * USD — preventing mixed-currency sums at aggregation time.
     */
    public Money getTotalCostAsMoney() {
        if (totalCost == null) {
            return null;
        }

        return Money.of(totalCost, currency == null ? "USD" : currency);
    }

    public long getTotalInputTokens() {
        return totalInputTokens;
    }

    public long getTotalOutputTokens() {
        return totalOutputTokens;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setApiKeyId(Long apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    public void setCurrency(String currency) {
        Validate.notBlank(currency, "currency must not be blank");
        Validate.isTrue(currency.length() == 3, "currency must be a 3-letter ISO 4217 code");

        this.currency = currency;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setRequestCount(int requestCount) {
        Validate.isTrue(requestCount >= 0, "requestCount must not be negative");

        this.requestCount = requestCount;
    }

    public void setTotalCost(BigDecimal totalCost) {
        if (totalCost != null) {
            Validate.isTrue(totalCost.signum() >= 0, "totalCost must not be negative");
        }

        this.totalCost = totalCost;
    }

    public void setTotalInputTokens(long totalInputTokens) {
        Validate.isTrue(totalInputTokens >= 0, "totalInputTokens must not be negative");

        this.totalInputTokens = totalInputTokens;
    }

    public void setTotalOutputTokens(long totalOutputTokens) {
        Validate.isTrue(totalOutputTokens >= 0, "totalOutputTokens must not be negative");

        this.totalOutputTokens = totalOutputTokens;
    }

    /**
     * Tenant-isolation guard: workspaceId can only transition from null → non-null. Any attempt to re-stamp a different
     * tenant is rejected.
     */
    public void setWorkspaceId(Long workspaceId) {
        Validate.notNull(workspaceId, "workspaceId must not be null");

        if (this.workspaceId != null) {
            throw new IllegalStateException(
                "workspaceId already set to " + this.workspaceId + "; cannot re-stamp to " + workspaceId);
        }

        this.workspaceId = workspaceId;
    }

    @Override
    public String toString() {
        return "AiGatewaySpendSummary{" +
            "id=" + id +
            ", provider='" + provider + '\'' +
            ", model='" + model + '\'' +
            ", periodStart=" + periodStart +
            ", periodEnd=" + periodEnd +
            ", requestCount=" + requestCount +
            ", totalCost=" + totalCost +
            ", createdDate=" + createdDate +
            '}';
    }
}

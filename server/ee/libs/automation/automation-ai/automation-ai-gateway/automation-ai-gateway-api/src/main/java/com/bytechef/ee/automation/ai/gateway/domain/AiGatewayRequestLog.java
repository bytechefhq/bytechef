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
@Table("ai_gateway_request_log")
public class AiGatewayRequestLog {

    @Column("api_key_id")
    private Long apiKeyId;

    @Column("cache_hit")
    private boolean cacheHit;

    @Column
    private BigDecimal cost;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column("error_message")
    private String errorMessage;

    @Id
    private Long id;

    @Column("input_tokens")
    private Integer inputTokens;

    @Column("latency_ms")
    private Integer latencyMs;

    @Column("output_tokens")
    private Integer outputTokens;

    @Column("project_id")
    private Long projectId;

    @Column("request_id")
    private String requestId;

    @Column("requested_model")
    private String requestedModel;

    @Column("routed_model")
    private String routedModel;

    @Column("routed_provider")
    private String routedProvider;

    @Column("routing_policy_id")
    private Long routingPolicyId;

    // Stored as the enum's ordinal. APPEND-ONLY: new AiGatewayRoutingStrategyType values must be added at the end,
    // never reordered or inserted — request-log rows keep their meaning by ordinal index. Reordering silently
    // mis-attributes historical routing decisions on every existing row.
    @Column("routing_strategy")
    private Integer routingStrategy;

    @Column
    private Integer status;

    @Column("workspace_id")
    private Long workspaceId;

    private AiGatewayRequestLog() {
    }

    public AiGatewayRequestLog(String requestId, String requestedModel) {
        Validate.notBlank(requestId, "requestId must not be blank");
        Validate.notBlank(requestedModel, "requestedModel must not be blank");

        this.requestId = requestId;
        this.requestedModel = requestedModel;
    }

    public AiGatewayRequestLog(
        String requestId, String requestedModel, String routedModel, String routedProvider, Long workspaceId) {

        Validate.notBlank(requestId, "requestId must not be blank");
        Validate.notBlank(requestedModel, "requestedModel must not be blank");
        Validate.notNull(workspaceId, "workspaceId must not be null — a request log without tenant attribution "
            + "cannot be safely filtered by the observability/cost paths");

        this.requestId = requestId;
        this.requestedModel = requestedModel;
        this.routedModel = routedModel;
        this.routedProvider = routedProvider;
        this.workspaceId = workspaceId;
    }

    public static AiGatewayRequestLog forSuccess(
        String requestId, String requestedModel, String routedModel, String routedProvider, Long workspaceId,
        int status, Integer inputTokens, Integer outputTokens, Integer latencyMs, BigDecimal cost) {

        Validate.notNull(workspaceId, "workspaceId must not be null for a success log");

        AiGatewayRequestLog log = new AiGatewayRequestLog(
            requestId, requestedModel, routedModel, routedProvider, workspaceId);

        log.setStatus(status);
        log.setInputTokens(inputTokens);
        log.setOutputTokens(outputTokens);
        log.setLatencyMs(latencyMs);
        log.setCost(cost);

        return log;
    }

    public static AiGatewayRequestLog forError(
        String requestId, String requestedModel, String routedModel, String routedProvider, Long workspaceId,
        int status, String errorMessage, Integer latencyMs) {

        Validate.notNull(workspaceId, "workspaceId must not be null for an error log");

        AiGatewayRequestLog log = new AiGatewayRequestLog(
            requestId, requestedModel, routedModel, routedProvider, workspaceId);

        log.setStatus(status);
        log.setErrorMessage(errorMessage);
        log.setLatencyMs(latencyMs);

        return log;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiGatewayRequestLog aiGatewayRequestLog)) {
            return false;
        }

        return Objects.equals(id, aiGatewayRequestLog.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public Long getApiKeyId() {
        return apiKeyId;
    }

    public boolean isCacheHit() {
        return cacheHit;
    }

    public BigDecimal getCost() {
        return cost;
    }

    /**
     * Currency-aware view of {@link #getCost()}. Prefer this when aggregating costs across rows (e.g., in reports or
     * alert thresholds) so mixed-currency arithmetic fails at the {@link Money#add}/{@link Money#compareTo} call site
     * instead of silently producing the wrong total. Persistence is still single-currency USD.
     */
    public Money getCostAsMoney() {
        return cost == null ? null : Money.usd(cost);
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Long getId() {
        return id;
    }

    public Integer getInputTokens() {
        return inputTokens;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getRequestedModel() {
        return requestedModel;
    }

    public String getRoutedModel() {
        return routedModel;
    }

    public String getRoutedProvider() {
        return routedProvider;
    }

    public Long getRoutingPolicyId() {
        return routingPolicyId;
    }

    public AiGatewayRoutingStrategyType getRoutingStrategy() {
        return routingStrategy == null ? null : AiGatewayRoutingStrategyType.values()[routingStrategy];
    }

    public Integer getStatus() {
        return status;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setApiKeyId(Long apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    public void setCacheHit(boolean cacheHit) {
        this.cacheHit = cacheHit;
    }

    public void setCost(BigDecimal cost) {
        if (cost != null) {
            Validate.isTrue(cost.signum() >= 0, "cost must not be negative");
        }

        this.cost = cost;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setInputTokens(Integer inputTokens) {
        if (inputTokens != null) {
            Validate.isTrue(inputTokens >= 0, "inputTokens must not be negative");
        }

        this.inputTokens = inputTokens;
    }

    public void setLatencyMs(Integer latencyMs) {
        if (latencyMs != null) {
            Validate.isTrue(latencyMs >= 0, "latencyMs must not be negative");
        }

        this.latencyMs = latencyMs;
    }

    public void setOutputTokens(Integer outputTokens) {
        if (outputTokens != null) {
            Validate.isTrue(outputTokens >= 0, "outputTokens must not be negative");
        }

        this.outputTokens = outputTokens;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setRequestId(String requestId) {
        Validate.notBlank(requestId, "requestId must not be blank");

        this.requestId = requestId;
    }

    public void setRequestedModel(String requestedModel) {
        Validate.notBlank(requestedModel, "requestedModel must not be blank");

        this.requestedModel = requestedModel;
    }

    public void setRoutedModel(String routedModel) {
        this.routedModel = routedModel;
    }

    public void setRoutedProvider(String routedProvider) {
        this.routedProvider = routedProvider;
    }

    public void setRoutingPolicyId(Long routingPolicyId) {
        this.routingPolicyId = routingPolicyId;
    }

    public void setRoutingStrategy(AiGatewayRoutingStrategyType routingStrategy) {
        this.routingStrategy = routingStrategy == null ? null : routingStrategy.ordinal();
    }

    public void setStatus(Integer status) {
        if (status != null) {
            Validate.isTrue(status >= 100 && status <= 599, "status must be a valid HTTP status code (100-599)");
        }

        this.status = status;
    }

    /**
     * Tenant-isolation guard: workspaceId can only transition from null → non-null, never overwritten or cleared. The
     * 2-arg constructor exists because the facade builds the log before parsing tags; this setter lets tags resolution
     * stamp the workspaceId exactly once. Any attempt to re-stamp (cross-tenant, or even the same value) is rejected to
     * prevent a later code path from retroactively changing the tenant owner of a log row.
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
        return "AiGatewayRequestLog{" +
            "id=" + id +
            ", requestId='" + requestId + '\'' +
            ", requestedModel='" + requestedModel + '\'' +
            ", routedModel='" + routedModel + '\'' +
            ", status=" + status +
            ", createdDate=" + createdDate +
            '}';
    }
}

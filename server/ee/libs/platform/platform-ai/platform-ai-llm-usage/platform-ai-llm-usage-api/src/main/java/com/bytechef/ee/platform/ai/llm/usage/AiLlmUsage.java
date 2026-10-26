/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.llm.usage;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("ai_llm_usage")
public class AiLlmUsage {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("owner_id")
    private Long ownerId;

    @Column("source")
    private Integer source;

    @Column("api_key_id")
    private Long apiKeyId;

    @Column("project_id")
    private Long projectId;

    @Column("workspace_id")
    private @Nullable Long workspaceId;

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

    @Column("routing_strategy")
    private Integer routingStrategy;

    @Column("cache_hit")
    private boolean cacheHit;

    @Column("input_tokens")
    private Integer inputTokens;

    @Column("output_tokens")
    private Integer outputTokens;

    @Column
    private BigDecimal cost;

    @Column("latency_ms")
    private Integer latencyMs;

    @Column
    private Integer status;

    @Column("error_message")
    private String errorMessage;

    @Column("agent_name")
    private String agentName;

    @Column("parent_agent")
    private String parentAgent;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    AiLlmUsage() {
    }

    public AiLlmUsage(String requestId, String requestedModel) {
        Validate.notBlank(requestId, "requestId must not be blank");
        Validate.notBlank(requestedModel, "requestedModel must not be blank");

        this.requestId = requestId;
        this.requestedModel = requestedModel;
    }

    public AiLlmUsage(
        String requestId, String requestedModel, String routedModel, String routedProvider) {
        Validate.notBlank(requestId, "requestId must not be blank");
        Validate.notBlank(requestedModel, "requestedModel must not be blank");

        this.requestId = requestId;
        this.requestedModel = requestedModel;
        this.routedModel = routedModel;
        this.routedProvider = routedProvider;
    }

    public static AiLlmUsage forSuccess(
        String requestId, String requestedModel, String routedModel, String routedProvider,
        int status, Integer inputTokens, Integer outputTokens, Integer latencyMs, BigDecimal cost) {
        AiLlmUsage usage = new AiLlmUsage(requestId, requestedModel, routedModel, routedProvider);

        usage.setStatus(status);
        usage.setInputTokens(inputTokens);
        usage.setOutputTokens(outputTokens);
        usage.setLatencyMs(latencyMs);
        usage.setCost(cost);
        usage.setSource(LlmUsageSource.AI_GATEWAY);

        return usage;
    }

    public static AiLlmUsage forError(
        String requestId, String requestedModel, String routedModel, String routedProvider,
        int status, String errorMessage, Integer latencyMs) {
        AiLlmUsage usage = new AiLlmUsage(requestId, requestedModel, routedModel, routedProvider);

        usage.setStatus(status);
        usage.setErrorMessage(errorMessage);
        usage.setLatencyMs(latencyMs);
        usage.setSource(LlmUsageSource.AI_GATEWAY);

        return usage;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiLlmUsage other)) {
            return false;
        }

        return Objects.equals(id, other.id);
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

    public Integer getRoutingStrategy() {
        return routingStrategy;
    }

    public Integer getStatus() {
        return status;
    }

    public @Nullable Long getUserId() {
        return userId;
    }

    public @Nullable Long getOwnerId() {
        return ownerId;
    }

    public @Nullable LlmUsageSource getSource() {
        if (source == null) {
            return null;
        }

        LlmUsageSource[] values = LlmUsageSource.values();

        if (source < 0 || source >= values.length) {
            throw new IllegalStateException("Invalid LlmUsageSource ordinal: " + source);
        }

        return values[source];
    }

    public @Nullable String getAgentName() {
        return agentName;
    }

    public @Nullable String getParentAgent() {
        return parentAgent;
    }

    public @Nullable Long getWorkspaceId() {
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

    public void setRoutingStrategy(@Nullable Integer routingStrategyOrdinal) {
        this.routingStrategy = routingStrategyOrdinal;
    }

    public void setStatus(Integer status) {
        if (status != null) {
            Validate.isTrue(status >= 100 && status <= 599, "status must be a valid HTTP status code (100-599)");
        }

        this.status = status;
    }

    public void setUserId(@Nullable Long userId) {
        this.userId = userId;
    }

    public void setOwnerId(@Nullable Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setSource(@Nullable LlmUsageSource source) {
        this.source = source == null ? null : source.ordinal();
    }

    public void setAgentName(@Nullable String agentName) {
        this.agentName = agentName;
    }

    public void setParentAgent(@Nullable String parentAgent) {
        this.parentAgent = parentAgent;
    }

    public void setWorkspaceId(@Nullable Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    @Override
    public String toString() {
        return "AiLlmUsage{" +
            "id=" + id +
            ", source=" + source +
            ", requestId='" + requestId + '\'' +
            ", requestedModel='" + requestedModel + '\'' +
            ", routedModel='" + routedModel + '\'' +
            ", agentName='" + agentName + '\'' +
            ", status=" + status +
            ", createdDate=" + createdDate +
            '}';
    }
}

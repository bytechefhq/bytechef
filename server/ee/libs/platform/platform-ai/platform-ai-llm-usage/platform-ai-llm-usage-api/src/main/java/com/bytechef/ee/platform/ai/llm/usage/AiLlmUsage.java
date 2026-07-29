/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * Insert-only metering row for one LLM completion. Promoted from the AI Gateway's original
 * {@code ai_gateway_request_log} table to be the single canonical LLM bookkeeping store for every agent surface (AI
 * Gateway, AI Hub, future ad-hoc AI agents).
 *
 * <p>
 * The entity carries every dimension the gateway already tracked (routing strategy, requested vs routed model,
 * api_key_id, project_id, cache_hit, HTTP status, error_message, latency) plus four columns that previously lived on AI
 * Hub's separate {@code ai_hub_usage} table — {@code user_id}, {@code owner_id} (the task / run id),
 * {@code agent_name}, {@code parent_agent} — so spend-by-agent dashboards keep working against the unified table. The
 * {@link #source} discriminator (a {@link LlmUsageSource} ordinal) lets analytics queries split the table by surface
 * even though every row sits in one schema. A row belongs to at most one workspace, carried by the nullable
 * {@link #workspaceId} column.
 *
 * <p>
 * Two write APIs land into the same row:
 * </p>
 *
 * <ul>
 * <li>The gateway's facade keeps building rows imperatively via {@link #forSuccess} / {@link #forError} factories for
 * full control over routing dims; rows carry {@code source = AI_GATEWAY}.</li>
 * <li>AI Hub (and future ad-hoc agents) write through the shared {@link LlmUsageRecorder} contract; rows carry
 * {@code source = AI_HUB} (or other surface ordinal) and leave the gateway-only routing dims null.</li>
 * </ul>
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

    /**
     * {@link LlmUsageSource} ordinal — append-only enum. Stored as the ordinal so existing rows keep their meaning when
     * new sources are appended.
     */
    @Column("source")
    private Integer source;

    @Column("api_key_id")
    private Long apiKeyId;

    @Column("project_id")
    private Long projectId;

    /**
     * Workspace that owns this usage row, or {@code null} when the call was recorded outside any workspace. Boxed on
     * purpose: a primitive would collapse "no workspace" into workspace 0, which is a real workspace id.
     */
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

    /**
     * Stored as the gateway's {@code AiGatewayRoutingStrategyType} ordinal. APPEND-ONLY: new strategy values must be
     * added at the end, never reordered or inserted — usage rows keep their meaning by ordinal index. The shared entity
     * exposes the raw int (callers wrap into the typed enum at use site) so this module doesn't depend on the
     * gateway-specific enum.
     */
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

    /**
     * Returns the raw routing-strategy ordinal as persisted. Gateway callers wrap it back to the typed
     * {@code AiGatewayRoutingStrategyType} via {@code values()[ordinal]} at use site so this shared module does not
     * depend on the gateway-specific enum.
     */
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

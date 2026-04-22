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
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_observability_trace")
public class AiObservabilityTrace {

    private static final AiObservabilityTraceSource[] SOURCE_VALUES = AiObservabilityTraceSource.values();
    private static final AiObservabilityTraceStatus[] STATUS_VALUES = AiObservabilityTraceStatus.values();

    @CreatedDate
    private Instant createdDate;

    private String externalTraceId;

    @Id
    private Long id;

    private String input;

    @LastModifiedDate
    private Instant lastModifiedDate;

    private String metadata;

    private String name;

    private String output;

    private Long projectId;

    private Long sessionId;

    private int source;

    private int status;

    @MappedCollection(idColumn = "ai_observability_trace")
    private Set<AiObservabilityTraceTag> tags;

    private BigDecimal totalCost;

    private Integer totalInputTokens;

    private Integer totalLatencyMs;

    private Integer totalOutputTokens;

    @org.springframework.data.relational.core.mapping.Column("pii_redacted")
    private boolean piiRedacted;

    @org.springframework.data.relational.core.mapping.Column("api_key_id")
    private Long apiKeyId;

    private String userId;

    @Version
    private int version;

    private Long workspaceId;

    private AiObservabilityTrace() {
    }

    public AiObservabilityTrace(Long workspaceId, AiObservabilityTraceSource source) {
        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notNull(source, "source must not be null");

        this.source = source.ordinal();
        this.status = AiObservabilityTraceStatus.ACTIVE.ordinal();
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiObservabilityTrace aiObservabilityTrace)) {
            return false;
        }

        return Objects.equals(id, aiObservabilityTrace.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getExternalTraceId() {
        return externalTraceId;
    }

    public Long getId() {
        return id;
    }

    public String getInput() {
        return input;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getMetadata() {
        return metadata;
    }

    public String getName() {
        return name;
    }

    public String getOutput() {
        return output;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public AiObservabilityTraceSource getSource() {
        return SOURCE_VALUES[source];
    }

    public AiObservabilityTraceStatus getStatus() {
        return STATUS_VALUES[status];
    }

    public Set<AiObservabilityTraceTag> getTags() {
        return tags == null ? Set.of() : Collections.unmodifiableSet(tags);
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public Integer getTotalInputTokens() {
        return totalInputTokens;
    }

    public Integer getTotalLatencyMs() {
        return totalLatencyMs;
    }

    public Integer getTotalOutputTokens() {
        return totalOutputTokens;
    }

    public boolean isPiiRedacted() {
        return piiRedacted;
    }

    public Long getApiKeyId() {
        return apiKeyId;
    }

    public String getUserId() {
        return userId;
    }

    public int getVersion() {
        return version;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setExternalTraceId(String externalTraceId) {
        this.externalTraceId = externalTraceId;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public void setStatus(AiObservabilityTraceStatus status) {
        Validate.notNull(status, "status must not be null");

        AiObservabilityTraceStatus current = STATUS_VALUES[this.status];

        // Once a trace reaches a terminal state, it must not revert to ACTIVE.
        if (current != AiObservabilityTraceStatus.ACTIVE && status == AiObservabilityTraceStatus.ACTIVE) {
            throw new IllegalStateException(
                "Cannot transition trace status from " + current + " back to ACTIVE");
        }

        this.status = status.ordinal();
    }

    @SuppressFBWarnings("EI2")
    public void setTags(Set<AiObservabilityTraceTag> tags) {
        this.tags = tags;
    }

    public void setTotalCost(BigDecimal totalCost) {
        if (totalCost != null && totalCost.signum() < 0) {
            throw new IllegalArgumentException("totalCost must not be negative: " + totalCost);
        }

        this.totalCost = totalCost;
    }

    public void setTotalInputTokens(Integer totalInputTokens) {
        if (totalInputTokens != null && totalInputTokens < 0) {
            throw new IllegalArgumentException("totalInputTokens must not be negative: " + totalInputTokens);
        }

        this.totalInputTokens = totalInputTokens;
    }

    public void setTotalLatencyMs(Integer totalLatencyMs) {
        if (totalLatencyMs != null && totalLatencyMs < 0) {
            throw new IllegalArgumentException("totalLatencyMs must not be negative: " + totalLatencyMs);
        }

        this.totalLatencyMs = totalLatencyMs;
    }

    public void setTotalOutputTokens(Integer totalOutputTokens) {
        if (totalOutputTokens != null && totalOutputTokens < 0) {
            throw new IllegalArgumentException("totalOutputTokens must not be negative: " + totalOutputTokens);
        }

        this.totalOutputTokens = totalOutputTokens;
    }

    public void setPiiRedacted(boolean piiRedacted) {
        this.piiRedacted = piiRedacted;
    }

    public void setApiKeyId(Long apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "AiObservabilityTrace{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", name='" + name + '\'' +
            ", source=" + getSource() +
            ", status=" + getStatus() +
            ", createdDate=" + createdDate +
            '}';
    }
}

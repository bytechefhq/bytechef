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
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_observability_span")
public class AiObservabilitySpan {

    private static final AiObservabilitySpanLevel[] LEVEL_VALUES = AiObservabilitySpanLevel.values();
    private static final AiObservabilitySpanStatus[] STATUS_VALUES = AiObservabilitySpanStatus.values();
    private static final AiObservabilitySpanType[] TYPE_VALUES = AiObservabilitySpanType.values();

    private BigDecimal cost;

    @CreatedDate
    private Instant createdDate;

    private Instant endTime;

    @Id
    private Long id;

    private String input;

    private Integer inputTokens;

    @LastModifiedDate
    private Instant lastModifiedDate;

    private Integer latencyMs;

    private int level;

    private String metadata;

    private String model;

    private String name;

    private String output;

    private Integer outputTokens;

    private Long parentSpanId;

    private Long promptId;

    private Long promptVersionId;

    private String provider;

    private Instant startTime;

    private int status;

    private Long traceId;

    private int type;

    @Version
    private int version;

    private AiObservabilitySpan() {
    }

    public AiObservabilitySpan(Long traceId, AiObservabilitySpanType type) {
        Validate.notNull(traceId, "traceId must not be null");
        Validate.notNull(type, "type must not be null");

        this.level = AiObservabilitySpanLevel.DEFAULT.ordinal();
        this.startTime = Instant.now();
        this.status = AiObservabilitySpanStatus.ACTIVE.ordinal();
        this.traceId = traceId;
        this.type = type.ordinal();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiObservabilitySpan aiObservabilitySpan)) {
            return false;
        }

        return Objects.equals(id, aiObservabilitySpan.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public BigDecimal getCost() {
        return cost;
    }

    /**
     * Currency-aware view of {@link #getCost()}. Prefer this when summing span costs for a trace or aggregating across
     * workspaces. Single-currency USD today — the {@link Money} abstraction catches a future per-provider currency
     * change at the call site.
     */
    public Money getCostAsMoney() {
        return cost == null ? null : Money.usd(cost);
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public Long getId() {
        return id;
    }

    public String getInput() {
        return input;
    }

    public Integer getInputTokens() {
        return inputTokens;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public AiObservabilitySpanLevel getLevel() {
        return LEVEL_VALUES[level];
    }

    public String getMetadata() {
        return metadata;
    }

    public String getModel() {
        return model;
    }

    public String getName() {
        return name;
    }

    public String getOutput() {
        return output;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }

    public Long getParentSpanId() {
        return parentSpanId;
    }

    public Long getPromptId() {
        return promptId;
    }

    public Long getPromptVersionId() {
        return promptVersionId;
    }

    public String getProvider() {
        return provider;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public AiObservabilitySpanStatus getStatus() {
        return STATUS_VALUES[status];
    }

    public Long getTraceId() {
        return traceId;
    }

    public AiObservabilitySpanType getType() {
        return TYPE_VALUES[type];
    }

    public int getVersion() {
        return version;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public void setEndTime(Instant endTime) {
        if (endTime != null && startTime != null && endTime.isBefore(startTime)) {
            throw new IllegalArgumentException(
                "endTime (" + endTime + ") must not be before startTime (" + startTime + ")");
        }

        this.endTime = endTime;
    }

    /**
     * Atomically transitions the span to a terminal state: sets endTime, computes latencyMs, and sets status. Prefer
     * this over calling the individual setters so the three fields stay consistent.
     */
    public void close(Instant endTime, AiObservabilitySpanStatus terminalStatus) {
        Validate.notNull(endTime, "endTime must not be null");
        Validate.notNull(terminalStatus, "terminalStatus must not be null");
        Validate.isTrue(
            terminalStatus != AiObservabilitySpanStatus.ACTIVE,
            "close() requires a terminal status, not ACTIVE");

        setEndTime(endTime);

        if (startTime != null) {
            long durationMillis = java.time.Duration.between(startTime, endTime)
                .toMillis();

            this.latencyMs = (int) Math.max(0L, durationMillis);
        }

        this.status = terminalStatus.ordinal();
    }

    public void setInput(String input) {
        this.input = input;
    }

    public void setInputTokens(Integer inputTokens) {
        this.inputTokens = inputTokens;
    }

    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }

    public void setLevel(AiObservabilitySpanLevel level) {
        this.level = level.ordinal();
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public void setOutputTokens(Integer outputTokens) {
        this.outputTokens = outputTokens;
    }

    public void setParentSpanId(Long parentSpanId) {
        this.parentSpanId = parentSpanId;
    }

    public void setPromptId(Long promptId) {
        this.promptId = promptId;
    }

    public void setPromptVersionId(Long promptVersionId) {
        this.promptVersionId = promptVersionId;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setStartTime(Instant startTime) {
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new IllegalArgumentException(
                "startTime (" + startTime + ") must not be after existing endTime (" + endTime + ")");
        }

        this.startTime = startTime;
    }

    public void setStatus(AiObservabilitySpanStatus status) {
        Validate.notNull(status, "status must not be null");

        if (status == AiObservabilitySpanStatus.ACTIVE && this.status != AiObservabilitySpanStatus.ACTIVE.ordinal()) {
            throw new IllegalStateException(
                "Cannot revert span to ACTIVE once it has reached a terminal status (" + getStatus() + ")");
        }

        this.status = status.ordinal();
    }

    @Override
    public String toString() {
        return "AiObservabilitySpan{" +
            "id=" + id +
            ", traceId=" + traceId +
            ", type=" + getType() +
            ", status=" + getStatus() +
            ", level=" + getLevel() +
            ", startTime=" + startTime +
            '}';
    }
}

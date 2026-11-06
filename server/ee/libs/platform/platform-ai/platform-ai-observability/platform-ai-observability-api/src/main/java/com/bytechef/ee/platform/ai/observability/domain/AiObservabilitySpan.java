/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.domain;

import com.bytechef.ee.platform.ai.llm.usage.Money;
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
 * @author Ivica Cardic
 * @version ee
 */
@Table("ai_observability_span")
public final class AiObservabilitySpan {

    private static final AiObservabilitySpanLevel[] LEVEL_VALUES = AiObservabilitySpanLevel.values();
    private static final AiObservabilitySpanStatus[] STATUS_VALUES = AiObservabilitySpanStatus.values();
    private static final AiObservabilitySpanType[] TYPE_VALUES = AiObservabilitySpanType.values();

    private BigDecimal cost;

    @CreatedDate
    private Instant createdDate;

    private Instant endTime;

    private String externalSpanId;

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

    public Money getCostAsMoney() {
        return cost == null ? null : Money.usd(cost);
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public String getExternalSpanId() {
        return externalSpanId;
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
        return safeOrdinalLookup("level", level, LEVEL_VALUES);
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
        return safeOrdinalLookup("status", status, STATUS_VALUES);
    }

    public Long getTraceId() {
        return traceId;
    }

    public AiObservabilitySpanType getType() {
        return safeOrdinalLookup("type", type, TYPE_VALUES);
    }

    private <E extends Enum<E>> E safeOrdinalLookup(String field, int ordinal, E[] values) {
        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalStateException(
                "AiObservabilitySpan id=" + id + " has corrupt " + field + " ordinal " + ordinal +
                    " (valid range 0.." + (values.length - 1) + ")");
        }

        return values[ordinal];
    }

    private void assertActive(String fieldName) {
        if (this.status != AiObservabilitySpanStatus.ACTIVE.ordinal()) {
            throw new IllegalStateException(
                "AiObservabilitySpan id=" + id + " is in terminal state " + getStatus() +
                    "; cannot mutate '" + fieldName + "' after close()");
        }
    }

    public int getVersion() {
        return version;
    }

    public void setCost(BigDecimal cost) {
        assertActive("cost");

        this.cost = cost;
    }

    private void setEndTime(Instant endTime) {
        assertActive("endTime");

        if (endTime != null && startTime != null && endTime.isBefore(startTime)) {
            throw new IllegalArgumentException(
                "endTime (" + endTime + ") must not be before startTime (" + startTime + ")");
        }

        this.endTime = endTime;

        if (endTime != null && startTime != null && this.latencyMs == null) {
            long durationMillis = java.time.Duration.between(startTime, endTime)
                .toMillis();

            this.latencyMs = (int) Math.max(0L, durationMillis);
        }
    }

    public void setExternalSpanId(String externalSpanId) {
        assertActive("externalSpanId");

        if (externalSpanId != null && this.externalSpanId != null
            && !externalSpanId.equals(this.externalSpanId)) {

            throw new IllegalStateException(
                "AiObservabilitySpan id=" + id + " already has externalSpanId='" + this.externalSpanId
                    + "'; cannot re-stamp to '" + externalSpanId
                    + "' — the (trace_id, external_span_id) idempotency contract forbids drift");
        }

        this.externalSpanId = externalSpanId;
    }

    public void close(Instant endTime, AiObservabilitySpanStatus terminalStatus) {
        Validate.notNull(endTime, "endTime must not be null");
        Validate.notNull(terminalStatus, "terminalStatus must not be null");
        Validate.isTrue(
            terminalStatus != AiObservabilitySpanStatus.ACTIVE,
            "close() requires a terminal status, not ACTIVE");

        if (this.status != AiObservabilitySpanStatus.ACTIVE.ordinal()) {
            throw new IllegalStateException(
                "Span " + id + " is already in terminal state " + STATUS_VALUES[this.status] +
                    " and cannot be re-closed");
        }

        setEndTime(endTime);

        this.status = terminalStatus.ordinal();
    }

    public void setInput(String input) {
        assertActive("input");

        this.input = input;
    }

    public void setInputTokens(Integer inputTokens) {
        assertActive("inputTokens");

        this.inputTokens = inputTokens;
    }

    public void setLatencyMs(Integer latencyMs) {
        assertActive("latencyMs");

        this.latencyMs = latencyMs;
    }

    public void setLevel(AiObservabilitySpanLevel level) {
        assertActive("level");

        this.level = level.ordinal();
    }

    public void setMetadata(String metadata) {
        assertActive("metadata");

        this.metadata = metadata;
    }

    public void setModel(String model) {
        assertActive("model");

        this.model = model;
    }

    public void setName(String name) {
        assertActive("name");

        this.name = name;
    }

    public void setOutput(String output) {
        assertActive("output");

        this.output = output;
    }

    public void setOutputTokens(Integer outputTokens) {
        assertActive("outputTokens");

        this.outputTokens = outputTokens;
    }

    public void setParentSpanId(Long parentSpanId) {
        assertActive("parentSpanId");

        this.parentSpanId = parentSpanId;
    }

    public void setPromptId(Long promptId) {
        assertActive("promptId");

        this.promptId = promptId;
    }

    public void setPromptVersionId(Long promptVersionId) {
        assertActive("promptVersionId");

        this.promptVersionId = promptVersionId;
    }

    public void setProvider(String provider) {
        assertActive("provider");

        this.provider = provider;
    }

    public void setStartTime(Instant startTime) {
        assertActive("startTime");

        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new IllegalArgumentException(
                "startTime (" + startTime + ") must not be after existing endTime (" + endTime + ")");
        }

        this.startTime = startTime;
    }

    public void setStatus(AiObservabilitySpanStatus status) {
        Validate.notNull(status, "status must not be null");

        assertActive("status");

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

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.domain;

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
 * @author Ivica Cardic
 * @version ee
 */
@Table("ai_eval_score")
public final class AiEvalScore {

    private String comment;

    @CreatedDate
    private Instant createdDate;

    private String createdBy;

    private int dataType;

    private Long evalRuleId;

    @Id
    private Long id;

    private String metadata;

    private String name;

    private int source;

    private String sourceIdentifier;

    private Long spanId;

    private String stringValue;

    private Long traceId;

    private BigDecimal value;

    @Column("workspace_id")
    private @Nullable Long workspaceId;

    private AiEvalScore() {
    }

    AiEvalScore(Long traceId, String name, AiEvalScoreDataType dataType, AiEvalScoreSource source) {
        Validate.notNull(traceId, "traceId must not be null");
        Validate.notBlank(name, "name must not be blank");
        Validate.notNull(dataType, "dataType must not be null");
        Validate.notNull(source, "source must not be null");

        this.dataType = dataType.ordinal();
        this.name = name;
        this.source = source.ordinal();
        this.traceId = traceId;
    }

    public static AiEvalScore numeric(Long traceId, String name, AiEvalScoreSource source, BigDecimal value) {
        AiEvalScore score = new AiEvalScore(traceId, name, AiEvalScoreDataType.NUMERIC, source);

        score.applyTypedValue(new AiEvalScoreValue.Numeric(value));

        return score;
    }

    public static AiEvalScore numeric(
        Long traceId, String name, AiEvalScoreSource source, BigDecimal value,
        String sourceIdentifier, String metadata) {

        AiEvalScore score = numeric(traceId, name, source, value);

        score.setSourceIdentifier(sourceIdentifier);
        score.setMetadata(metadata);

        return score;
    }

    public static AiEvalScore bool(Long traceId, String name, AiEvalScoreSource source, boolean value) {
        AiEvalScore score = new AiEvalScore(traceId, name, AiEvalScoreDataType.BOOLEAN, source);

        score.applyTypedValue(new AiEvalScoreValue.Bool(value));

        return score;
    }

    public static AiEvalScore bool(
        Long traceId, String name, AiEvalScoreSource source, boolean value,
        String sourceIdentifier, String metadata) {

        AiEvalScore score = bool(traceId, name, source, value);

        score.setSourceIdentifier(sourceIdentifier);
        score.setMetadata(metadata);

        return score;
    }

    public static AiEvalScore categorical(Long traceId, String name, AiEvalScoreSource source, String label) {
        AiEvalScore score = new AiEvalScore(traceId, name, AiEvalScoreDataType.CATEGORICAL, source);

        score.applyTypedValue(new AiEvalScoreValue.Categorical(label));

        return score;
    }

    public static AiEvalScore categorical(
        Long traceId, String name, AiEvalScoreSource source, String label,
        String sourceIdentifier, String metadata) {

        AiEvalScore score = categorical(traceId, name, source, label);

        score.setSourceIdentifier(sourceIdentifier);
        score.setMetadata(metadata);

        return score;
    }

    public void applyTypedValue(AiEvalScoreValue typedValue) {
        Validate.notNull(typedValue, "typedValue must not be null");

        AiEvalScoreDataType currentDataType = getDataType();

        Validate.isTrue(typedValue.dataType() == currentDataType,
            "typedValue dataType %s does not match score dataType %s", typedValue.dataType(), currentDataType);

        switch (typedValue) {
            case AiEvalScoreValue.Numeric numeric -> {
                this.value = numeric.value();
                this.stringValue = null;
            }
            case AiEvalScoreValue.Bool bool -> {
                this.value = bool.asNumeric();
                this.stringValue = bool.asString();
            }
            case AiEvalScoreValue.Categorical categorical -> {
                this.value = null;
                this.stringValue = categorical.label();
            }
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiEvalScore aiEvalScore)) {
            return false;
        }

        return Objects.equals(id, aiEvalScore.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public String getComment() {
        return comment;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public AiEvalScoreDataType getDataType() {
        return safeOrdinalLookup("dataType", dataType, AiEvalScoreDataType.values());
    }

    public Long getEvalRuleId() {
        return evalRuleId;
    }

    public Long getId() {
        return id;
    }

    public String getMetadata() {
        return metadata;
    }

    public String getName() {
        return name;
    }

    public AiEvalScoreSource getSource() {
        return safeOrdinalLookup("source", source, AiEvalScoreSource.values());
    }

    private <E extends Enum<E>> E safeOrdinalLookup(String field, int ordinal, E[] values) {
        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalStateException(
                "AiEvalScore." + field + " ordinal " + ordinal + " is out of range [0, " + values.length
                    + ") — row may be corrupt (id=" + id + ")");
        }

        return values[ordinal];
    }

    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    public Long getSpanId() {
        return spanId;
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private String getStringValue() {
        return stringValue;
    }

    public Long getTraceId() {
        return traceId;
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private BigDecimal getValue() {
        return value;
    }

    public AiEvalScoreValue getTypedValue() {
        return AiEvalScoreValue.fromColumns(id, getDataType(), value, stringValue);
    }

    public @Nullable Long getWorkspaceId() {
        return workspaceId;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public void setEvalRuleId(Long evalRuleId) {
        this.evalRuleId = evalRuleId;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    public void setSpanId(Long spanId) {
        this.spanId = spanId;
    }

    public void setWorkspaceId(@Nullable Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    @Deprecated
    private void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    @Deprecated
    private void setValue(BigDecimal value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "AiEvalScore{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", traceId=" + traceId +
            ", name='" + name + '\'' +
            ", source=" + getSource() +
            ", sourceIdentifier='" + sourceIdentifier + '\'' +
            ", dataType=" + getDataType() +
            ", metadata='" + metadata + '\'' +
            '}';
    }
}

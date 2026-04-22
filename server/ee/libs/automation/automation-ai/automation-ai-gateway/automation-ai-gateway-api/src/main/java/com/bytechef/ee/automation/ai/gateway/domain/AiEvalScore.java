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
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * An eval score row. Construct via the typed factories {@link #numeric}, {@link #bool}, and {@link #categorical}; read
 * via {@link #getTypedValue()}. The raw {@link #getValue()}/{@link #getStringValue()} accessors exist for Spring Data
 * JDBC hydration.
 *
 * @version ee
 */
@Table("ai_eval_score")
public class AiEvalScore {

    private String comment;

    @CreatedDate
    private Instant createdDate;

    private String createdBy;

    private int dataType;

    private Long evalRuleId;

    @Id
    private Long id;

    private String name;

    private int source;

    private Long spanId;

    private String stringValue;

    private Long traceId;

    private BigDecimal value;

    private Long workspaceId;

    private AiEvalScore() {
    }

    AiEvalScore(
        Long workspaceId, Long traceId, String name,
        AiEvalScoreDataType dataType, AiEvalScoreSource source) {

        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notNull(traceId, "traceId must not be null");
        Validate.notBlank(name, "name must not be blank");
        Validate.notNull(dataType, "dataType must not be null");
        Validate.notNull(source, "source must not be null");

        this.dataType = dataType.ordinal();
        this.name = name;
        this.source = source.ordinal();
        this.traceId = traceId;
        this.workspaceId = workspaceId;
    }

    public static AiEvalScore numeric(
        Long workspaceId, Long traceId, String name, AiEvalScoreSource source, BigDecimal value) {

        AiEvalScore score = new AiEvalScore(workspaceId, traceId, name, AiEvalScoreDataType.NUMERIC, source);

        score.applyTypedValue(new AiEvalScoreValue.Numeric(value));

        return score;
    }

    public static AiEvalScore bool(
        Long workspaceId, Long traceId, String name, AiEvalScoreSource source, boolean value) {

        AiEvalScore score = new AiEvalScore(workspaceId, traceId, name, AiEvalScoreDataType.BOOLEAN, source);

        score.applyTypedValue(new AiEvalScoreValue.Bool(value));

        return score;
    }

    public static AiEvalScore categorical(
        Long workspaceId, Long traceId, String name, AiEvalScoreSource source, String label) {

        AiEvalScore score = new AiEvalScore(workspaceId, traceId, name, AiEvalScoreDataType.CATEGORICAL, source);

        score.applyTypedValue(new AiEvalScoreValue.Categorical(label));

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
        return AiEvalScoreDataType.values()[dataType];
    }

    public Long getEvalRuleId() {
        return evalRuleId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AiEvalScoreSource getSource() {
        return AiEvalScoreSource.values()[source];
    }

    public Long getSpanId() {
        return spanId;
    }

    /** @deprecated prefer {@link #getTypedValue()}. */
    @Deprecated
    public String getStringValue() {
        return stringValue;
    }

    public Long getTraceId() {
        return traceId;
    }

    /** @deprecated prefer {@link #getTypedValue()}. */
    @Deprecated
    public BigDecimal getValue() {
        return value;
    }

    @Nullable
    public AiEvalScoreValue getTypedValue() {
        return AiEvalScoreValue.fromColumns(getDataType(), value, stringValue);
    }

    public Long getWorkspaceId() {
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

    public void setSpanId(Long spanId) {
        this.spanId = spanId;
    }

    /** @deprecated use {@link #applyTypedValue} — this setter does not check dataType consistency. */
    @Deprecated
    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    /**
     * @deprecated see {@link #setStringValue}.
     */
    @Deprecated
    public void setValue(BigDecimal value) {
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
            ", dataType=" + getDataType() +
            '}';
    }
}

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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * An eval score row. Construct via the typed factories {@link #numeric}, {@link #bool}, and {@link #categorical}; read
 * via {@link #getTypedValue()}. The raw {@code value} / {@code stringValue} columns are accessed only by Spring Data
 * JDBC's reflective row mapping (which works at any visibility) — the private accessors keep the columns unreachable
 * even from same-package callers, who must go through the typed view so dataType-mismatch bugs surface at the type
 * level instead of as quiet null reads. Tightening from package-private to private closes the residual loophole where a
 * future class added to {@code com.bytechef.ee.automation.ai.gateway.domain} could desync
 * {@code (dataType, value, stringValue)} by calling the raw setters.
 *
 * <p>
 * Workspace association lives on {@link WorkspaceAiEvalScore} — the entity is workspace-agnostic. Callers go through
 * {@code AiEvalScoreService.getWorkspaceId(id)} (Variant A).
 *
 * @author Ivica Cardic
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

    private String metadata;

    private String name;

    private int source;

    private String sourceIdentifier;

    private Long spanId;

    private String stringValue;

    private Long traceId;

    private BigDecimal value;

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

    /**
     * Resolves the persisted ordinal back to its {@link AiEvalScoreDataType}. A bad ordinal (out-of-range row from a
     * mis-migration, manual SQL tampering, or schema-version skew between writers) would otherwise surface as an
     * {@link ArrayIndexOutOfBoundsException} from deep inside a getter — confusing to diagnose. The explicit
     * {@link IllegalStateException} names the table/column and the offending value so operators can repair the row
     * directly.
     */
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

    /**
     * Resolves the persisted ordinal back to its {@link AiEvalScoreSource}. Same defensive contract as
     * {@link #getDataType()}: appending a new enum value at the end of {@code AiEvalScoreSource} is ordinal-safe; any
     * other reorder or removal corrupts every previously-written row, and this guard surfaces the corruption with the
     * row id rather than at the call site as a raw {@link ArrayIndexOutOfBoundsException}.
     */
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

    /**
     * Private hydration accessor; Spring Data JDBC's reflective row mapping reaches it at any visibility. Same-package
     * callers must use {@link #getTypedValue()} so dataType-mismatch bugs surface at the type level rather than as a
     * quiet null read. PMD cannot see the reflective Spring Data caller and would otherwise flag the method as unused;
     * the suppression is narrowly scoped and load-bearing.
     */
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private String getStringValue() {
        return stringValue;
    }

    public Long getTraceId() {
        return traceId;
    }

    /**
     * Private hydration accessor; same rationale as {@link #getStringValue()}.
     */
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private BigDecimal getValue() {
        return value;
    }

    /**
     * Returns the typed value reconstructed from the persisted columns. Throws {@link IllegalStateException} when the
     * row's columns cannot materialize the declared {@code dataType} — see
     * {@link AiEvalScoreValue#fromColumns(Long, AiEvalScoreDataType, java.math.BigDecimal, String)}. Callers that
     * tolerate corrupt rows must catch the exception explicitly rather than rely on the previous silent-null behavior,
     * which the strictness of {@code applyTypedValue} on the write path was already incompatible with.
     */
    public AiEvalScoreValue getTypedValue() {
        return AiEvalScoreValue.fromColumns(id, getDataType(), value, stringValue);
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

    /**
     * Private hydration setter for Spring Data JDBC's reflective row mapping. All callers — including same-package code
     * — must use {@link #applyTypedValue(AiEvalScoreValue)} so the dataType invariant ({@code dataType} agrees with the
     * chosen {@code value}/{@code stringValue} branch) cannot be bypassed.
     *
     * @deprecated use {@link #applyTypedValue(AiEvalScoreValue)} — this setter does not check dataType consistency.
     */
    @Deprecated
    private void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    /**
     * Private hydration setter for Spring Data JDBC's reflective row mapping; same rationale as
     * {@link #setStringValue(String)}.
     *
     * @deprecated use {@link #applyTypedValue(AiEvalScoreValue)} — this setter does not check dataType consistency.
     */
    @Deprecated
    private void setValue(BigDecimal value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "AiEvalScore{" +
            "id=" + id +
            ", traceId=" + traceId +
            ", name='" + name + '\'' +
            ", source=" + getSource() +
            ", sourceIdentifier='" + sourceIdentifier + '\'' +
            ", dataType=" + getDataType() +
            ", metadata='" + metadata + '\'' +
            '}';
    }
}

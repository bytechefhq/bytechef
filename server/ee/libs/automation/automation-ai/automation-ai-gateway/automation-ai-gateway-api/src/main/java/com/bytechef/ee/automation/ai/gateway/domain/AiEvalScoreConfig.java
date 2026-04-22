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
@Table("ai_eval_score_config")
public class AiEvalScoreConfig {

    private String categories;

    @CreatedDate
    private Instant createdDate;

    private Integer dataType;

    private String description;

    @Id
    private Long id;

    @LastModifiedDate
    private Instant lastModifiedDate;

    private BigDecimal maxValue;

    private BigDecimal minValue;

    private String name;

    @Version
    private int version;

    private Long workspaceId;

    private AiEvalScoreConfig() {
    }

    public AiEvalScoreConfig(Long workspaceId, String name) {
        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notBlank(name, "name must not be blank");

        this.name = name;
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiEvalScoreConfig aiEvalScoreConfig)) {
            return false;
        }

        return Objects.equals(id, aiEvalScoreConfig.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public String getCategories() {
        return categories;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public AiEvalScoreDataType getDataType() {
        if (dataType == null) {
            return null;
        }

        return AiEvalScoreDataType.values()[dataType];
    }

    public String getDescription() {
        return description;
    }

    public Long getId() {
        return id;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public BigDecimal getMaxValue() {
        return maxValue;
    }

    public BigDecimal getMinValue() {
        return minValue;
    }

    public String getName() {
        return name;
    }

    public int getVersion() {
        return version;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }

    public void setDataType(AiEvalScoreDataType dataType) {
        this.dataType = dataType == null ? null : dataType.ordinal();
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setMaxValue(BigDecimal maxValue) {
        validateRange(this.minValue, maxValue);

        this.maxValue = maxValue;
    }

    public void setMinValue(BigDecimal minValue) {
        validateRange(minValue, this.maxValue);

        this.minValue = minValue;
    }

    /**
     * Cross-field check: {@code minValue <= maxValue}. Without this, a config can be created where no score will ever
     * pass the range check — silent misconfiguration that returns zero evaluated scores.
     */
    private static void validateRange(BigDecimal min, BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException(
                "minValue (" + min + ") must be <= maxValue (" + max + ")");
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "AiEvalScoreConfig{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", name='" + name + '\'' +
            ", dataType=" + getDataType() +
            '}';
    }
}

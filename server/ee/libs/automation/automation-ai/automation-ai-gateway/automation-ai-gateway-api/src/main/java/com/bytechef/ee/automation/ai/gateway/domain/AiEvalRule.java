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
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/**
 * {@code CT_CONSTRUCTOR_THROW} is suppressed: the public constructor intentionally validates its arguments with
 * {@link Validate#notNull}/{@link Validate#notBlank} and throws on invalid input rather than letting a half-built row
 * reach the repository layer where bad data would corrupt downstream evaluations.
 *
 * @version ee
 */
@SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
@Table("ai_eval_rule")
public class AiEvalRule {

    @CreatedDate
    private Instant createdDate;

    private Integer delaySeconds;

    private boolean enabled;

    private String filters;

    @Id
    private Long id;

    @LastModifiedDate
    private Instant lastModifiedDate;

    private String model;

    private String name;

    private Long projectId;

    private String promptTemplate;

    private BigDecimal samplingRate;

    private Long scoreConfigId;

    @Version
    private int version;

    private Long workspaceId;

    private AiEvalRule() {
    }

    public AiEvalRule(
        Long workspaceId, String name, Long scoreConfigId,
        String promptTemplate, String model, BigDecimal samplingRate) {

        Validate.notNull(workspaceId, "workspaceId must not be null");
        Validate.notBlank(name, "name must not be blank");
        Validate.notNull(scoreConfigId, "scoreConfigId must not be null");
        Validate.notBlank(promptTemplate, "promptTemplate must not be blank");
        Validate.notBlank(model, "model must not be blank");
        Validate.notNull(samplingRate, "samplingRate must not be null");
        validateSamplingRate(samplingRate);

        this.enabled = false;
        this.model = model;
        this.name = name;
        this.promptTemplate = promptTemplate;
        this.samplingRate = samplingRate;
        this.scoreConfigId = scoreConfigId;
        this.workspaceId = workspaceId;
    }

    /**
     * Sampling rate must be in {@code [0, 1]}. Values outside this range are money-sensitive: a rule with
     * {@code samplingRate = 2.0} silently means "sample every trace" and triggers cost-bearing LLM judge calls on
     * traces the operator never intended to evaluate.
     */
    private static void validateSamplingRate(BigDecimal samplingRate) {
        if (samplingRate.signum() < 0 || samplingRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(
                "samplingRate must be in [0, 1], got: " + samplingRate);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiEvalRule aiEvalRule)) {
            return false;
        }

        return Objects.equals(id, aiEvalRule.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Integer getDelaySeconds() {
        return delaySeconds;
    }

    public String getFilters() {
        return filters;
    }

    public Long getId() {
        return id;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getModel() {
        return model;
    }

    public String getName() {
        return name;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public BigDecimal getSamplingRate() {
        return samplingRate;
    }

    public Long getScoreConfigId() {
        return scoreConfigId;
    }

    public int getVersion() {
        return version;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setDelaySeconds(Integer delaySeconds) {
        this.delaySeconds = delaySeconds;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setFilters(String filters) {
        // Validate against the sealed AiObservabilityAlertFilter hierarchy (shared with alert rules) so malformed
        // filter JSON fails at write time rather than during evaluation hours or days later.
        if (filters != null && !filters.isBlank()) {
            AiObservabilityAlertFilter parsed = AiObservabilityAlertFilter.fromJson(filters);

            this.filters = AiObservabilityAlertFilter.toJson(parsed);
        } else {
            this.filters = filters;
        }
    }

    public void setModel(String model) {
        Validate.notBlank(model, "model must not be blank");

        this.model = model;
    }

    public void setName(String name) {
        Validate.notBlank(name, "name must not be blank");

        this.name = name;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setPromptTemplate(String promptTemplate) {
        Validate.notBlank(promptTemplate, "promptTemplate must not be blank");

        this.promptTemplate = promptTemplate;
    }

    public void setSamplingRate(BigDecimal samplingRate) {
        Validate.notNull(samplingRate, "samplingRate must not be null");
        validateSamplingRate(samplingRate);

        this.samplingRate = samplingRate;
    }

    public void setScoreConfigId(Long scoreConfigId) {
        Validate.notNull(scoreConfigId, "scoreConfigId must not be null");

        this.scoreConfigId = scoreConfigId;
    }

    @Override
    public String toString() {
        return "AiEvalRule{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", name='" + name + '\'' +
            ", model='" + model + '\'' +
            ", enabled=" + enabled +
            ", samplingRate=" + samplingRate +
            '}';
    }
}

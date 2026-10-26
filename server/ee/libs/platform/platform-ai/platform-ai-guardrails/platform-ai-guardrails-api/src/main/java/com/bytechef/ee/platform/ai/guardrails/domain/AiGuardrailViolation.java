/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.guardrails.domain;

import java.math.BigDecimal;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("ai_guardrail_violation")
public class AiGuardrailViolation {

    private static final AiGuardrailViolationAction[] ACTION_VALUES = AiGuardrailViolationAction.values();

    private int action;

    private String category;

    private BigDecimal confidence;

    @CreatedDate
    private Instant createdDate;

    private @Nullable Integer environment;

    @Id
    private Long id;

    private int kind;

    private @Nullable String principal;

    private int spanLength;

    private int spanStart;

    private String surface;

    private @Nullable Long workspaceId;

    private AiGuardrailViolation() {
    }

    public AiGuardrailViolation(
        String category, int kind, int spanStart, int spanLength, BigDecimal confidence,
        AiGuardrailViolationAction action, String surface, @Nullable Long workspaceId,
        @Nullable Integer environment, @Nullable String principal) {

        this.category = category;
        this.kind = kind;
        this.spanStart = spanStart;
        this.spanLength = spanLength;
        this.confidence = confidence;
        this.action = action.ordinal();
        this.surface = surface;
        this.workspaceId = workspaceId;
        this.environment = environment;
        this.principal = principal;
    }

    public AiGuardrailViolationAction getAction() {
        return ACTION_VALUES[action];
    }

    public String getCategory() {
        return category;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public @Nullable Integer getEnvironment() {
        return environment;
    }

    public Long getId() {
        return id;
    }

    public int getKind() {
        return kind;
    }

    public @Nullable String getPrincipal() {
        return principal;
    }

    public int getSpanLength() {
        return spanLength;
    }

    public int getSpanStart() {
        return spanStart;
    }

    public String getSurface() {
        return surface;
    }

    public @Nullable Long getWorkspaceId() {
        return workspaceId;
    }

    @Override
    public String toString() {
        return "AiGuardrailViolation{" +
            "id=" + id +
            ", category='" + category + '\'' +
            ", kind=" + kind +
            ", spanStart=" + spanStart +
            ", spanLength=" + spanLength +
            ", confidence=" + confidence +
            ", action=" + getAction() +
            ", surface='" + surface + '\'' +
            ", workspaceId=" + workspaceId +
            ", environment=" + environment +
            ", createdDate=" + createdDate +
            '}';
    }
}

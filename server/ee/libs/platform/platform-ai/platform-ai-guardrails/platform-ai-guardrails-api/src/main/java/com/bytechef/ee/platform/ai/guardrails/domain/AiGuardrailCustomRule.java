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
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("ai_guardrail_custom_rule")
public class AiGuardrailCustomRule {

    private @Nullable String contextKeywords;

    private @Nullable BigDecimal contextScore;

    private @Nullable Integer contextWindow;

    @CreatedDate
    private Instant createdDate;

    private boolean enabled;

    @Id
    private Long id;

    private int kind;

    @LastModifiedDate
    private Instant lastModifiedDate;

    private String pattern;

    private BigDecimal score;

    private String type;

    @Version
    private int version;

    private long workspaceId;

    private AiGuardrailCustomRule() {
    }

    public AiGuardrailCustomRule(
        long workspaceId, String type, String pattern, int kind, BigDecimal score,
        @Nullable String contextKeywords, @Nullable Integer contextWindow, @Nullable BigDecimal contextScore) {

        this.workspaceId = workspaceId;
        this.type = type;
        this.pattern = pattern;
        this.kind = kind;
        this.score = score;
        this.contextKeywords = contextKeywords;
        this.contextWindow = contextWindow;
        this.contextScore = contextScore;
    }

    public @Nullable String getContextKeywords() {
        return contextKeywords;
    }

    public @Nullable BigDecimal getContextScore() {
        return contextScore;
    }

    public @Nullable Integer getContextWindow() {
        return contextWindow;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Long getId() {
        return id;
    }

    public int getKind() {
        return kind;
    }

    public Instant getLastModifiedDate() {
        return lastModifiedDate;
    }

    public String getPattern() {
        return pattern;
    }

    public BigDecimal getScore() {
        return score;
    }

    public String getType() {
        return type;
    }

    public long getWorkspaceId() {
        return workspaceId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setContextKeywords(@Nullable String contextKeywords) {
        this.contextKeywords = contextKeywords;
    }

    public void setContextScore(@Nullable BigDecimal contextScore) {
        this.contextScore = contextScore;
    }

    public void setContextWindow(@Nullable Integer contextWindow) {
        this.contextWindow = contextWindow;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setKind(int kind) {
        this.kind = kind;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "AiGuardrailCustomRule{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", type='" + type + '\'' +
            ", kind=" + kind +
            ", score=" + score +
            ", enabled=" + enabled +
            ", createdDate=" + createdDate +
            '}';
    }
}

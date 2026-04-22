/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

import java.time.Instant;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * @version ee
 */
@Table("ai_prompt_version")
public class AiPromptVersion {

    @Column
    private boolean active;

    @Column("commit_message")
    private String commitMessage;

    @Column
    private String content;

    @Column("created_by")
    private String createdBy;

    @Column("created_date")
    @CreatedDate
    private Instant createdDate;

    @Column
    private String environment;

    @Id
    private Long id;

    @Column("prompt_id")
    private Long promptId;

    @Column
    private int type;

    @Column
    private String variables;

    @Version
    private int version;

    @Column("version_number")
    private int versionNumber;

    private AiPromptVersion() {
    }

    public AiPromptVersion(
        Long promptId, int versionNumber, AiPromptVersionType type, String content, String createdBy) {

        Validate.notNull(promptId, "promptId must not be null");
        Validate.notNull(type, "type must not be null");
        Validate.notBlank(content, "content must not be blank");
        Validate.notBlank(createdBy, "createdBy must not be blank");
        Validate.isTrue(versionNumber > 0, "versionNumber must be positive (monotonic user-facing version)");

        this.content = content;
        this.createdBy = createdBy;
        this.promptId = promptId;
        this.type = type.ordinal();
        this.versionNumber = versionNumber;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof AiPromptVersion aiPromptVersion)) {
            return false;
        }

        return Objects.equals(id, aiPromptVersion.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public boolean isActive() {
        return active;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public String getContent() {
        return content;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public String getEnvironment() {
        return environment;
    }

    public Long getId() {
        return id;
    }

    public Long getPromptId() {
        return promptId;
    }

    public AiPromptVersionType getType() {
        return AiPromptVersionType.values()[type];
    }

    public String getVariables() {
        return variables;
    }

    /**
     * Typed view of the variables JSON. Prefer this in substitution code — an unknown variable kind surfaces as a parse
     * error instead of silently leaving the placeholder unfilled at render time.
     */
    public java.util.List<AiPromptVariable> getTypedVariables() {
        return AiPromptVariable.parseList(variables);
    }

    public void setTypedVariables(java.util.List<AiPromptVariable> typedVariables) {
        this.variables = AiPromptVariable.toJson(typedVariables);
    }

    public int getVersion() {
        return version;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    /**
     * Sets the active flag for this version. Setting {@code active=true} on a version whose {@link #getEnvironment()}
     * is blank is meaningless — the active-in-environment invariant is scoped per (promptId, environment) — so this
     * setter rejects that combination. The "only one active version per (promptId, environment)" constraint itself is
     * enforced at the service layer ({@code AiPromptVersionService.setActiveVersion}) because it requires a cross-row
     * read; a DB-level partial unique index on {@code (prompt_id, environment) WHERE active} would be a
     * defense-in-depth follow-up.
     */
    public void setActive(boolean active) {
        if (active && (environment == null || environment.isBlank())) {
            throw new IllegalArgumentException(
                "cannot mark a prompt version active without an environment — set environment first");
        }

        this.active = active;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    public void setContent(String content) {
        Validate.notBlank(content, "content must not be blank");

        this.content = content;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public void setVariables(String variables) {
        if (variables != null && !variables.isBlank()) {
            AiPromptVariable.parseList(variables);
        }

        this.variables = variables;
    }

    @Override
    public String toString() {
        return "AiPromptVersion{" +
            "id=" + id +
            ", promptId=" + promptId +
            ", versionNumber=" + versionNumber +
            ", type=" + getType() +
            ", environment='" + environment + '\'' +
            ", active=" + active +
            ", createdBy='" + createdBy + '\'' +
            '}';
    }
}

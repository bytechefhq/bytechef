/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.personalagent;

import com.bytechef.platform.configuration.domain.Environment;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Persistent definition of a user-created Personal Agent. Owned by a user within an environment; the workspace
 * association is the nullable {@link #workspaceId} column, so an agent belongs to at most one workspace and null means
 * none applies. The slug regex on {@link #name} is the only entity-level uniqueness gate: the DB does NOT enforce a
 * (user, environment, name) unique constraint, so two agents with the same name and user/env coexist as distinct rows
 * distinguished by id.
 *
 * <p>
 * <b>Why a slugified name plus a separate title:</b> the name is the stable identifier the LLM tools and the URL
 * routing key off ({@code /personal-agents/my-research-bot}); the title is the display label that the user can change
 * without invalidating cached references. Same shape as {@code AiAutoMemory} for the same reasons.
 * </p>
 *
 * <p>
 * <b>Instructions field:</b> appended to the AI Hub system prompt when a task with {@code kind = PERSONAL_AGENT} is
 * dispatched. The agent's instructions are a non-zero-trust extension — a malicious user with write access to their own
 * agent CAN bypass restrictions imposed only at the system-prompt layer (the model itself is the security boundary, not
 * the prompt). Any future per-tool ACL belongs as a separate {@code allowed_tools} column rather than encoded in
 * instructions.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Table("ai_hub_personal_agent")
public class AiHubPersonalAgent {

    /**
     * Slugified-name format. Lowercase letters, digits, hyphens, and underscores; non-empty; max 64 chars. Same regex
     * as {@code AiAutoMemory.NAME_PATTERN}; the matching {@code ck_personal_agent_name_slug} CHECK constraint is in the
     * Liquibase init migration so any non-service path is rejected at the DB.
     */
    public static final Pattern NAME_PATTERN = Pattern.compile("^[a-z0-9_-]{1,64}$");

    /** Hard upper bound on display title length, matching the {@code VARCHAR(255)} column. */
    public static final int MAX_TITLE_LENGTH = 255;

    /** Hard upper bound on description length. Renders as a tooltip / short blurb in the sidebar. */
    public static final int MAX_DESCRIPTION_LENGTH = 1024;

    /**
     * Hard upper bound on the instructions field. 65 KB is well above any reasonable per-agent system-prompt extension
     * and matches the column's {@code TEXT}-equivalent storage. Capped at the entity layer so an oversized value
     * rejects with a typed exception rather than producing an opaque DB error.
     */
    public static final int MAX_INSTRUCTIONS_LENGTH = 65_536;

    @Id
    private Long id;

    @Column("user_id")
    private long userId;

    @Column("name")
    private String name;

    @Column("title")
    private String title;

    @Column("description")
    private String description;

    @Column("instructions")
    private String instructions;

    @Column("environment")
    private int environment;

    /**
     * Optional per-agent LLM provider override (e.g. {@code "openai"}, {@code "anthropic"}). Null = use workspace
     * default. When set, {@link #llmModel} must also be set. Validated at the service layer.
     */
    @Column("llm_provider")
    private @Nullable String llmProvider;

    /**
     * Optional per-agent LLM model override (e.g. {@code "gpt-4o-mini"}, {@code "claude-3-5-sonnet-20241022"}). Null =
     * use workspace default. When set, {@link #llmProvider} must also be set. The (provider, model) pair must exist in
     * the workspace's available providers at save time; validated by the service.
     */
    @Column("llm_model")
    private @Nullable String llmModel;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    /**
     * Workspace the agent belongs to. Nullable: an agent belongs to at most one workspace, and null means none applies.
     * Workspace-scoped queries never match a null, so a workspace-less agent is invisible to them.
     */
    @Column("workspace_id")
    private @Nullable Long workspaceId;

    public AiHubPersonalAgent() {
    }

    /**
     * Bind-once constructor for new (unpersisted) rows. The per-field setter for {@code userId} is package-private so a
     * loaded row cannot be retargeted by mistake; the workspace association is the separately settable
     * {@link #workspaceId} column.
     */
    public AiHubPersonalAgent(long userId) {
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("AiHubPersonalAgent.name must not be null");
        }

        if (!NAME_PATTERN.matcher(name)
            .matches()) {
            throw new IllegalArgumentException(
                "AiHubPersonalAgent.name must match " + NAME_PATTERN.pattern() + " — got '" + name + "'");
        }

        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(@Nullable String title) {
        if (title != null && title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                "AiHubPersonalAgent.title must be at most " + MAX_TITLE_LENGTH + " characters (got "
                    + title.length() + ")");
        }

        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                "AiHubPersonalAgent.description must be at most " + MAX_DESCRIPTION_LENGTH + " characters (got "
                    + description.length() + ")");
        }

        this.description = description;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(@Nullable String instructions) {
        if (instructions != null && instructions.length() > MAX_INSTRUCTIONS_LENGTH) {
            throw new IllegalArgumentException(
                "AiHubPersonalAgent.instructions must be at most " + MAX_INSTRUCTIONS_LENGTH
                    + " characters (got "
                    + instructions.length() + ")");
        }

        this.instructions = instructions;
    }

    public Environment getEnvironment() {
        Environment[] environments = Environment.values();

        if (environment < 0 || environment >= environments.length) {
            throw new IllegalStateException("Invalid environment value: " + environment);
        }

        return environments[environment];
    }

    public int getEnvironmentOrdinal() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        if (environment != null) {
            this.environment = environment.ordinal();
        }
    }

    public @Nullable String getLlmProvider() {
        return llmProvider;
    }

    public void setLlmProvider(@Nullable String llmProvider) {
        this.llmProvider = llmProvider;
    }

    public @Nullable String getLlmModel() {
        return llmModel;
    }

    public void setLlmModel(@Nullable String llmModel) {
        this.llmModel = llmModel;
    }

    /**
     * True when both {@link #llmProvider} and {@link #llmModel} are set — the agent has a per-agent model override.
     * False when both are null (use workspace default). Throws when only one is set, which indicates corrupt state (the
     * service-layer guard should have prevented this at save time).
     */
    public boolean hasLlmOverride() {
        if (llmProvider == null && llmModel == null) {
            return false;
        }

        if (llmProvider != null && llmModel != null) {
            return true;
        }

        throw new IllegalStateException(
            "AiHubPersonalAgent.llmProvider and llmModel must both be set or both null; got llmProvider="
                + llmProvider + ", llmModel=" + llmModel);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public @Nullable Long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(@Nullable Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        AiHubPersonalAgent that = (AiHubPersonalAgent) other;

        if (id == null) {
            return this == that;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /**
     * Excludes {@code title}, {@code description}, and {@code instructions} on purpose. The instructions field is the
     * load-bearing reason for redaction: a user can paste credentials, internal URLs, or sensitive prompt material into
     * instructions, and Spring Data JDBC trace logging or a {@code logger.warn(agent)} call site would otherwise
     * persist that to log aggregators. Mirrors the redaction policy {@code AiAutoMemory.toString} uses.
     */
    @Override
    public String toString() {
        return "AiHubPersonalAgent{" +
            "id=" + id +
            ", userId=" + userId +
            ", name='" + name + '\'' +
            ", environment=" + environment +
            ", createdAt=" + createdAt +
            ", updatedAt=" + updatedAt +
            '}';
    }
}

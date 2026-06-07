/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.ai.auto.memory;

import com.bytechef.platform.configuration.domain.Environment;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Persistent long-term memory entry for the agent. Owned by a principal — either a user or a deployment, discriminated
 * by {@code principal_type}/{@code principal_id}; the workspace association lives on {@code workspace_ai_auto_memory}
 * (mirrors workspace_mcp_server / workspace_ai_hub_personal_agent), so the same memory row can in principle be shared
 * across workspaces. The DB does NOT enforce a (user, env, name) unique constraint — the slug regex on {@link #name} is
 * the only entity-level shape gate; a service-layer duplicate-name check in create() is the policy gate.
 *
 * <p>
 * This is a Spring Data JDBC row-mapper entity. Setters mostly exist for the construct-then-save flow. The load-bearing
 * setter-side guards are {@link #setName(String)} (enforces the slug regex so a controller bug or LLM tool call that
 * hands in raw user input cannot bypass the slugifier), {@link #setMemoryType(AiAutoMemoryType)} (rejects null so the
 * underlying ordinal column never silently defaults to {@code 0} / {@code USER}), and the length caps on
 * {@link #setTitle(String)}, {@link #setDescription(String)}, and {@link #setContent(String)} (prevent the LLM from
 * constructing oversized values that would hit the DB constraint or push past UI render limits). Workspace/user
 * ownership is enforced in {@code AiAutoMemoryServiceImpl}.
 * </p>
 *
 * @author Ivica Cardic
 */
@Table("ai_auto_memory")
public class AiAutoMemory {

    /**
     * Slugified-name format. Lowercase letters, digits, hyphens, and underscores; non-empty; max 64 chars.
     *
     * <p>
     * <b>Validation scope:</b> enforced at three layers — (1) {@link #setName(String)} so service-layer callers cannot
     * install a malformed name; (2) the {@code ck_ai_memory_name_slug} CHECK constraint on the
     * {@code ai_auto_memory.name} column so any non-service path (raw SQL, manual reflection, a future repository layer
     * that forgets the setter) is rejected at the DB; (3) the same regex string ({@code ^[a-z0-9_- ]{1,64}$}) is
     * duplicated in the Liquibase migration so the two layers stay synchronised — if you change this pattern, also
     * update {@code 20260424000001_ai_memory_init.xml}.
     * </p>
     *
     * <p>
     * Spring Data JDBC hydration writes directly to the {@code name} field via reflection and therefore bypasses this
     * setter — by design, so a corrupt or pre-existing legacy DB row still rehydrates and can be repaired via the same
     * setter on the next update. Do not push validation into {@link #getName()}; Spring Data round-trips would then
     * loop on hydration.
     * </p>
     */
    public static final Pattern NAME_PATTERN = Pattern.compile("^[a-z0-9_-]{1,64}$");

    @Id
    private Long id;

    @Column("principal_id")
    private long principalId;

    @Column("principal_type")
    private int principalType;

    @Column("name")
    private String name;

    @Column("title")
    private String title;

    @Column("description")
    private String description;

    @Column("memory_type")
    private int memoryType;

    @Column("environment")
    private int environment;

    @Column("content")
    private String content;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    public AiAutoMemory() {
    }

    /**
     * Bind-once constructor for new (unpersisted) rows. The per-field setters for {@code principalId} and
     * {@code principalType} are package-private so a loaded row cannot be retargeted by mistake. Workspace association
     * lives on {@code workspace_ai_auto_memory} and is set independently when the membership row is created.
     */
    public AiAutoMemory(AiAutoMemoryPrincipalType principalType, long principalId) {
        this.principalType = Objects.requireNonNull(principalType, "principalType")
            .ordinal();
        this.principalId = principalId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getPrincipalId() {
        return principalId;
    }

    /**
     * Package-private so post-load retargeting cannot happen across module boundaries. New rows must use
     * {@link #AiAutoMemory(AiAutoMemoryPrincipalType, long)}; Spring Data JDBC hydration writes the field directly via
     * reflection.
     */
    void setPrincipalId(long principalId) {
        this.principalId = principalId;
    }

    public AiAutoMemoryPrincipalType getPrincipalType() {
        AiAutoMemoryPrincipalType[] values = AiAutoMemoryPrincipalType.values();

        if (principalType < 0 || principalType >= values.length) {
            throw new IllegalStateException("Unknown AiAutoMemoryPrincipalType ordinal: " + principalType);
        }

        return values[principalType];
    }

    void setPrincipalType(AiAutoMemoryPrincipalType principalType) {
        this.principalType = Objects.requireNonNull(principalType, "principalType")
            .ordinal();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("AiAutoMemory.name must not be null");
        }

        if (!NAME_PATTERN.matcher(name)
            .matches()) {
            throw new IllegalArgumentException(
                "AiAutoMemory.name must match " + NAME_PATTERN.pattern() + " — got '" + name + "'");
        }

        this.name = name;
    }

    /**
     * Hard upper bound on memory title length, matching the {@code VARCHAR(255)} column. Prevents the LLM from
     * constructing oversized titles that would hit the DB constraint or push past UI render limits.
     */
    public static final int MAX_TITLE_LENGTH = 255;
    /** Hard upper bound on memory description length. */
    public static final int MAX_DESCRIPTION_LENGTH = 1024;
    /**
     * Hard upper bound on memory content length. The agent prompt instructs the LLM to keep memories short, but the
     * user can still paste here.
     */
    public static final int MAX_CONTENT_LENGTH = 65_536;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title != null && title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                "AiAutoMemory.title must be at most " + MAX_TITLE_LENGTH + " characters (got "
                    + title.length() + ")");
        }

        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                "AiAutoMemory.description must be at most " + MAX_DESCRIPTION_LENGTH + " characters (got "
                    + description.length() + ")");
        }

        this.description = description;
    }

    public AiAutoMemoryType getMemoryType() {
        // Inlined bounds check (was EnumOrdinals.fromOrdinal) so this entity has no CC-specific dep.
        // The DB row stored an int whose value should fall inside the current enum range; a corrupt or
        // future-version persisted value surfaces as a typed IllegalStateException with the offending
        // ordinal in the message — easier to diagnose than the raw ArrayIndexOutOfBoundsException.
        AiAutoMemoryType[] all = AiAutoMemoryType.values();

        if (memoryType < 0 || memoryType >= all.length) {
            throw new IllegalStateException(
                "AiAutoMemory.memoryType ordinal " + memoryType + " is out of range for AiAutoMemoryType (0.."
                    + (all.length - 1) + ")");
        }

        return all[memoryType];
    }

    public void setMemoryType(AiAutoMemoryType memoryType) {
        if (memoryType == null) {
            throw new IllegalArgumentException("AiAutoMemory.memoryType must not be null");
        }

        this.memoryType = memoryType.ordinal();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        if (content != null && content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException(
                "AiAutoMemory.content must be at most " + MAX_CONTENT_LENGTH + " characters (got "
                    + content.length() + ")");
        }

        this.content = content;
    }

    /**
     * Returns the {@link Environment} this memory belongs to. Memories are environment-scoped so that DEVELOPMENT
     * preferences do not bleed into PRODUCTION (and vice versa). Bounds-checked to surface a corrupt ordinal as a typed
     * {@link IllegalStateException}.
     */
    public Environment getEnvironment() {
        Environment[] environments = Environment.values();

        if (environment < 0 || environment >= environments.length) {
            throw new IllegalStateException("Invalid environment value: " + environment);
        }

        return environments[environment];
    }

    public long getEnvironmentId() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        if (environment != null) {
            this.environment = environment.ordinal();
        }
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

    @Override
    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        AiAutoMemory that = (AiAutoMemory) other;

        // Two transient (id == null) instances must not collide in a Set before persistence.
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
     * Excludes {@code name}, {@code title}, {@code description}, and {@code content} on purpose. Memory rows hold
     * user-authored prompt content / preferences that may carry PII. Spring Data JDBC trace logging and any
     * {@code logger.warn(memory)} call site would otherwise persist that content to log aggregators. The remaining
     * fields (id, principalType, principalId, memoryType, timestamps) are sufficient for ops to correlate a row with a
     * database record. Note: the redaction policy is unique to {@code AiAutoMemory} — sibling entities like
     * {@link AiHubUsage} log all numeric fields (token counts, model names, costs) because none of them carry PII; if
     * that ever changes, this Javadoc and the matching {@code toString} should be revisited together.
     */
    @Override
    public String toString() {
        return "AiAutoMemory{" +
            "id=" + id +
            ", principalType=" + principalType +
            ", principalId=" + principalId +
            ", memoryType=" + memoryType +
            ", environment=" + environment +
            ", createdAt=" + createdAt +
            ", updatedAt=" + updatedAt +
            '}';
    }
}

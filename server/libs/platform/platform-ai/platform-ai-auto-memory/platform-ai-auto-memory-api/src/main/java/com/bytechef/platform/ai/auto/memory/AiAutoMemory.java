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
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Persistent long-term memory entry for the agent. Owned by an {@link AiAutoMemoryOwner} — a principal (user,
 * deployment or integration instance, discriminated by {@code principal_type}/{@code principal_id}) within one
 * workspace and one environment. The owner is bound at construction and cannot change afterwards. On the JDBC binding
 * the {@code uk_ai_auto_memory_owner_env_name} unique constraint keeps {@link #name} unique per owner; the file-storage
 * bindings rely on the service-layer duplicate check alone.
 *
 * <p>
 * This is a Spring Data JDBC row-mapper entity. The load-bearing setter-side guards are {@link #setName(String)}
 * (enforces the slug regex so a controller bug or LLM tool call that hands in raw input that is not already a valid
 * slug is rejected), {@link #setTitle(String)} and {@link #setContent(String)} (both required, so they reject a null or
 * blank value), {@link #setMemoryType(AiAutoMemoryType)} (rejects null), the length caps on title, description and
 * content, and the single-line rule on title and description (both are rendered as one frontmatter line each, so a line
 * break would end the field early). A memory whose type was never set has no type at all — {@link #getMemoryType()}
 * throws and the NOT NULL column rejects the insert — rather than silently defaulting to ordinal {@code 0} /
 * {@code USER}. Ownership is enforced by the repository's owner-scoped lookups, and every binding refuses a save or
 * delete aimed at another owner's row.
 * </p>
 *
 * <p>
 * {@link #version} is an optimistic lock: a save or delete of a stale copy fails with Spring's
 * {@code OptimisticLockingFailureException} on both the JDBC and the file-storage bindings, so a concurrent edit is
 * rejected instead of silently overwritten. The JDBC check is atomic; the file-storage one is check-then-write.
 * </p>
 *
 * @author Ivica Cardic
 */
@Table("ai_auto_memory")
public final class AiAutoMemory {

    /**
     * Slugified-name format. Lowercase letters, digits, hyphens, and underscores; non-empty; max 64 chars.
     *
     * <p>
     * Enforced by {@link #setName(String)} and by the {@code ck_ai_auto_memory_name_slug} CHECK constraint on
     * {@code ai_auto_memory.name}, which rejects writes that skip the setter (raw SQL). The regex is duplicated in
     * {@code 20260424000001_ai_auto_memory_init.xml}; if you change this pattern, change it there too.
     * </p>
     *
     * <p>
     * Hydration bypasses this setter — on the JDBC binding by writing the {@code name} field via reflection, on the
     * file-storage binding through {@link #restore} — so a stored memory that no longer matches still loads and can be
     * repaired through the setter.
     * </p>
     */
    public static final Pattern NAME_PATTERN = Pattern.compile("^[a-z0-9_-]{1,64}$");

    /**
     * Hard upper bound on memory title length, matching the {@code VARCHAR(255)} column.
     */
    public static final int MAX_TITLE_LENGTH = 255;

    /** Hard upper bound on memory description length. */
    public static final int MAX_DESCRIPTION_LENGTH = 1024;

    /** Hard upper bound on memory content length. */
    public static final int MAX_CONTENT_LENGTH = 65_536;

    @Id
    private Long id;

    @Column("workspace_id")
    private long workspaceId;

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
    private Integer memoryType;

    @Column("environment")
    private int environment;

    @Column("content")
    private String content;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column("version")
    private Long version;

    /**
     * For Spring Data JDBC hydration only: it leaves the owner fields at {@code 0}, which {@link #getOwner()} rejects
     * until hydration fills them. New rows go through {@link #AiAutoMemory(AiAutoMemoryOwner)}.
     */
    @PersistenceCreator
    AiAutoMemory() {
    }

    /**
     * Binds a new (unpersisted) row to its owner. There are no setters for the owner fields, so a loaded row cannot be
     * moved to another owner by mistake; Spring Data JDBC hydration writes those fields directly via reflection.
     */
    public AiAutoMemory(AiAutoMemoryOwner owner) {
        Objects.requireNonNull(owner, "owner");

        AiAutoMemoryPrincipalType ownerPrincipalType = owner.principalType();
        Environment ownerEnvironment = owner.environment();

        this.workspaceId = owner.workspaceId();
        this.principalType = ownerPrincipalType.ordinal();
        this.principalId = owner.principalId();
        this.environment = ownerEnvironment.ordinal();
    }

    /**
     * Rebuilds a stored memory for a repository binding that cannot write fields directly the way Spring Data JDBC
     * hydration does. Like that hydration it skips the setters' format rules — slug, length caps, single-line — so a
     * memory stored before a rule was tightened still loads, and the binding does not report it as missing. This is the
     * only way to give a memory an id and a version outside Spring Data JDBC; a binding returns the restored instance
     * from {@code save} rather than mutating the one it was handed.
     */
    public static AiAutoMemory restore(
        AiAutoMemoryOwner owner, long id, String name, String title, @Nullable String description,
        AiAutoMemoryType memoryType, String content, @Nullable LocalDateTime createdAt,
        @Nullable LocalDateTime updatedAt, long version) {

        AiAutoMemory aiAutoMemory = new AiAutoMemory(owner);

        aiAutoMemory.id = id;
        aiAutoMemory.name = Objects.requireNonNull(name, "name");
        aiAutoMemory.title = Objects.requireNonNull(title, "title");
        aiAutoMemory.description = description;
        aiAutoMemory.memoryType = Objects.requireNonNull(memoryType, "memoryType")
            .ordinal();
        aiAutoMemory.content = Objects.requireNonNull(content, "content");
        aiAutoMemory.createdAt = createdAt;
        aiAutoMemory.updatedAt = updatedAt;
        aiAutoMemory.version = version;

        return aiAutoMemory;
    }

    public Long getId() {
        return id;
    }

    public AiAutoMemoryOwner getOwner() {
        return new AiAutoMemoryOwner(workspaceId, getPrincipalType(), principalId, getEnvironment());
    }

    public long getWorkspaceId() {
        return workspaceId;
    }

    public long getPrincipalId() {
        return principalId;
    }

    public AiAutoMemoryPrincipalType getPrincipalType() {
        AiAutoMemoryPrincipalType[] values = AiAutoMemoryPrincipalType.values();

        if (principalType < 0 || principalType >= values.length) {
            throw new IllegalStateException("Unknown AiAutoMemoryPrincipalType ordinal: " + principalType);
        }

        return values[principalType];
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

    public String getTitle() {
        return title;
    }

    /**
     * Stored without surrounding whitespace, as the frontmatter parser reads it back.
     */
    public void setTitle(String title) {
        validateRequired(title, "title");

        String strippedTitle = title.strip();

        if (strippedTitle.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                "AiAutoMemory.title must be at most " + MAX_TITLE_LENGTH + " characters (got "
                    + strippedTitle.length() + ")");
        }

        validateSingleLine(strippedTitle, "title");

        this.title = strippedTitle;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Stored without surrounding whitespace, as the frontmatter parser reads it back; a blank description is none.
     */
    public void setDescription(String description) {
        if (description == null || description.isBlank()) {
            this.description = null;

            return;
        }

        String strippedDescription = description.strip();

        if (strippedDescription.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                "AiAutoMemory.description must be at most " + MAX_DESCRIPTION_LENGTH + " characters (got "
                    + strippedDescription.length() + ")");
        }

        validateSingleLine(strippedDescription, "description");

        this.description = strippedDescription;
    }

    /**
     * Bounds-checked so a corrupt or future-version persisted ordinal surfaces as a typed {@link IllegalStateException}
     * naming the offending value rather than an {@link ArrayIndexOutOfBoundsException}.
     */
    public AiAutoMemoryType getMemoryType() {
        if (memoryType == null) {
            throw new IllegalStateException("AiAutoMemory.memoryType has not been set");
        }

        AiAutoMemoryType[] memoryTypes = AiAutoMemoryType.values();

        if (memoryType < 0 || memoryType >= memoryTypes.length) {
            throw new IllegalStateException(
                "AiAutoMemory.memoryType ordinal " + memoryType + " is out of range for AiAutoMemoryType (0.."
                    + (memoryTypes.length - 1) + ")");
        }

        return memoryTypes[memoryType];
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
        validateRequired(content, "content");

        if (content.length() > MAX_CONTENT_LENGTH) {
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

    /**
     * The persisted ordinal of {@link #getEnvironment()} — not an id. Storage bindings use it as the stored value.
     */
    public int getEnvironmentOrdinal() {
        return environment;
    }

    /**
     * Whether every persisted ordinal — principal type, memory type and environment — names a constant this build
     * knows. The CHECK constraints leave the upper bound open so a value can be appended, so a row written by a newer
     * build (for example before a rollback) can carry one this build cannot map; its typed getters would throw. Storage
     * bindings skip such a row instead of failing every listing it appears in.
     */
    public boolean hasKnownOrdinals() {
        return isKnownOrdinal(principalType, AiAutoMemoryPrincipalType.values().length)
            && memoryType != null && isKnownOrdinal(memoryType, AiAutoMemoryType.values().length)
            && isKnownOrdinal(environment, Environment.values().length);
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

    /**
     * The optimistic-lock version: {@code null} until the memory is first saved, then advanced by every save. Only the
     * storage binding assigns it.
     */
    public Long getVersion() {
        return version;
    }

    private static boolean isKnownOrdinal(int ordinal, int constantCount) {
        return ordinal >= 0 && ordinal < constantCount;
    }

    private static void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("AiAutoMemory." + fieldName + " must not be blank");
        }
    }

    private static void validateSingleLine(String value, String fieldName) {
        if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("AiAutoMemory." + fieldName + " must be a single line");
        }
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
     * fields (id, workspaceId, principalType, principalId, memoryType, environment, timestamps, version) are sufficient
     * for ops to correlate a row with a database record.
     */
    @Override
    public String toString() {
        return "AiAutoMemory{" +
            "id=" + id +
            ", workspaceId=" + workspaceId +
            ", principalType=" + principalType +
            ", principalId=" + principalId +
            ", memoryType=" + memoryType +
            ", environment=" + environment +
            ", createdAt=" + createdAt +
            ", updatedAt=" + updatedAt +
            ", version=" + version +
            '}';
    }
}

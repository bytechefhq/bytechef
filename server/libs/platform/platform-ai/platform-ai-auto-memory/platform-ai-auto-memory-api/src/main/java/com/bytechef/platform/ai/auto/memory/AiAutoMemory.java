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
 * @author Ivica Cardic
 */
@Table("ai_auto_memory")
public final class AiAutoMemory {

    public static final Pattern NAME_PATTERN = Pattern.compile("^[a-z0-9_-]{1,64}$");

    public static final int MAX_TITLE_LENGTH = 255;

    public static final int MAX_DESCRIPTION_LENGTH = 1024;

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

    @PersistenceCreator
    AiAutoMemory() {
    }

    public AiAutoMemory(AiAutoMemoryOwner owner) {
        Objects.requireNonNull(owner, "owner");

        AiAutoMemoryPrincipalType ownerPrincipalType = owner.principalType();
        Environment ownerEnvironment = owner.environment();

        this.workspaceId = owner.workspaceId();
        this.principalType = ownerPrincipalType.ordinal();
        this.principalId = owner.principalId();
        this.environment = ownerEnvironment.ordinal();
    }

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

        if (id == null) {
            return this == that;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

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

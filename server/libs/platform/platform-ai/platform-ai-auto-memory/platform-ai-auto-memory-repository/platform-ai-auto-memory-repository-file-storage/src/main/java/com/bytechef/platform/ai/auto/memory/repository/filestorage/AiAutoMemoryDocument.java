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

package com.bytechef.platform.ai.auto.memory.repository.filestorage;

import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.configuration.domain.Environment;
import java.time.LocalDateTime;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * The persisted form of an {@link AiAutoMemory} on a file-storage backend: one JSON object per memory.
 *
 * <p>
 * Enums are stored as their persisted INT ordinals so the JSON matches the relational columns, and timestamps as
 * ISO-8601 strings. Ordinals are range-checked when a document is turned back into a memory, so a corrupt or
 * future-version document fails with a typed {@link IllegalStateException} naming the bad value.
 * </p>
 *
 * <p>
 * {@code version} is boxed so a document without the field still loads, as version {@code 0}.
 * </p>
 *
 * <p>
 * {@link #toDomain()} rebuilds the memory through {@link AiAutoMemory#restore}, not the setters, so a document stored
 * before a format rule was tightened still loads — as it does on the JDBC binding — instead of being skipped.
 * </p>
 *
 * @author Ivica Cardic
 */
public record AiAutoMemoryDocument(
    long id, long workspaceId, long principalId, int principalType, @Nullable String name, @Nullable String title,
    @Nullable String description, int memoryType, int environment, @Nullable String content,
    @Nullable String createdAt, @Nullable String updatedAt, @Nullable Long version) {

    /**
     * The document to store for {@code aiAutoMemory} under {@code id} at {@code version}. The id is passed rather than
     * read from the memory because a new memory has none until the repository assigns it.
     */
    public static AiAutoMemoryDocument fromDomain(AiAutoMemory aiAutoMemory, long id, long version) {
        AiAutoMemoryType memoryType = aiAutoMemory.getMemoryType();
        AiAutoMemoryPrincipalType principalType = aiAutoMemory.getPrincipalType();

        LocalDateTime createdAt = aiAutoMemory.getCreatedAt();
        LocalDateTime updatedAt = aiAutoMemory.getUpdatedAt();

        return new AiAutoMemoryDocument(
            id, aiAutoMemory.getWorkspaceId(), aiAutoMemory.getPrincipalId(), principalType.ordinal(),
            aiAutoMemory.getName(), aiAutoMemory.getTitle(), aiAutoMemory.getDescription(), memoryType.ordinal(),
            aiAutoMemory.getEnvironmentOrdinal(), aiAutoMemory.getContent(),
            createdAt == null ? null : createdAt.toString(), updatedAt == null ? null : updatedAt.toString(), version);
    }

    public AiAutoMemory toDomain() {
        AiAutoMemoryOwner owner = new AiAutoMemoryOwner(
            workspaceId, toPrincipalType(principalType), principalId,
            fromOrdinal(Environment.values(), environment, "environment"));

        return AiAutoMemory.restore(
            owner, id, Objects.requireNonNull(name, "name"), Objects.requireNonNull(title, "title"), description,
            fromOrdinal(AiAutoMemoryType.values(), memoryType, "memoryType"),
            Objects.requireNonNull(content, "content"),
            createdAt == null ? null : LocalDateTime.parse(createdAt),
            updatedAt == null ? null : LocalDateTime.parse(updatedAt), Objects.requireNonNullElse(version, 0L));
    }

    private static AiAutoMemoryPrincipalType toPrincipalType(int ordinal) {
        return fromOrdinal(AiAutoMemoryPrincipalType.values(), ordinal, "principalType");
    }

    private static <E extends Enum<E>> E fromOrdinal(E[] values, int ordinal, String fieldName) {
        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalStateException(
                "AiAutoMemoryDocument." + fieldName + " ordinal " + ordinal + " is out of range (0.."
                    + (values.length - 1) + ")");
        }

        return values[ordinal];
    }
}

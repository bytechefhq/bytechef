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
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.configuration.domain.Environment;
import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/**
 * The persisted form of an {@link AiAutoMemory} on a file-storage backend: one JSON object per memory.
 *
 * <p>
 * The owning workspace is a field on this document, exactly as it is a column on the relational row. It is nullable for
 * the same reason: a memory belongs to at most one workspace, and null means none applies.
 * </p>
 *
 * <p>
 * Enums are stored as their persisted INT ordinals so the JSON matches the relational columns, and timestamps as
 * ISO-8601 strings.
 * </p>
 *
 * @author Ivica Cardic
 */
public record AiAutoMemoryDocument(
    long id, @Nullable Long workspaceId, long principalId, int principalType, @Nullable String name,
    @Nullable String title, @Nullable String description, int memoryType, int environment, @Nullable String content,
    @Nullable String createdAt, @Nullable String updatedAt) {

    public static AiAutoMemoryDocument fromDomain(AiAutoMemory aiAutoMemory) {
        Long id = aiAutoMemory.getId();

        AiAutoMemoryType memoryType = aiAutoMemory.getMemoryType();
        AiAutoMemoryPrincipalType principalType = aiAutoMemory.getPrincipalType();

        LocalDateTime createdAt = aiAutoMemory.getCreatedAt();
        LocalDateTime updatedAt = aiAutoMemory.getUpdatedAt();

        return new AiAutoMemoryDocument(
            id == null ? 0 : id, aiAutoMemory.getWorkspaceId(), aiAutoMemory.getPrincipalId(),
            principalType == null ? 0 : principalType.ordinal(), aiAutoMemory.getName(), aiAutoMemory.getTitle(),
            aiAutoMemory.getDescription(), memoryType == null ? 0 : memoryType.ordinal(),
            (int) aiAutoMemory.getEnvironmentId(), aiAutoMemory.getContent(),
            createdAt == null ? null : createdAt.toString(), updatedAt == null ? null : updatedAt.toString());
    }

    public AiAutoMemory toDomain() {
        AiAutoMemory aiAutoMemory = new AiAutoMemory(
            AiAutoMemoryPrincipalType.values()[principalType], principalId);

        aiAutoMemory.setId(id);
        aiAutoMemory.setWorkspaceId(workspaceId);
        aiAutoMemory.setName(name);
        aiAutoMemory.setTitle(title);
        aiAutoMemory.setDescription(description);
        aiAutoMemory.setMemoryType(AiAutoMemoryType.values()[memoryType]);
        aiAutoMemory.setEnvironment(Environment.values()[environment]);
        aiAutoMemory.setContent(content);

        if (createdAt != null) {
            aiAutoMemory.setCreatedAt(LocalDateTime.parse(createdAt));
        }

        if (updatedAt != null) {
            aiAutoMemory.setUpdatedAt(LocalDateTime.parse(updatedAt));
        }

        return aiAutoMemory;
    }
}

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
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface AiAutoMemoryService {

    AiAutoMemory create(
        AiAutoMemoryOwner owner, String name, String title, @Nullable String description,
        AiAutoMemoryType memoryType, String content);

    Optional<AiAutoMemory> read(AiAutoMemoryOwner owner, String name);

    AiAutoMemory update(
        AiAutoMemoryOwner owner, String name, long expectedVersion, AiAutoMemoryPatch patch);

    AiAutoMemory updateById(
        AiAutoMemoryOwner owner, long memoryId, long expectedVersion, AiAutoMemoryPatch patch);

    AiAutoMemory delete(AiAutoMemoryOwner owner, String name);

    AiAutoMemory deleteById(AiAutoMemoryOwner owner, long memoryId);

    AiAutoMemory rename(AiAutoMemoryOwner owner, String oldName, String newName);

    List<AiAutoMemory> list(AiAutoMemoryOwner owner, @Nullable AiAutoMemoryType memoryType);

    List<AiAutoMemory> listAllOwners(long workspaceId, Environment environment, @Nullable AiAutoMemoryType memoryType);

    Optional<AiAutoMemory> findById(AiAutoMemoryOwner owner, long memoryId);

    List<AiAutoMemoryPrincipalCount> listPrincipals(long workspaceId, Environment environment);

    default boolean isAvailable() {
        return true;
    }
}

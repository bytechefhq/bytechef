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

package com.bytechef.platform.ai.auto.memory.repository;

import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalCount;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface AiAutoMemoryRepository {

    AiAutoMemory save(AiAutoMemory memory);

    void delete(AiAutoMemory memory);

    Optional<AiAutoMemory> findOwnedById(AiAutoMemoryOwner owner, long id);

    List<AiAutoMemory> findByOwner(AiAutoMemoryOwner owner, @Nullable AiAutoMemoryType memoryType);

    List<AiAutoMemory> findAllByOwnerAndName(AiAutoMemoryOwner owner, String name);

    List<AiAutoMemory> findByWorkspace(
        long workspaceId, Environment environment, @Nullable AiAutoMemoryType memoryType);

    List<AiAutoMemoryPrincipalCount> listPrincipals(long workspaceId, Environment environment);
}

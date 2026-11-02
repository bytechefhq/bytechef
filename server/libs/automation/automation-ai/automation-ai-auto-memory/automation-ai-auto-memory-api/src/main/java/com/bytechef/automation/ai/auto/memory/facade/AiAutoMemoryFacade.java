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

package com.bytechef.automation.ai.auto.memory.facade;

import com.bytechef.automation.ai.auto.memory.domain.AiAutoMemoryPrincipal;
import com.bytechef.automation.ai.auto.memory.domain.AiAutoMemoryPrincipalKey;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPatch;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryType;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public interface AiAutoMemoryFacade {

    void deleteAiAutoMemory(
        long workspaceId, Environment environment, long id, @Nullable AiAutoMemoryPrincipalKey principalKey);

    Optional<AiAutoMemory> fetchAiAutoMemory(
        long workspaceId, Environment environment, long id, @Nullable AiAutoMemoryPrincipalKey principalKey);

    List<AiAutoMemory> getAiAutoMemories(
        long workspaceId, Environment environment, @Nullable AiAutoMemoryType memoryType,
        @Nullable AiAutoMemoryPrincipalKey principalKey);

    List<AiAutoMemoryPrincipal> getAiAutoMemoryPrincipals(long workspaceId, Environment environment);

    AiAutoMemory updateAiAutoMemory(
        long workspaceId, Environment environment, long id, long expectedVersion, AiAutoMemoryPatch patch,
        @Nullable AiAutoMemoryPrincipalKey principalKey);
}

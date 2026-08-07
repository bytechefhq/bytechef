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

package com.bytechef.platform.ai.auto.memory.repository.jdbc;

import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalCount;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;

/**
 * One row of the grouped principal query, carrying {@code principal_type} as the raw persisted INT ordinal.
 *
 * <p>
 * The ordinal is mapped to {@link AiAutoMemoryPrincipalType} here rather than by letting the projection declare the
 * enum directly: {@code AiAutoMemory} stores the discriminator as an int and converts it by hand for exactly this
 * reason — the column holds an ordinal, not a name — so the query projection follows the entity instead of leaning on
 * whatever ordinal-to-enum conversion the mapping layer happens to provide.
 * </p>
 *
 * @author Ivica Cardic
 */
public record AiAutoMemoryPrincipalCountRow(int principalType, long principalId, int memoryCount) {

    public AiAutoMemoryPrincipalCount toAiAutoMemoryPrincipalCount() {
        AiAutoMemoryPrincipalType[] principalTypes = AiAutoMemoryPrincipalType.values();

        if (principalType < 0 || principalType >= principalTypes.length) {
            throw new IllegalStateException("Unknown AiAutoMemoryPrincipalType ordinal: " + principalType);
        }

        return new AiAutoMemoryPrincipalCount(principalTypes[principalType], principalId, memoryCount);
    }
}

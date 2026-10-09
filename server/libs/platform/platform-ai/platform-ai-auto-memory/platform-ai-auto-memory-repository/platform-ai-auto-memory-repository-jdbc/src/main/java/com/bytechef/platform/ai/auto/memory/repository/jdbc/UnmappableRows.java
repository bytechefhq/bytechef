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

import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
final class UnmappableRows {

    private static final Logger log = LoggerFactory.getLogger(JdbcAiAutoMemoryRepository.class);

    private UnmappableRows() {
    }

    static List<AiAutoMemory> skipUnmappable(List<AiAutoMemory> memories) {
        return memories.stream()
            .filter(UnmappableRows::isMappable)
            .toList();
    }

    static Optional<AiAutoMemory> skipUnmappable(Optional<AiAutoMemory> memory) {
        return memory.filter(UnmappableRows::isMappable);
    }

    private static boolean isMappable(AiAutoMemory memory) {
        if (memory.hasKnownOrdinals()) {
            return true;
        }

        log.error("Skipping auto-memory row with an ordinal this build does not know: {}", memory);

        return false;
    }
}

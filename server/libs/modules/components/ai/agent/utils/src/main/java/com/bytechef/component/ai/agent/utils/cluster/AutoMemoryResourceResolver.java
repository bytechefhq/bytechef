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

package com.bytechef.component.ai.agent.utils.cluster;

import com.bytechef.platform.ai.agent.memory.MemoryResourceResolver;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import org.springframework.core.io.WritableResource;

/**
 * @author Ivica Cardic
 */
public final class AutoMemoryResourceResolver implements MemoryResourceResolver {

    private static final String INDEX_MEMORY_NAME = "memory";

    private final AiAutoMemoryService aiAutoMemoryService;
    private final AiAutoMemoryOwner owner;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AutoMemoryResourceResolver(AiAutoMemoryService aiAutoMemoryService, AiAutoMemoryOwner owner) {
        this.aiAutoMemoryService = Objects.requireNonNull(aiAutoMemoryService, "aiAutoMemoryService");
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    @Override
    public WritableResource resolve(String relativePath) {
        return new AutoMemoryResource(aiAutoMemoryService, owner, toMemoryName(relativePath));
    }

    static String toMemoryName(String path) {
        String memoryName = path == null ? ""
            : path.trim()
                .toLowerCase(Locale.ROOT);

        while (memoryName.startsWith("/")) {
            memoryName = memoryName.substring(1);
        }

        if (memoryName.endsWith(".md")) {
            memoryName = memoryName.substring(0, memoryName.length() - ".md".length());
        }

        return memoryName;
    }

    static void verifyMemoryName(String memoryName) throws IOException {
        if (!AiAutoMemory.NAME_PATTERN.matcher(memoryName)
            .matches()) {

            throw new IOException(
                "Invalid entry name '" + memoryName + "': use 1-64 lowercase letters, digits, '-' or '_', optionally "
                    + "ending in '.md'.");
        }

        if (memoryName.equals(INDEX_MEMORY_NAME)) {
            throw new IOException(
                "The entry name '" + memoryName + "' is reserved for the index (MEMORY.md); choose another name.");
        }
    }
}

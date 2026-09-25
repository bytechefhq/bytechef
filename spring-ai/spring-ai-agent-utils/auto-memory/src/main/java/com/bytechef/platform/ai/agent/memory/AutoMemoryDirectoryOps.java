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

package com.bytechef.platform.ai.agent.memory;

import org.springframework.ai.chat.model.ToolContext;

/**
 * Metadata seam for {@link AutoMemoryTools}: listing the memory index, existence checks, deletion, and renaming.
 * Replaces upstream's {@code java.nio.file.Files}-based directory operations so a non-filesystem backend (e.g. a
 * database) can serve them. Implementations derive the tenant from the supplied {@link ToolContext}.
 */
public interface AutoMemoryDirectoryOps {

    /**
     * Renders the memory index. {@code path} is the root ("", "/" or "MEMORY.md"); a human-readable index listing is
     * returned.
     */
    String list(String path, ToolContext toolContext);

    boolean exists(String relativePath, ToolContext toolContext);

    void delete(String relativePath, ToolContext toolContext);

    void rename(String oldRelativePath, String newRelativePath, ToolContext toolContext);
}

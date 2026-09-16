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

import java.io.IOException;

/**
 * Metadata seam for {@link AutoMemoryTools}: listing the memory index, existence checks, deletion, and renaming.
 * Replaces upstream's {@code java.nio.file.Files}-based directory operations so a non-filesystem backend (e.g. a
 * database) can serve them. Implementations are scoped to one memory owner at construction.
 *
 * <p>
 * Failure contract: an outcome the model can act on — a missing entry, a name already taken, an entry another writer
 * changed meanwhile — must be thrown as an {@link IOException} carrying a message for the model, and memory being
 * unavailable as an {@link AutoMemoryUnavailableException}. {@link AutoMemoryTools} returns those to the model; an
 * {@link IllegalArgumentException} is returned too but logged at WARN, since it may be a bug rather than bad input, and
 * anything else is logged and reported only as a failed operation.
 * </p>
 *
 * @author Ivica Cardic
 */
public interface AutoMemoryDirectoryOps {

    /**
     * Renders the memory index as a human-readable listing. {@code path} is whichever spelling of the root or the index
     * the caller used ({@code ""}, {@code "/"}, {@code "MEMORY.md"} in any case, with leading slashes); implementations
     * may ignore it.
     */
    String list(String path);

    /**
     * Whether an entry exists at {@code relativePath}.
     */
    boolean exists(String relativePath);

    /**
     * Deletes the entry at {@code relativePath}; a missing entry is an {@link IOException}.
     */
    void delete(String relativePath) throws IOException;

    /**
     * Renames an entry; a missing source or a taken target is an {@link IOException}.
     */
    void rename(String oldRelativePath, String newRelativePath) throws IOException;
}

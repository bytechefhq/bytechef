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
import org.springframework.core.io.WritableResource;

/**
 * Resolves a relative memory path to a tenant-scoped, writable {@link org.springframework.core.io.Resource}. This is
 * the read/write content seam for {@link AutoMemoryTools}: reads use {@code getInputStream()} and writes use
 * {@code getOutputStream()}. Implementations derive the tenant from the supplied {@link ToolContext}.
 */
@FunctionalInterface
public interface MemoryResourceResolver {

    WritableResource resolve(String relativePath, ToolContext toolContext);
}

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

import com.bytechef.platform.ai.agent.memory.AutoMemoryDirectoryOps;
import com.bytechef.platform.ai.agent.memory.AutoMemoryUnavailableException;
import com.bytechef.platform.ai.agent.memory.MemoryResourceResolver;
import org.springframework.core.io.WritableResource;

/**
 * Backs the memory tools when this run has no memory to use — an editor test run, an owner that cannot be resolved, or
 * an application that does not store memory. Every operation throws {@link AutoMemoryUnavailableException} carrying the
 * reason, which {@code AutoMemoryTools} returns to the model as an error it can relay, instead of the agent silently
 * having no memory tools at all.
 *
 * @author Ivica Cardic
 */
final class UnavailableAutoMemory implements AutoMemoryDirectoryOps, MemoryResourceResolver {

    private final String reason;

    UnavailableAutoMemory(String reason) {
        this.reason = reason;
    }

    @Override
    public String list(String path) {
        throw new AutoMemoryUnavailableException(reason);
    }

    @Override
    public boolean exists(String relativePath) {
        throw new AutoMemoryUnavailableException(reason);
    }

    @Override
    public void delete(String relativePath) {
        throw new AutoMemoryUnavailableException(reason);
    }

    @Override
    public void rename(String oldRelativePath, String newRelativePath) {
        throw new AutoMemoryUnavailableException(reason);
    }

    @Override
    public WritableResource resolve(String relativePath) {
        throw new AutoMemoryUnavailableException(reason);
    }
}

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
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.core.io.WritableResource;

/**
 * {@link MemoryResourceResolver} backed by {@link AiAutoMemoryService} with a fixed
 * {@code (workspaceId, principalType, principalId, environment)} tenant. The scoping is pinned per agent run at
 * construction time, so the {@link ToolContext} passed to {@link #resolve(String, ToolContext)} is ignored.
 *
 * @author Ivica Cardic
 */
public class AutoMemoryResourceResolver implements MemoryResourceResolver {

    private final AiAutoMemoryService aiAutoMemoryService;
    private final long workspaceId;
    private final AiAutoMemoryPrincipalType principalType;
    private final long principalId;
    private final int environment;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AutoMemoryResourceResolver(
        AiAutoMemoryService aiAutoMemoryService, long workspaceId, AiAutoMemoryPrincipalType principalType,
        long principalId, int environment) {

        this.aiAutoMemoryService = aiAutoMemoryService;
        this.workspaceId = workspaceId;
        this.principalType = principalType;
        this.principalId = principalId;
        this.environment = environment;
    }

    @Override
    public WritableResource resolve(String relativePath, ToolContext toolContext) {
        return new AutoMemoryResource(
            aiAutoMemoryService, workspaceId, principalType, principalId, environment, toMemoryName(relativePath));
    }

    /**
     * Derives the memory {@code name} slug from a tool-supplied path: strips an optional {@code .md} suffix and lower-
     * cases. The slug — not the path — is the authoritative key for
     * {@link com.bytechef.platform.ai.auto.memory.AiAutoMemory}.
     */
    private static String toMemoryName(String path) {
        String trimmed = path == null ? "" : path.trim();

        if (trimmed.endsWith(".md")) {
            trimmed = trimmed.substring(0, trimmed.length() - ".md".length());
        }

        return trimmed.toLowerCase();
    }
}

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
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.ai.chat.model.ToolContext;

/**
 * {@link AutoMemoryDirectoryOps} backed by {@link AiAutoMemoryService} with a fixed
 * {@code (workspaceId, principalType, principalId, environment)} tenant. The "index" (MEMORY.md) is synthesized from
 * {@link AiAutoMemoryService#listByPrincipalAndWorkspace} rather than stored — the DB is the source of truth, so there
 * is no standalone index file to maintain. The scoping is pinned per agent run at construction time, so the
 * {@link ToolContext} passed to each operation is ignored.
 *
 * @author Ivica Cardic
 */
public class DbBackedAutoMemoryDirectoryOps implements AutoMemoryDirectoryOps {

    private final AiAutoMemoryService aiAutoMemoryService;
    private final long workspaceId;
    private final AiAutoMemoryPrincipalType principalType;
    private final long principalId;
    private final int environment;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DbBackedAutoMemoryDirectoryOps(
        AiAutoMemoryService aiAutoMemoryService, long workspaceId, AiAutoMemoryPrincipalType principalType,
        long principalId, int environment) {

        this.aiAutoMemoryService = aiAutoMemoryService;
        this.workspaceId = workspaceId;
        this.principalType = principalType;
        this.principalId = principalId;
        this.environment = environment;
    }

    @Override
    public String list(String path, ToolContext toolContext) {
        List<AiAutoMemory> memories = aiAutoMemoryService.listByPrincipalAndWorkspace(
            workspaceId, principalType, principalId, environment);

        if (memories.isEmpty()) {
            return "MEMORY index is empty. Create entries with MemoryCreate.";
        }

        StringBuilder stringBuilder = new StringBuilder("MEMORY index (");

        stringBuilder.append(memories.size())
            .append(" entries):\n");

        for (AiAutoMemory memory : memories) {
            stringBuilder.append("- ")
                .append(memory.getName())
                .append(".md — [")
                .append(memory.getMemoryType()
                    .name())
                .append("] ")
                .append(memory.getTitle());

            String description = memory.getDescription();

            if (description != null && !description.isBlank()) {
                stringBuilder.append(" — ")
                    .append(description);
            }

            stringBuilder.append("\n");
        }

        return stringBuilder.toString();
    }

    @Override
    public boolean exists(String relativePath, ToolContext toolContext) {
        return aiAutoMemoryService.read(
            workspaceId, principalType, principalId, environment, toMemoryName(relativePath))
            .isPresent();
    }

    @Override
    public void delete(String relativePath, ToolContext toolContext) {
        aiAutoMemoryService.delete(
            workspaceId, principalType, principalId, environment, toMemoryName(relativePath));
    }

    @Override
    public void rename(String oldRelativePath, String newRelativePath, ToolContext toolContext) {
        aiAutoMemoryService.rename(
            workspaceId, principalType, principalId, environment,
            toMemoryName(oldRelativePath), toMemoryName(newRelativePath));
    }

    /**
     * Derives the memory {@code name} slug from a tool-supplied path: strips an optional {@code .md} suffix and lower-
     * cases. The slug — not the path — is the authoritative key for {@link AiAutoMemory}.
     */
    private static String toMemoryName(String path) {
        String trimmed = path == null ? "" : path.trim();

        if (trimmed.endsWith(".md")) {
            trimmed = trimmed.substring(0, trimmed.length() - ".md".length());
        }

        return trimmed.toLowerCase();
    }
}

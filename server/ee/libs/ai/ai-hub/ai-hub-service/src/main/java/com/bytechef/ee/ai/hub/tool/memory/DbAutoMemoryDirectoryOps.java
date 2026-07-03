/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool.memory;

import com.bytechef.ee.ai.hub.tool.AiHubToolInvocationContext;
import com.bytechef.platform.ai.agent.memory.AutoMemoryDirectoryOps;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.ai.chat.model.ToolContext;

/**
 * {@link AutoMemoryDirectoryOps} backed by {@link AiAutoMemoryService}. The "index" (MEMORY.md) is synthesized from
 * {@link AiAutoMemoryService#listByPrincipalAndWorkspace} rather than stored — the DB is the source of truth, so there
 * is no standalone index file to maintain.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class DbAutoMemoryDirectoryOps implements AutoMemoryDirectoryOps {

    private final AiAutoMemoryService aiAutoMemoryService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DbAutoMemoryDirectoryOps(AiAutoMemoryService aiAutoMemoryService) {
        this.aiAutoMemoryService = aiAutoMemoryService;
    }

    @Override
    public String list(String path, ToolContext toolContext) {
        AiHubToolInvocationContext context = resolve(toolContext);

        List<AiAutoMemory> memories = aiAutoMemoryService.listByPrincipalAndWorkspace(
            context.workspaceId(), AiAutoMemoryPrincipalType.USER, context.userId(),
            AiHubToolInvocationContext.resolveEnvironmentOrDefault(context));

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
        AiHubToolInvocationContext context = resolve(toolContext);

        return aiAutoMemoryService.read(
            context.workspaceId(), AiAutoMemoryPrincipalType.USER, context.userId(),
            AiHubToolInvocationContext.resolveEnvironmentOrDefault(context),
            AutoMemoryToolSupport.toMemoryName(relativePath))
            .isPresent();
    }

    @Override
    public void delete(String relativePath, ToolContext toolContext) {
        AiHubToolInvocationContext context = resolve(toolContext);

        aiAutoMemoryService.delete(
            context.workspaceId(), AiAutoMemoryPrincipalType.USER, context.userId(),
            AiHubToolInvocationContext.resolveEnvironmentOrDefault(context),
            AutoMemoryToolSupport.toMemoryName(relativePath));
    }

    @Override
    public void rename(String oldRelativePath, String newRelativePath, ToolContext toolContext) {
        AiHubToolInvocationContext context = resolve(toolContext);

        aiAutoMemoryService.rename(
            context.workspaceId(), AiAutoMemoryPrincipalType.USER, context.userId(),
            AiHubToolInvocationContext.resolveEnvironmentOrDefault(context),
            AutoMemoryToolSupport.toMemoryName(oldRelativePath), AutoMemoryToolSupport.toMemoryName(newRelativePath));
    }

    private static AiHubToolInvocationContext resolve(ToolContext toolContext) {
        AiHubToolInvocationContext context = AutoMemoryToolSupport.resolveContext(toolContext);

        String contextError = AutoMemoryToolSupport.contextError(context);

        if (contextError != null) {
            throw new IllegalStateException(contextError);
        }

        return context;
    }
}

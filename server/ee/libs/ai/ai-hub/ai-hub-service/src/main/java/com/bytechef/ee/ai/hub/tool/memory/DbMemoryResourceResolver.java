/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.tool.memory;

import com.bytechef.ee.ai.hub.tool.AiHubToolInvocationContext;
import com.bytechef.platform.ai.agent.memory.MemoryResourceResolver;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.core.io.WritableResource;

/**
 * {@link MemoryResourceResolver} backed by {@link AiAutoMemoryService}. Resolves the requesting tenant
 * {@code (workspaceId, userId, environment)} from the Spring AI {@link ToolContext} and returns a
 * {@link DbMemoryResource} bound to the named entry.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class DbMemoryResourceResolver implements MemoryResourceResolver {

    private final AiAutoMemoryService aiAutoMemoryService;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DbMemoryResourceResolver(AiAutoMemoryService aiAutoMemoryService) {
        this.aiAutoMemoryService = aiAutoMemoryService;
    }

    @Override
    public WritableResource resolve(String relativePath, ToolContext toolContext) {
        AiHubToolInvocationContext context = AutoMemoryToolSupport.resolveContext(toolContext);

        String contextError = AutoMemoryToolSupport.contextError(context);

        if (contextError != null) {
            throw new IllegalStateException(contextError);
        }

        return new DbMemoryResource(
            aiAutoMemoryService, context.workspaceId(), context.userId(),
            AiHubToolInvocationContext.resolveEnvironmentOrDefault(context),
            AutoMemoryToolSupport.toMemoryName(relativePath));
    }
}

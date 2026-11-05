/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.security;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.repository.McpServerRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class EmbeddedMcpServerOwnershipResolver implements ResourceOwnershipResolver {

    private final McpServerRepository mcpServerRepository;

    @SuppressFBWarnings("EI")
    public EmbeddedMcpServerOwnershipResolver(McpServerRepository mcpServerRepository) {
        this.mcpServerRepository = mcpServerRepository;
    }

    @Override
    public String resourceType() {
        return "McpServer";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return mcpServerRepository.findById(id)
            .filter(mcpServer -> mcpServer.getType() == PlatformType.EMBEDDED)
            .map(mcpServer -> ResourceOwner.ofEmbedded())
            .orElseGet(ResourceOwner::unknown);
    }

    @Override
    public ResourceOwner resolveOwner(Serializable id) {
        return id instanceof Number number ? resolveOwner(number.longValue()) : ResourceOwner.unknown();
    }
}

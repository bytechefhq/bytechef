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
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.repository.McpComponentRepository;
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
public class EmbeddedMcpComponentOwnershipResolver implements ResourceOwnershipResolver {

    private final McpComponentRepository mcpComponentRepository;
    private final McpServerRepository mcpServerRepository;

    @SuppressFBWarnings("EI")
    public EmbeddedMcpComponentOwnershipResolver(
        McpComponentRepository mcpComponentRepository, McpServerRepository mcpServerRepository) {

        this.mcpComponentRepository = mcpComponentRepository;
        this.mcpServerRepository = mcpServerRepository;
    }

    @Override
    public String resourceType() {
        return "McpComponent";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return mcpComponentRepository.findById(id)
            .map(McpComponent::getMcpServerId)
            .flatMap(mcpServerRepository::findById)
            .filter(mcpServer -> mcpServer.getType() == PlatformType.EMBEDDED)
            .map(mcpServer -> ResourceOwner.ofEmbedded())
            .orElseGet(ResourceOwner::unknown);
    }

    @Override
    public ResourceOwner resolveOwner(Serializable id) {
        return id instanceof Number number ? resolveOwner(number.longValue()) : ResourceOwner.unknown();
    }
}

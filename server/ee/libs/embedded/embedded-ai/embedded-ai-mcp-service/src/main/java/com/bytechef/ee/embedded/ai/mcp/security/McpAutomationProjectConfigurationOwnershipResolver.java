/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.security;

import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.ee.embedded.ai.mcp.repository.McpAutomationProjectConfigurationRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
public class McpAutomationProjectConfigurationOwnershipResolver implements ResourceOwnershipResolver {

    private final McpAutomationProjectConfigurationRepository mcpAutomationProjectConfigurationRepository;

    @SuppressFBWarnings("EI")
    public McpAutomationProjectConfigurationOwnershipResolver(
        McpAutomationProjectConfigurationRepository mcpAutomationProjectConfigurationRepository) {

        this.mcpAutomationProjectConfigurationRepository = mcpAutomationProjectConfigurationRepository;
    }

    @Override
    public String resourceType() {
        return "McpAutomationProjectConfiguration";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return mcpAutomationProjectConfigurationRepository.existsById(id) ? ResourceOwner.ofEmbedded()
            : ResourceOwner.unknown();
    }

    @Override
    public ResourceOwner resolveOwner(Serializable id) {
        return id instanceof Number number ? resolveOwner(number.longValue()) : ResourceOwner.unknown();
    }
}

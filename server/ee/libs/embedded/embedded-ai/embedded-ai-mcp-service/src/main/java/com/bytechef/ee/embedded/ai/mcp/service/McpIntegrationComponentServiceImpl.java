/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.service;

import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationComponent;
import com.bytechef.ee.embedded.ai.mcp.repository.McpIntegrationComponentRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class McpIntegrationComponentServiceImpl implements McpIntegrationComponentService {

    private final McpIntegrationComponentRepository mcpIntegrationComponentRepository;

    @SuppressFBWarnings("EI")
    public McpIntegrationComponentServiceImpl(McpIntegrationComponentRepository mcpIntegrationComponentRepository) {
        this.mcpIntegrationComponentRepository = mcpIntegrationComponentRepository;
    }

    @Override
    public McpIntegrationComponent create(long mcpComponentId, long integrationId) {
        return mcpIntegrationComponentRepository.save(new McpIntegrationComponent(mcpComponentId, integrationId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<McpIntegrationComponent> fetchMcpIntegrationComponent(long mcpComponentId) {
        return mcpIntegrationComponentRepository.findByMcpComponentId(mcpComponentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<McpIntegrationComponent> getIntegrationMcpIntegrationComponents(long integrationId) {
        return mcpIntegrationComponentRepository.findAllByIntegrationId(integrationId);
    }
}

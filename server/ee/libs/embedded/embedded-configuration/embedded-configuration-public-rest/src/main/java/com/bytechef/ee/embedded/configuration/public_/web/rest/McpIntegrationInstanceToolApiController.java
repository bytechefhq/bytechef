/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.mcp.facade.McpIntegrationInstanceToolFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.embedded.configuration.public_.web.rest.McpIntegrationInstanceToolApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
class McpIntegrationInstanceToolApiController {

    private final McpIntegrationInstanceToolFacade mcpIntegrationInstanceToolFacade;

    @SuppressFBWarnings("EI")
    McpIntegrationInstanceToolApiController(
        McpIntegrationInstanceToolFacade mcpIntegrationInstanceToolFacade) {

        this.mcpIntegrationInstanceToolFacade = mcpIntegrationInstanceToolFacade;
    }

    @CrossOrigin
    @PostMapping("/integration-instances/{id}/mcp-tools/{mcpToolId}/enable")
    public ResponseEntity<Void> enableFrontendMcpIntegrationInstanceTool(
        @PathVariable Long id, @PathVariable Long mcpToolId) {

        mcpIntegrationInstanceToolFacade.enableMcpIntegrationInstanceTool(id, mcpToolId, true);

        return ResponseEntity.noContent()
            .build();
    }

    @CrossOrigin
    @DeleteMapping("/integration-instances/{id}/mcp-tools/{mcpToolId}/enable")
    public ResponseEntity<Void> disableFrontendMcpIntegrationInstanceTool(
        @PathVariable Long id, @PathVariable Long mcpToolId) {

        mcpIntegrationInstanceToolFacade.enableMcpIntegrationInstanceTool(id, mcpToolId, false);

        return ResponseEntity.noContent()
            .build();
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    @PostMapping("/external/{externalUserId}/integration-instances/{id}/mcp-tools/{mcpToolId}/enable")
    public ResponseEntity<Void> enableMcpIntegrationInstanceTool(
        @PathVariable String externalUserId, @PathVariable Long id, @PathVariable Long mcpToolId) {

        mcpIntegrationInstanceToolFacade.enableMcpIntegrationInstanceTool(id, mcpToolId, true);

        return ResponseEntity.noContent()
            .build();
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    @DeleteMapping("/external/{externalUserId}/integration-instances/{id}/mcp-tools/{mcpToolId}/enable")
    public ResponseEntity<Void> disableMcpIntegrationInstanceTool(
        @PathVariable String externalUserId, @PathVariable Long id, @PathVariable Long mcpToolId) {

        mcpIntegrationInstanceToolFacade.enableMcpIntegrationInstanceTool(id, mcpToolId, false);

        return ResponseEntity.noContent()
            .build();
    }
}

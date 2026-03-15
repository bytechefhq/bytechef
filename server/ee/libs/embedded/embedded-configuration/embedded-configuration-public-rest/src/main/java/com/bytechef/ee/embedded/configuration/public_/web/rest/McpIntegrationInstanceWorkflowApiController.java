/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.UpdateFrontendIntegrationInstanceWorkflowRequestModel;
import com.bytechef.ee.embedded.mcp.facade.McpIntegrationInstanceWorkflowFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@RestController("com.bytechef.ee.embedded.configuration.public_.web.rest.McpIntegrationInstanceWorkflowApiController")
@RequestMapping("${openapi.openAPIDefinition.base-path.embedded:}/v1")
@ConditionalOnCoordinator
@ConditionalOnEEVersion
class McpIntegrationInstanceWorkflowApiController {

    private final McpIntegrationInstanceWorkflowFacade mcpIntegrationInstanceWorkflowFacade;

    @SuppressFBWarnings("EI")
    McpIntegrationInstanceWorkflowApiController(
        McpIntegrationInstanceWorkflowFacade mcpIntegrationInstanceWorkflowFacade) {

        this.mcpIntegrationInstanceWorkflowFacade = mcpIntegrationInstanceWorkflowFacade;
    }

    @CrossOrigin
    @PutMapping("/integration-instances/{id}/mcp-workflows/{workflowUuid}")
    public ResponseEntity<Void> updateFrontendMcpIntegrationInstanceWorkflow(
        @PathVariable Long id, @PathVariable String workflowUuid,
        @RequestBody UpdateFrontendIntegrationInstanceWorkflowRequestModel updateFrontendIntegrationInstanceWorkflowRequestModel) {

        mcpIntegrationInstanceWorkflowFacade.updateMcpIntegrationInstanceWorkflow(
            id, workflowUuid, updateFrontendIntegrationInstanceWorkflowRequestModel.getInputs());

        return ResponseEntity.noContent()
            .build();
    }

    @CrossOrigin
    @PostMapping("/integration-instances/{id}/mcp-workflows/{workflowUuid}/enable")
    public ResponseEntity<Void> enableFrontendMcpIntegrationInstanceWorkflow(
        @PathVariable Long id, @PathVariable String workflowUuid) {

        mcpIntegrationInstanceWorkflowFacade.enableMcpIntegrationInstanceWorkflow(id, workflowUuid, true);

        return ResponseEntity.noContent()
            .build();
    }

    @CrossOrigin
    @DeleteMapping("/integration-instances/{id}/mcp-workflows/{workflowUuid}/enable")
    public ResponseEntity<Void> disableFrontendMcpIntegrationInstanceWorkflow(
        @PathVariable Long id, @PathVariable String workflowUuid) {

        mcpIntegrationInstanceWorkflowFacade.enableMcpIntegrationInstanceWorkflow(id, workflowUuid, false);

        return ResponseEntity.noContent()
            .build();
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    @PostMapping("/external/{externalUserId}/integration-instances/{id}/mcp-workflows/{workflowUuid}/enable")
    public ResponseEntity<Void> enableMcpIntegrationInstanceWorkflow(
        @PathVariable String externalUserId, @PathVariable Long id, @PathVariable String workflowUuid) {

        mcpIntegrationInstanceWorkflowFacade.enableMcpIntegrationInstanceWorkflow(id, workflowUuid, true);

        return ResponseEntity.noContent()
            .build();
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    @DeleteMapping("/external/{externalUserId}/integration-instances/{id}/mcp-workflows/{workflowUuid}/enable")
    public ResponseEntity<Void> disableMcpIntegrationInstanceWorkflow(
        @PathVariable String externalUserId, @PathVariable Long id, @PathVariable String workflowUuid) {

        mcpIntegrationInstanceWorkflowFacade.enableMcpIntegrationInstanceWorkflow(id, workflowUuid, false);

        return ResponseEntity.noContent()
            .build();
    }
}

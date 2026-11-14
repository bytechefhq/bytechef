/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import com.bytechef.ee.embedded.ai.mcp.audit.ConnectedUserMcpServerAuditEvents;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.ai.mcp.exception.McpAutomationWorkflowNotFoundException;
import com.bytechef.ee.embedded.ai.mcp.service.McpAutomationProjectConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpAutomationProjectConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.connected.user.security.ConnectedUserAuthentications;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
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
class McpAutomationWorkflowFacadeImpl implements McpAutomationWorkflowFacade {

    private final AutomationProjectConfigurationService automationProjectConfigurationService;
    private final ConnectedUserProjectService connectedUserProjectService;
    private final ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService;
    private final McpAutomationProjectConfigurationService mcpAutomationProjectConfigurationService;
    private final McpAutomationProjectConfigurationWorkflowService mcpAutomationProjectConfigurationWorkflowService;
    private final McpConnectedUserProjectWorkflowService mcpConnectedUserProjectWorkflowService;

    @SuppressFBWarnings("EI")
    McpAutomationWorkflowFacadeImpl(
        AutomationProjectConfigurationService automationProjectConfigurationService,
        ConnectedUserProjectService connectedUserProjectService,
        ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService,
        McpAutomationProjectConfigurationService mcpAutomationProjectConfigurationService,
        McpAutomationProjectConfigurationWorkflowService mcpAutomationProjectConfigurationWorkflowService,
        McpConnectedUserProjectWorkflowService mcpConnectedUserProjectWorkflowService) {

        this.automationProjectConfigurationService = automationProjectConfigurationService;
        this.connectedUserProjectService = connectedUserProjectService;
        this.connectedUserProjectWorkflowService = connectedUserProjectWorkflowService;
        this.mcpAutomationProjectConfigurationService = mcpAutomationProjectConfigurationService;
        this.mcpAutomationProjectConfigurationWorkflowService = mcpAutomationProjectConfigurationWorkflowService;
        this.mcpConnectedUserProjectWorkflowService = mcpConnectedUserProjectWorkflowService;
    }

    @Override
    @Audited(event = ConnectedUserMcpServerAuditEvents.CONNECTED_USER_MCP_AUTOMATION_WORKFLOW_ENABLED_UPDATED, data = {
        @AuditData(key = "connectedUserId", value = "#connectedUserId"),
        @AuditData(key = "workflowUuid", value = "#workflowUuid"),
        @AuditData(key = "enabled", value = "#enable")
    })
    @PreAuthorize("isTenantAdmin() or isConnectedUser()")
    public void enableMcpAutomationWorkflow(long connectedUserId, String workflowUuid, boolean enable) {
        if (ConnectedUserAuthentications.fetchCurrent()
            .isPresent() && !ConnectedUserAuthentications.isCurrentConnectedUser(connectedUserId)) {

            throw new McpAutomationWorkflowNotFoundException(workflowUuid);
        }

        Set<Long> referenceIds = new HashSet<>();

        for (ConnectedUserProjectWorkflow reference : getReferences(connectedUserId)) {
            if (Objects.equals(reference.getAutomationUuid(), workflowUuid)) {
                referenceIds.add(reference.getId());
            }
        }

        if (referenceIds.isEmpty()) {
            throw new McpAutomationWorkflowNotFoundException(workflowUuid);
        }

        for (McpConnectedUserProjectWorkflow row : mcpConnectedUserProjectWorkflowService
            .getConnectedUserMcpConnectedUserProjectWorkflows(connectedUserId)) {

            if (referenceIds.contains(row.getConnectedUserProjectWorkflowId()) && row.isEnabled() != enable) {
                mcpConnectedUserProjectWorkflowService.updateEnabled(row.getId(), enable);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isTenantAdmin() or isConnectedUser()")
    public Map<String, Boolean> getMcpEnabledByAutomationUuid(long connectedUserId) {
        Map<Long, Boolean> enabledByReferenceId = new HashMap<>();
        Map<Long, Boolean> offeredByLinkWorkflowId = new HashMap<>();

        for (McpConnectedUserProjectWorkflow row : mcpConnectedUserProjectWorkflowService
            .getConnectedUserMcpConnectedUserProjectWorkflows(connectedUserId)) {

            boolean offered = offeredByLinkWorkflowId.computeIfAbsent(
                row.getMcpAutomationProjectConfigurationWorkflowId(), this::isOffered);

            if (offered) {
                enabledByReferenceId.merge(
                    row.getConnectedUserProjectWorkflowId(), row.isEnabled(), Boolean::logicalOr);
            }
        }

        Map<String, Boolean> mcpEnabledByAutomationUuid = new HashMap<>();

        for (ConnectedUserProjectWorkflow reference : getReferences(connectedUserId)) {
            String automationUuid = reference.getAutomationUuid();

            if (automationUuid != null && enabledByReferenceId.containsKey(reference.getId())) {
                mcpEnabledByAutomationUuid.merge(
                    automationUuid, enabledByReferenceId.get(reference.getId()), Boolean::logicalOr);
            }
        }

        return mcpEnabledByAutomationUuid;
    }

    private boolean isOffered(long mcpAutomationProjectConfigurationWorkflowId) {
        Optional<McpAutomationProjectConfigurationWorkflow> linkWorkflowOptional =
            mcpAutomationProjectConfigurationWorkflowService.fetch(mcpAutomationProjectConfigurationWorkflowId);

        if (linkWorkflowOptional.isEmpty()) {
            return false;
        }

        McpAutomationProjectConfigurationWorkflow linkWorkflow = linkWorkflowOptional.get();

        if (!linkWorkflow.isEnabled()) {
            return false;
        }

        Optional<McpAutomationProjectConfiguration> linkOptional = mcpAutomationProjectConfigurationService.fetch(
            linkWorkflow.getMcpAutomationProjectConfigurationId());

        if (linkOptional.isEmpty()) {
            return false;
        }

        McpAutomationProjectConfiguration link = linkOptional.get();

        AutomationProjectConfiguration configuration = automationProjectConfigurationService.getConfiguration(
            link.getAutomationProjectConfigurationId());

        return configuration.isEnabled() && automationProjectConfigurationService.isWorkflowEnabled(
            configuration, UUID.fromString(linkWorkflow.getWorkflowUuid()));
    }

    private List<ConnectedUserProjectWorkflow> getReferences(long connectedUserId) {
        List<ConnectedUserProjectWorkflow> references = new ArrayList<>();

        for (ConnectedUserProject connectedUserProject : connectedUserProjectService
            .getConnectedUserConnectedUserProjects(connectedUserId)) {

            references.addAll(connectedUserProjectWorkflowService.getConnectedUserProjectWorkflows(
                connectedUserProject.getId()));
        }

        return references;
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceTool;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.ai.mcp.service.McpAutomationProjectConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpAutomationProjectConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpToolService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@Transactional(readOnly = true)
@ConditionalOnEEVersion
class McpServerMembership {

    private final IntegrationInstanceService integrationInstanceService;
    private final McpAutomationProjectConfigurationService mcpAutomationProjectConfigurationService;
    private final McpAutomationProjectConfigurationWorkflowService mcpAutomationProjectConfigurationWorkflowService;
    private final McpComponentService mcpComponentService;
    private final McpConnectedUserProjectWorkflowService mcpConnectedUserProjectWorkflowService;
    private final McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService;
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;
    private final McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;
    private final McpIntegrationInstanceWorkflowService mcpIntegrationInstanceWorkflowService;
    private final McpToolService mcpToolService;

    @SuppressWarnings("PMD.ExcessiveParameterList")
    McpServerMembership(
        IntegrationInstanceService integrationInstanceService,
        McpAutomationProjectConfigurationService mcpAutomationProjectConfigurationService,
        McpAutomationProjectConfigurationWorkflowService mcpAutomationProjectConfigurationWorkflowService,
        McpComponentService mcpComponentService,
        McpConnectedUserProjectWorkflowService mcpConnectedUserProjectWorkflowService,
        McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService,
        McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService,
        McpIntegrationInstanceToolService mcpIntegrationInstanceToolService,
        McpIntegrationInstanceWorkflowService mcpIntegrationInstanceWorkflowService,
        McpToolService mcpToolService) {

        this.integrationInstanceService = integrationInstanceService;
        this.mcpAutomationProjectConfigurationService = mcpAutomationProjectConfigurationService;
        this.mcpAutomationProjectConfigurationWorkflowService = mcpAutomationProjectConfigurationWorkflowService;
        this.mcpComponentService = mcpComponentService;
        this.mcpConnectedUserProjectWorkflowService = mcpConnectedUserProjectWorkflowService;
        this.mcpIntegrationInstanceConfigurationService = mcpIntegrationInstanceConfigurationService;
        this.mcpIntegrationInstanceConfigurationWorkflowService = mcpIntegrationInstanceConfigurationWorkflowService;
        this.mcpIntegrationInstanceToolService = mcpIntegrationInstanceToolService;
        this.mcpIntegrationInstanceWorkflowService = mcpIntegrationInstanceWorkflowService;
        this.mcpToolService = mcpToolService;
    }

    Set<Long> getConnectedUserIds(long mcpServerId) {
        Set<Long> connectedUserIds = new LinkedHashSet<>();

        Set<Long> integrationInstanceIds = new LinkedHashSet<>(
            mcpIntegrationInstanceToolService.getIntegrationInstanceIdsByMcpServerId(mcpServerId));

        integrationInstanceIds.addAll(
            mcpIntegrationInstanceWorkflowService.getIntegrationInstanceIdsByMcpServerId(mcpServerId));

        if (!integrationInstanceIds.isEmpty()) {
            for (IntegrationInstance integrationInstance : integrationInstanceService.getIntegrationInstances(
                new ArrayList<>(integrationInstanceIds))) {

                connectedUserIds.add(integrationInstance.getConnectedUserId());
            }
        }

        connectedUserIds.addAll(mcpConnectedUserProjectWorkflowService.getConnectedUserIdsByMcpServerId(mcpServerId));

        return connectedUserIds;
    }

    Optional<Boolean> getConnectedUserState(long connectedUserId, long mcpServerId) {
        Set<Long> mcpToolIds = new HashSet<>();

        for (McpComponent mcpComponent : mcpComponentService.getMcpServerMcpComponents(mcpServerId)) {
            for (McpTool mcpTool : mcpToolService
                .getMcpComponentMcpTools(Objects.requireNonNull(mcpComponent.getId()))) {

                mcpToolIds.add(mcpTool.getId());
            }
        }

        Set<Long> integrationLinkIds = new HashSet<>();

        for (McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration : mcpIntegrationInstanceConfigurationService
            .getMcpServerMcpIntegrationInstanceConfigurations(mcpServerId)) {

            for (McpIntegrationInstanceConfigurationWorkflow link : mcpIntegrationInstanceConfigurationWorkflowService
                .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
                    mcpIntegrationInstanceConfiguration.getId())) {

                integrationLinkIds.add(link.getId());
            }
        }

        Set<Long> automationLinkWorkflowIds = new HashSet<>();

        for (McpAutomationProjectConfiguration mcpAutomationProjectConfiguration : mcpAutomationProjectConfigurationService
            .getMcpServerMcpAutomationProjectConfigurations(mcpServerId)) {

            for (McpAutomationProjectConfigurationWorkflow linkWorkflow : mcpAutomationProjectConfigurationWorkflowService
                .getMcpAutomationProjectConfigurationWorkflows(mcpAutomationProjectConfiguration.getId())) {

                automationLinkWorkflowIds.add(linkWorkflow.getId());
            }
        }

        Boolean enabled = null;

        for (IntegrationInstance integrationInstance : integrationInstanceService
            .getConnectedUserIntegrationInstances(connectedUserId)) {

            long integrationInstanceId = Objects.requireNonNull(integrationInstance.getId());

            for (McpIntegrationInstanceTool mcpIntegrationInstanceTool : mcpIntegrationInstanceToolService
                .getMcpIntegrationInstanceTools(integrationInstanceId)) {

                if (mcpToolIds.contains(mcpIntegrationInstanceTool.getMcpToolId())) {
                    enabled = merge(enabled, mcpIntegrationInstanceTool.isEnabled());
                }
            }

            for (McpIntegrationInstanceWorkflow mcpIntegrationInstanceWorkflow : mcpIntegrationInstanceWorkflowService
                .getMcpIntegrationInstanceWorkflows(integrationInstanceId)) {

                if (integrationLinkIds.contains(
                    mcpIntegrationInstanceWorkflow.getMcpIntegrationInstanceConfigurationWorkflowId())) {

                    enabled = merge(enabled, mcpIntegrationInstanceWorkflow.isEnabled());
                }
            }
        }

        for (McpConnectedUserProjectWorkflow mcpConnectedUserProjectWorkflow : mcpConnectedUserProjectWorkflowService
            .getConnectedUserMcpConnectedUserProjectWorkflows(connectedUserId)) {

            if (automationLinkWorkflowIds.contains(
                mcpConnectedUserProjectWorkflow.getMcpAutomationProjectConfigurationWorkflowId())) {

                enabled = merge(enabled, mcpConnectedUserProjectWorkflow.isEnabled());
            }
        }

        return Optional.ofNullable(enabled);
    }

    Set<Long> getMcpServerIds(long connectedUserId) {
        return new LinkedHashSet<>(getEnabledByMcpServerId(connectedUserId).keySet());
    }

    private Map<Long, Boolean> getEnabledByMcpServerId(long connectedUserId) {
        Map<Long, Boolean> enabledByMcpServerId = new HashMap<>();

        for (IntegrationInstance integrationInstance : integrationInstanceService
            .getConnectedUserIntegrationInstances(connectedUserId)) {

            long integrationInstanceId = Objects.requireNonNull(integrationInstance.getId());

            for (McpIntegrationInstanceTool mcpIntegrationInstanceTool : mcpIntegrationInstanceToolService
                .getMcpIntegrationInstanceTools(integrationInstanceId)) {

                Optional<Long> mcpServerId = fetchToolMcpServerId(mcpIntegrationInstanceTool.getMcpToolId());

                mcpServerId.ifPresent(
                    serverId -> enabledByMcpServerId.merge(
                        serverId, mcpIntegrationInstanceTool.isEnabled(), Boolean::logicalOr));
            }

            for (McpIntegrationInstanceWorkflow mcpIntegrationInstanceWorkflow : mcpIntegrationInstanceWorkflowService
                .getMcpIntegrationInstanceWorkflows(integrationInstanceId)) {

                Optional<Long> mcpServerId = fetchLinkMcpServerId(
                    mcpIntegrationInstanceWorkflow.getMcpIntegrationInstanceConfigurationWorkflowId());

                mcpServerId.ifPresent(
                    serverId -> enabledByMcpServerId.merge(
                        serverId, mcpIntegrationInstanceWorkflow.isEnabled(), Boolean::logicalOr));
            }
        }

        for (McpConnectedUserProjectWorkflow mcpConnectedUserProjectWorkflow : mcpConnectedUserProjectWorkflowService
            .getConnectedUserMcpConnectedUserProjectWorkflows(connectedUserId)) {

            Optional<Long> mcpServerId = fetchAutomationLinkWorkflowMcpServerId(
                mcpConnectedUserProjectWorkflow.getMcpAutomationProjectConfigurationWorkflowId());

            mcpServerId.ifPresent(
                serverId -> enabledByMcpServerId.merge(
                    serverId, mcpConnectedUserProjectWorkflow.isEnabled(), Boolean::logicalOr));
        }

        return enabledByMcpServerId;
    }

    private Optional<Long> fetchAutomationLinkWorkflowMcpServerId(long mcpAutomationProjectConfigurationWorkflowId) {
        Optional<McpAutomationProjectConfigurationWorkflow> linkWorkflowOptional =
            mcpAutomationProjectConfigurationWorkflowService.fetch(mcpAutomationProjectConfigurationWorkflowId);

        if (linkWorkflowOptional.isEmpty()) {
            return Optional.empty();
        }

        McpAutomationProjectConfigurationWorkflow linkWorkflow = linkWorkflowOptional.get();

        Optional<McpAutomationProjectConfiguration> linkOptional = mcpAutomationProjectConfigurationService.fetch(
            linkWorkflow.getMcpAutomationProjectConfigurationId());

        return linkOptional.map(McpAutomationProjectConfiguration::getMcpServerId);
    }

    private Optional<Long> fetchLinkMcpServerId(long mcpIntegrationInstanceConfigurationWorkflowId) {
        Optional<McpIntegrationInstanceConfigurationWorkflow> linkOptional =
            mcpIntegrationInstanceConfigurationWorkflowService.fetchMcpIntegrationInstanceConfigurationWorkflow(
                mcpIntegrationInstanceConfigurationWorkflowId);

        if (linkOptional.isEmpty()) {
            return Optional.empty();
        }

        McpIntegrationInstanceConfigurationWorkflow link = linkOptional.get();

        Optional<McpIntegrationInstanceConfiguration> configurationOptional =
            mcpIntegrationInstanceConfigurationService.fetchMcpIntegrationInstanceConfiguration(
                link.getMcpIntegrationInstanceConfigurationId());

        return configurationOptional.map(McpIntegrationInstanceConfiguration::getMcpServerId);
    }

    private Optional<Long> fetchToolMcpServerId(long mcpToolId) {
        Optional<McpTool> mcpToolOptional = mcpToolService.fetchMcpTool(mcpToolId);

        if (mcpToolOptional.isEmpty()) {
            return Optional.empty();
        }

        McpTool mcpTool = mcpToolOptional.get();

        McpComponent mcpComponent = mcpComponentService.getMcpComponent(mcpTool.getMcpComponentId());

        return Optional.of(mcpComponent.getMcpServerId());
    }

    private static Boolean merge(Boolean current, boolean enabled) {
        return current == null ? enabled : current || enabled;
    }
}

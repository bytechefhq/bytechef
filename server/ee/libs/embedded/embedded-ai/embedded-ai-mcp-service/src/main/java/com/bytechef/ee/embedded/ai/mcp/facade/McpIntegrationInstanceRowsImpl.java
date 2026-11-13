/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceTool;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpToolService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
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
@Transactional
@ConditionalOnEEVersion
public class McpIntegrationInstanceRowsImpl implements McpIntegrationInstanceRows {

    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;
    private final IntegrationService integrationService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final McpComponentService mcpComponentService;
    private final McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService;
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;
    private final McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;
    private final McpIntegrationInstanceWorkflowService mcpIntegrationInstanceWorkflowService;
    private final McpToolService mcpToolService;

    @SuppressFBWarnings("EI")
    public McpIntegrationInstanceRowsImpl(
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceService integrationInstanceService,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService, IntegrationService integrationService,
        IntegrationWorkflowService integrationWorkflowService, McpComponentService mcpComponentService,
        McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService,
        McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService,
        McpIntegrationInstanceToolService mcpIntegrationInstanceToolService,
        McpIntegrationInstanceWorkflowService mcpIntegrationInstanceWorkflowService, McpToolService mcpToolService) {

        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
        this.integrationService = integrationService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.mcpComponentService = mcpComponentService;
        this.mcpIntegrationInstanceConfigurationService = mcpIntegrationInstanceConfigurationService;
        this.mcpIntegrationInstanceConfigurationWorkflowService = mcpIntegrationInstanceConfigurationWorkflowService;
        this.mcpIntegrationInstanceToolService = mcpIntegrationInstanceToolService;
        this.mcpIntegrationInstanceWorkflowService = mcpIntegrationInstanceWorkflowService;
        this.mcpToolService = mcpToolService;
    }

    @Override
    public void addMissingRows(long connectedUserId, long mcpServerId) {
        addMissingRows(connectedUserId, getMcpServerOffer(mcpServerId));
    }

    @Override
    public void addMissingRowsForConnectedUsers(long mcpServerId) {
        Set<Long> connectedUserIds = getConnectedUserIds(mcpServerId);

        if (connectedUserIds.isEmpty()) {
            return;
        }

        McpServerOffer mcpServerOffer = getMcpServerOffer(mcpServerId);

        for (long connectedUserId : connectedUserIds) {
            addMissingRows(connectedUserId, mcpServerOffer);
        }
    }

    @Override
    public void addMissingRowsForIntegrationInstance(long integrationInstanceId) {
        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            integrationInstanceId);

        long connectedUserId = integrationInstance.getConnectedUserId();

        for (long mcpServerId : getMcpServerIds(connectedUserId)) {
            addMissingRows(connectedUserId, mcpServerId);
        }
    }

    @Override
    public void addMissingRowsForIntegrationInstanceConfiguration(long integrationInstanceConfigurationId) {
        Set<Long> mcpServerIds = new LinkedHashSet<>();

        for (McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration : mcpIntegrationInstanceConfigurationService
            .getMcpIntegrationInstanceConfigurationsByIntegrationInstanceConfigurationId(
                integrationInstanceConfigurationId)) {

            mcpServerIds.add(mcpIntegrationInstanceConfiguration.getMcpServerId());
        }

        for (long mcpServerId : mcpServerIds) {
            addMissingRowsForConnectedUsers(mcpServerId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> getConnectedUserIds(long mcpServerId) {
        Set<Long> integrationInstanceIds = new LinkedHashSet<>(
            mcpIntegrationInstanceToolService.getIntegrationInstanceIdsByMcpServerId(mcpServerId));

        integrationInstanceIds.addAll(
            mcpIntegrationInstanceWorkflowService.getIntegrationInstanceIdsByMcpServerId(mcpServerId));

        Set<Long> connectedUserIds = new LinkedHashSet<>();

        if (integrationInstanceIds.isEmpty()) {
            return connectedUserIds;
        }

        for (IntegrationInstance integrationInstance : integrationInstanceService.getIntegrationInstances(
            new ArrayList<>(integrationInstanceIds))) {

            connectedUserIds.add(integrationInstance.getConnectedUserId());
        }

        return connectedUserIds;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> getMcpServerIds(long connectedUserId) {
        Set<Long> mcpServerIds = new LinkedHashSet<>();

        for (IntegrationInstance integrationInstance : integrationInstanceService
            .getConnectedUserIntegrationInstances(connectedUserId)) {

            long integrationInstanceId = Objects.requireNonNull(integrationInstance.getId());

            for (McpIntegrationInstanceTool mcpIntegrationInstanceTool : mcpIntegrationInstanceToolService
                .getMcpIntegrationInstanceTools(integrationInstanceId)) {

                fetchToolMcpServerId(mcpIntegrationInstanceTool.getMcpToolId()).ifPresent(mcpServerIds::add);
            }

            for (McpIntegrationInstanceWorkflow mcpIntegrationInstanceWorkflow : mcpIntegrationInstanceWorkflowService
                .getMcpIntegrationInstanceWorkflows(integrationInstanceId)) {

                fetchLinkMcpServerId(mcpIntegrationInstanceWorkflow.getMcpIntegrationInstanceConfigurationWorkflowId())
                    .ifPresent(mcpServerIds::add);
            }
        }

        return mcpServerIds;
    }

    @Override
    public void provisionMcpServer(long connectedUserId, long mcpServerId) {
        McpServerOffer mcpServerOffer = getMcpServerOffer(mcpServerId);

        for (IntegrationInstance integrationInstance : integrationInstanceService
            .getConnectedUserIntegrationInstances(connectedUserId)) {

            long integrationInstanceId = Objects.requireNonNull(integrationInstance.getId());

            for (long mcpToolId : mcpServerOffer.getOfferedMcpToolIds(integrationInstance)) {
                Optional<McpIntegrationInstanceTool> mcpIntegrationInstanceToolOptional =
                    mcpIntegrationInstanceToolService.fetchMcpIntegrationInstanceTool(integrationInstanceId, mcpToolId);

                if (mcpIntegrationInstanceToolOptional.isPresent()) {
                    McpIntegrationInstanceTool mcpIntegrationInstanceTool = mcpIntegrationInstanceToolOptional.get();

                    mcpIntegrationInstanceToolService.updateEnabled(mcpIntegrationInstanceTool.getId(), true);
                } else {
                    mcpIntegrationInstanceToolService.createMcpIntegrationInstanceTool(
                        integrationInstanceId, mcpToolId, true);
                }
            }

            for (McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow : mcpServerOffer
                .getOfferedLinks(integrationInstance)) {

                long mcpIntegrationInstanceConfigurationWorkflowId =
                    mcpIntegrationInstanceConfigurationWorkflow.getId();

                Optional<McpIntegrationInstanceWorkflow> mcpIntegrationInstanceWorkflowOptional =
                    mcpIntegrationInstanceWorkflowService.fetchMcpIntegrationInstanceWorkflow(
                        integrationInstanceId, mcpIntegrationInstanceConfigurationWorkflowId);

                if (mcpIntegrationInstanceWorkflowOptional.isPresent()) {
                    McpIntegrationInstanceWorkflow mcpIntegrationInstanceWorkflow =
                        mcpIntegrationInstanceWorkflowOptional.get();

                    mcpIntegrationInstanceWorkflowService.updateEnabled(mcpIntegrationInstanceWorkflow.getId(), true);
                } else {
                    mcpIntegrationInstanceWorkflowService.createMcpIntegrationInstanceWorkflow(
                        integrationInstanceId, mcpIntegrationInstanceConfigurationWorkflowId, true);
                }

                createMissingIntegrationInstanceWorkflow(
                    integrationInstanceId,
                    mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId());
            }
        }
    }

    @Override
    public void remapIntegrationInstance(long integrationInstanceId) {
        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            integrationInstanceId);

        long targetIntegrationInstanceConfigurationId = integrationInstance.getIntegrationInstanceConfigurationId();

        for (McpIntegrationInstanceWorkflow mcpIntegrationInstanceWorkflow : mcpIntegrationInstanceWorkflowService
            .getMcpIntegrationInstanceWorkflows(integrationInstanceId)) {

            McpIntegrationInstanceConfigurationWorkflow sourceLink = mcpIntegrationInstanceConfigurationWorkflowService
                .fetchMcpIntegrationInstanceConfigurationWorkflow(
                    mcpIntegrationInstanceWorkflow.getMcpIntegrationInstanceConfigurationWorkflowId())
                .orElse(null);

            if (sourceLink == null) {
                continue;
            }

            McpIntegrationInstanceConfiguration sourceMcpIntegrationInstanceConfiguration =
                mcpIntegrationInstanceConfigurationService
                    .fetchMcpIntegrationInstanceConfiguration(sourceLink.getMcpIntegrationInstanceConfigurationId())
                    .orElse(null);

            if (sourceMcpIntegrationInstanceConfiguration == null || Objects.equals(
                sourceMcpIntegrationInstanceConfiguration.getIntegrationInstanceConfigurationId(),
                targetIntegrationInstanceConfigurationId)) {

                continue;
            }

            String workflowUuid = getWorkflowUuid(sourceLink.getIntegrationInstanceConfigurationWorkflowId());

            mcpIntegrationInstanceWorkflowService.delete(mcpIntegrationInstanceWorkflow.getId());

            createTargetRow(
                integrationInstanceId, targetIntegrationInstanceConfigurationId,
                sourceMcpIntegrationInstanceConfiguration.getMcpServerId(), workflowUuid,
                mcpIntegrationInstanceWorkflow.isEnabled());
        }

        addMissingRowsForIntegrationInstance(integrationInstanceId);
    }

    private void addMissingRows(long connectedUserId, McpServerOffer mcpServerOffer) {
        List<IntegrationInstance> integrationInstances = integrationInstanceService
            .getConnectedUserIntegrationInstances(connectedUserId);

        Map<Long, List<McpIntegrationInstanceTool>> mcpIntegrationInstanceToolsByIntegrationInstanceId =
            new HashMap<>();
        Map<Long, List<McpIntegrationInstanceWorkflow>> mcpIntegrationInstanceWorkflowsByIntegrationInstanceId =
            new HashMap<>();

        for (IntegrationInstance integrationInstance : integrationInstances) {
            long integrationInstanceId = Objects.requireNonNull(integrationInstance.getId());

            mcpIntegrationInstanceToolsByIntegrationInstanceId.put(
                integrationInstanceId, mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(
                    integrationInstanceId));
            mcpIntegrationInstanceWorkflowsByIntegrationInstanceId.put(
                integrationInstanceId, mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(
                    integrationInstanceId));
        }

        boolean enabled = hasEnabledRow(
            mcpIntegrationInstanceToolsByIntegrationInstanceId, mcpIntegrationInstanceWorkflowsByIntegrationInstanceId,
            mcpServerOffer);

        for (IntegrationInstance integrationInstance : integrationInstances) {
            long integrationInstanceId = Objects.requireNonNull(integrationInstance.getId());

            Set<Long> existingMcpToolIds = new HashSet<>();

            for (McpIntegrationInstanceTool mcpIntegrationInstanceTool : mcpIntegrationInstanceToolsByIntegrationInstanceId
                .get(integrationInstanceId)) {

                existingMcpToolIds.add(mcpIntegrationInstanceTool.getMcpToolId());
            }

            for (long mcpToolId : mcpServerOffer.getOfferedMcpToolIds(integrationInstance)) {
                if (!existingMcpToolIds.contains(mcpToolId)) {
                    mcpIntegrationInstanceToolService.createMcpIntegrationInstanceTool(
                        integrationInstanceId, mcpToolId, enabled);
                }
            }

            Set<Long> existingLinkIds = new HashSet<>();

            for (McpIntegrationInstanceWorkflow mcpIntegrationInstanceWorkflow : mcpIntegrationInstanceWorkflowsByIntegrationInstanceId
                .get(integrationInstanceId)) {

                existingLinkIds.add(mcpIntegrationInstanceWorkflow.getMcpIntegrationInstanceConfigurationWorkflowId());
            }

            for (McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow : mcpServerOffer
                .getOfferedLinks(integrationInstance)) {

                long mcpIntegrationInstanceConfigurationWorkflowId =
                    mcpIntegrationInstanceConfigurationWorkflow.getId();

                if (existingLinkIds.contains(mcpIntegrationInstanceConfigurationWorkflowId)) {
                    continue;
                }

                mcpIntegrationInstanceWorkflowService.createMcpIntegrationInstanceWorkflow(
                    integrationInstanceId, mcpIntegrationInstanceConfigurationWorkflowId, enabled);

                createMissingIntegrationInstanceWorkflow(
                    integrationInstanceId,
                    mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId());
            }
        }
    }

    private void createMissingIntegrationInstanceWorkflow(
        long integrationInstanceId, long integrationInstanceConfigurationWorkflowId) {

        if (integrationInstanceWorkflowService
            .fetchIntegrationInstanceWorkflow(integrationInstanceId, integrationInstanceConfigurationWorkflowId)
            .isEmpty()) {

            integrationInstanceWorkflowService.createIntegrationInstanceWorkflow(
                integrationInstanceId, integrationInstanceConfigurationWorkflowId);
        }
    }

    private void createTargetRow(
        long integrationInstanceId, long targetIntegrationInstanceConfigurationId, long mcpServerId,
        String workflowUuid, boolean enabled) {

        for (McpIntegrationInstanceConfigurationWorkflow targetLink : getLinks(
            targetIntegrationInstanceConfigurationId, mcpServerId)) {

            long integrationInstanceConfigurationWorkflowId =
                targetLink.getIntegrationInstanceConfigurationWorkflowId();

            if (!workflowUuid.equals(getWorkflowUuid(integrationInstanceConfigurationWorkflowId)) ||
                mcpIntegrationInstanceWorkflowService
                    .fetchMcpIntegrationInstanceWorkflow(integrationInstanceId, targetLink.getId())
                    .isPresent()) {

                continue;
            }

            mcpIntegrationInstanceWorkflowService.createMcpIntegrationInstanceWorkflow(
                integrationInstanceId, targetLink.getId(), enabled);

            createMissingIntegrationInstanceWorkflow(integrationInstanceId, integrationInstanceConfigurationWorkflowId);
        }
    }

    private Optional<Long> fetchLinkMcpServerId(long mcpIntegrationInstanceConfigurationWorkflowId) {
        return mcpIntegrationInstanceConfigurationWorkflowService
            .fetchMcpIntegrationInstanceConfigurationWorkflow(mcpIntegrationInstanceConfigurationWorkflowId)
            .flatMap(link -> mcpIntegrationInstanceConfigurationService.fetchMcpIntegrationInstanceConfiguration(
                link.getMcpIntegrationInstanceConfigurationId()))
            .map(McpIntegrationInstanceConfiguration::getMcpServerId);
    }

    private Optional<Long> fetchToolMcpServerId(long mcpToolId) {
        return mcpToolService.fetchMcpTool(mcpToolId)
            .map(mcpTool -> mcpComponentService.getMcpComponent(mcpTool.getMcpComponentId()))
            .map(McpComponent::getMcpServerId);
    }

    private String getComponentName(long integrationInstanceConfigurationId) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                integrationInstanceConfigurationId);

        Integration integration = integrationService.getIntegration(
            integrationInstanceConfiguration.getIntegrationId());

        return integration.getComponentName();
    }

    private List<McpIntegrationInstanceConfigurationWorkflow> getLinks(
        long integrationInstanceConfigurationId, long mcpServerId) {

        List<McpIntegrationInstanceConfigurationWorkflow> links = new ArrayList<>();

        for (McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration : mcpIntegrationInstanceConfigurationService
            .getMcpIntegrationInstanceConfigurationsByIntegrationInstanceConfigurationId(
                integrationInstanceConfigurationId)) {

            if (!Objects.equals(mcpIntegrationInstanceConfiguration.getMcpServerId(), mcpServerId)) {
                continue;
            }

            links.addAll(
                mcpIntegrationInstanceConfigurationWorkflowService
                    .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
                        mcpIntegrationInstanceConfiguration.getId()));
        }

        return links;
    }

    private McpServerOffer getMcpServerOffer(long mcpServerId) {
        Map<String, List<Long>> enabledMcpToolIdsByComponentName = new HashMap<>();
        Set<Long> mcpToolIds = new HashSet<>();

        for (McpComponent mcpComponent : mcpComponentService.getMcpServerMcpComponents(mcpServerId)) {
            List<Long> enabledMcpToolIds = enabledMcpToolIdsByComponentName.computeIfAbsent(
                mcpComponent.getComponentName(), componentName -> new ArrayList<>());

            for (McpTool mcpTool : mcpToolService
                .getMcpComponentMcpTools(Objects.requireNonNull(mcpComponent.getId()))) {

                long mcpToolId = Objects.requireNonNull(mcpTool.getId());

                mcpToolIds.add(mcpToolId);

                if (mcpTool.isEnabled()) {
                    enabledMcpToolIds.add(mcpToolId);
                }
            }
        }

        Map<Long, List<McpIntegrationInstanceConfigurationWorkflow>> offeredLinksByIntegrationInstanceConfigurationId =
            new HashMap<>();
        Set<Long> linkIds = new HashSet<>();

        for (McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration : mcpIntegrationInstanceConfigurationService
            .getMcpServerMcpIntegrationInstanceConfigurations(mcpServerId)) {

            List<McpIntegrationInstanceConfigurationWorkflow> offeredLinks =
                offeredLinksByIntegrationInstanceConfigurationId.computeIfAbsent(
                    mcpIntegrationInstanceConfiguration.getIntegrationInstanceConfigurationId(),
                    integrationInstanceConfigurationId -> new ArrayList<>());

            for (McpIntegrationInstanceConfigurationWorkflow link : mcpIntegrationInstanceConfigurationWorkflowService
                .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
                    mcpIntegrationInstanceConfiguration.getId())) {

                linkIds.add(link.getId());

                if (!link.isEnabled()) {
                    continue;
                }

                IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                    integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                        link.getIntegrationInstanceConfigurationWorkflowId());

                if (integrationInstanceConfigurationWorkflow.isEnabled()) {
                    offeredLinks.add(link);
                }
            }
        }

        return new McpServerOffer(
            enabledMcpToolIdsByComponentName, mcpToolIds, offeredLinksByIntegrationInstanceConfigurationId, linkIds);
    }

    private String getWorkflowUuid(long integrationInstanceConfigurationWorkflowId) {
        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                integrationInstanceConfigurationWorkflowId);

        IntegrationWorkflow integrationWorkflow = integrationWorkflowService.getWorkflowIntegrationWorkflow(
            integrationInstanceConfigurationWorkflow.getWorkflowId());

        return integrationWorkflow.getUuidAsString();
    }

    private boolean hasEnabledRow(
        Map<Long, List<McpIntegrationInstanceTool>> mcpIntegrationInstanceToolsByIntegrationInstanceId,
        Map<Long, List<McpIntegrationInstanceWorkflow>> mcpIntegrationInstanceWorkflowsByIntegrationInstanceId,
        McpServerOffer mcpServerOffer) {

        for (List<McpIntegrationInstanceTool> mcpIntegrationInstanceTools : mcpIntegrationInstanceToolsByIntegrationInstanceId
            .values()) {

            for (McpIntegrationInstanceTool mcpIntegrationInstanceTool : mcpIntegrationInstanceTools) {
                if (mcpIntegrationInstanceTool.isEnabled() &&
                    mcpServerOffer.mcpToolIds.contains(mcpIntegrationInstanceTool.getMcpToolId())) {

                    return true;
                }
            }
        }

        for (List<McpIntegrationInstanceWorkflow> mcpIntegrationInstanceWorkflows : mcpIntegrationInstanceWorkflowsByIntegrationInstanceId
            .values()) {

            for (McpIntegrationInstanceWorkflow mcpIntegrationInstanceWorkflow : mcpIntegrationInstanceWorkflows) {
                if (mcpIntegrationInstanceWorkflow.isEnabled() &&
                    mcpServerOffer.linkIds.contains(
                        mcpIntegrationInstanceWorkflow.getMcpIntegrationInstanceConfigurationWorkflowId())) {

                    return true;
                }
            }
        }

        return false;
    }

    private final class McpServerOffer {

        private final Map<Long, String> componentNameByIntegrationInstanceConfigurationId = new HashMap<>();
        private final Map<String, List<Long>> enabledMcpToolIdsByComponentName;
        private final Set<Long> linkIds;
        private final Set<Long> mcpToolIds;
        private final Map<Long, List<McpIntegrationInstanceConfigurationWorkflow>> offeredLinksByIntegrationInstanceConfigurationId;

        private McpServerOffer(
            Map<String, List<Long>> enabledMcpToolIdsByComponentName, Set<Long> mcpToolIds,
            Map<Long, List<McpIntegrationInstanceConfigurationWorkflow>> offeredLinksByIntegrationInstanceConfigurationId,
            Set<Long> linkIds) {

            this.enabledMcpToolIdsByComponentName = enabledMcpToolIdsByComponentName;
            this.linkIds = linkIds;
            this.mcpToolIds = mcpToolIds;
            this.offeredLinksByIntegrationInstanceConfigurationId = offeredLinksByIntegrationInstanceConfigurationId;
        }

        private List<McpIntegrationInstanceConfigurationWorkflow> getOfferedLinks(
            IntegrationInstance integrationInstance) {

            return offeredLinksByIntegrationInstanceConfigurationId.getOrDefault(
                integrationInstance.getIntegrationInstanceConfigurationId(), List.of());
        }

        private List<Long> getOfferedMcpToolIds(IntegrationInstance integrationInstance) {
            String componentName = componentNameByIntegrationInstanceConfigurationId.computeIfAbsent(
                integrationInstance.getIntegrationInstanceConfigurationId(),
                McpIntegrationInstanceRowsImpl.this::getComponentName);

            return enabledMcpToolIdsByComponentName.getOrDefault(componentName, List.of());
        }
    }
}

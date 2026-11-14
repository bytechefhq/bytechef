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
import com.bytechef.ee.embedded.ai.mcp.service.McpAutomationProjectConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpAutomationProjectConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@Transactional
@ConditionalOnEEVersion
public class McpAutomationRowsImpl implements McpAutomationRows {

    private static final Logger log = LoggerFactory.getLogger(McpAutomationRowsImpl.class);

    private final ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService;
    private final ConnectedUserProjectService connectedUserProjectService;
    private final ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService;
    private final McpAutomationProjectConfigurationService mcpAutomationProjectConfigurationService;
    private final McpAutomationProjectConfigurationWorkflowService mcpAutomationProjectConfigurationWorkflowService;
    private final McpConnectedUserProjectWorkflowService mcpConnectedUserProjectWorkflowService;
    private final McpServerMembership mcpServerMembership;
    private final TransactionTemplate nestedTransactionTemplate;

    @SuppressFBWarnings("EI")
    public McpAutomationRowsImpl(
        ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService,
        ConnectedUserProjectService connectedUserProjectService,
        ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService,
        McpAutomationProjectConfigurationService mcpAutomationProjectConfigurationService,
        McpAutomationProjectConfigurationWorkflowService mcpAutomationProjectConfigurationWorkflowService,
        McpConnectedUserProjectWorkflowService mcpConnectedUserProjectWorkflowService,
        McpServerMembership mcpServerMembership, PlatformTransactionManager platformTransactionManager) {

        this.connectedUserProjectConfigurationService = connectedUserProjectConfigurationService;
        this.connectedUserProjectService = connectedUserProjectService;
        this.connectedUserProjectWorkflowService = connectedUserProjectWorkflowService;
        this.mcpAutomationProjectConfigurationService = mcpAutomationProjectConfigurationService;
        this.mcpAutomationProjectConfigurationWorkflowService = mcpAutomationProjectConfigurationWorkflowService;
        this.mcpConnectedUserProjectWorkflowService = mcpConnectedUserProjectWorkflowService;
        this.mcpServerMembership = mcpServerMembership;

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);

        this.nestedTransactionTemplate = transactionTemplate;
    }

    @Override
    public void addMissingRowsForLink(long mcpAutomationProjectConfigurationId) {
        Optional<McpAutomationProjectConfiguration> linkOptional = mcpAutomationProjectConfigurationService.fetch(
            mcpAutomationProjectConfigurationId);

        if (linkOptional.isEmpty()) {
            return;
        }

        McpAutomationProjectConfiguration link = linkOptional.get();

        List<McpAutomationProjectConfigurationWorkflow> linkWorkflows =
            mcpAutomationProjectConfigurationWorkflowService.getMcpAutomationProjectConfigurationWorkflows(
                mcpAutomationProjectConfigurationId);

        if (linkWorkflows.isEmpty()) {
            return;
        }

        for (ConnectedUserProjectConfiguration assignment : connectedUserProjectConfigurationService.getAssignments(
            link.getAutomationProjectConfigurationId())) {

            addMissingRows(assignment.getConnectedUserId(), link.getMcpServerId(), linkWorkflows, new HashMap<>());
        }
    }

    @Override
    public void addMissingRowsForReference(long connectedUserProjectWorkflowId) {
        ConnectedUserProjectWorkflow reference = connectedUserProjectWorkflowService.getConnectedUserProjectWorkflow(
            connectedUserProjectWorkflowId);

        if (!isActivatedReference(reference)) {
            return;
        }

        ConnectedUserProject connectedUserProject = connectedUserProjectService.getConnectedUserProject(
            reference.getConnectedUserProjectId());

        long connectedUserId = connectedUserProject.getConnectedUserId();

        Map<Long, Boolean> newRowEnabledByMcpServerId = new HashMap<>();

        for (long automationProjectConfigurationId : getAssignedConfigurationIds(connectedUserId)) {
            for (McpAutomationProjectConfiguration link : mcpAutomationProjectConfigurationService
                .getAutomationProjectConfigurationMcpAutomationProjectConfigurations(
                    automationProjectConfigurationId)) {

                for (McpAutomationProjectConfigurationWorkflow linkWorkflow : mcpAutomationProjectConfigurationWorkflowService
                    .getMcpAutomationProjectConfigurationWorkflows(link.getId())) {

                    if (Objects.equals(linkWorkflow.getWorkflowUuid(), reference.getAutomationUuid())) {
                        createMissingRow(
                            connectedUserId, link.getMcpServerId(), reference.getId(), linkWorkflow.getId(),
                            newRowEnabledByMcpServerId);
                    }
                }
            }
        }
    }

    @Override
    public void provisionMcpServer(long connectedUserId, long mcpServerId) {
        Set<Long> assignedConfigurationIds = getAssignedConfigurationIds(connectedUserId);

        Map<String, ConnectedUserProjectWorkflow> referencesByAutomationUuid = getActivatedReferencesByAutomationUuid(
            connectedUserId);

        for (McpAutomationProjectConfiguration link : mcpAutomationProjectConfigurationService
            .getMcpServerMcpAutomationProjectConfigurations(mcpServerId)) {

            if (!assignedConfigurationIds.contains(link.getAutomationProjectConfigurationId())) {
                continue;
            }

            for (McpAutomationProjectConfigurationWorkflow linkWorkflow : mcpAutomationProjectConfigurationWorkflowService
                .getMcpAutomationProjectConfigurationWorkflows(link.getId())) {

                ConnectedUserProjectWorkflow reference = referencesByAutomationUuid.get(linkWorkflow.getWorkflowUuid());

                if (reference == null) {
                    continue;
                }

                Optional<McpConnectedUserProjectWorkflow> rowOptional = mcpConnectedUserProjectWorkflowService.fetch(
                    reference.getId(), linkWorkflow.getId());

                if (rowOptional.isPresent()) {
                    McpConnectedUserProjectWorkflow row = rowOptional.get();

                    mcpConnectedUserProjectWorkflowService.updateEnabled(row.getId(), true);
                } else {
                    mcpConnectedUserProjectWorkflowService.create(reference.getId(), linkWorkflow.getId(), true);
                }
            }
        }
    }

    @Override
    public void remapConnectedUser(
        long connectedUserId, long previousAutomationProjectConfigurationId, long automationProjectConfigurationId) {

        List<SourceRow> sourceRows = getSourceRows(connectedUserId, previousAutomationProjectConfigurationId);

        Map<Long, Boolean> newRowEnabledByMcpServerId = new HashMap<>();

        for (SourceRow sourceRow : sourceRows) {
            getNewRowEnabled(connectedUserId, sourceRow.mcpServerId(), newRowEnabledByMcpServerId);
        }

        for (SourceRow sourceRow : sourceRows) {
            McpConnectedUserProjectWorkflow row = sourceRow.row();

            mcpConnectedUserProjectWorkflowService.delete(row.getId());

            createTargetRows(
                row, automationProjectConfigurationId, sourceRow.mcpServerId(), sourceRow.workflowUuid());
        }

        for (McpAutomationProjectConfiguration link : mcpAutomationProjectConfigurationService
            .getAutomationProjectConfigurationMcpAutomationProjectConfigurations(automationProjectConfigurationId)) {

            List<McpAutomationProjectConfigurationWorkflow> linkWorkflows =
                mcpAutomationProjectConfigurationWorkflowService.getMcpAutomationProjectConfigurationWorkflows(
                    link.getId());

            addMissingRows(connectedUserId, link.getMcpServerId(), linkWorkflows, newRowEnabledByMcpServerId);
        }
    }

    private void addMissingRows(
        long connectedUserId, long mcpServerId, List<McpAutomationProjectConfigurationWorkflow> linkWorkflows,
        Map<Long, Boolean> newRowEnabledByMcpServerId) {

        Map<String, ConnectedUserProjectWorkflow> referencesByAutomationUuid = getActivatedReferencesByAutomationUuid(
            connectedUserId);

        for (McpAutomationProjectConfigurationWorkflow linkWorkflow : linkWorkflows) {
            ConnectedUserProjectWorkflow reference = referencesByAutomationUuid.get(linkWorkflow.getWorkflowUuid());

            if (reference != null) {
                createMissingRow(
                    connectedUserId, mcpServerId, reference.getId(), linkWorkflow.getId(), newRowEnabledByMcpServerId);
            }
        }
    }

    private void createMissingRow(
        long connectedUserId, long mcpServerId, long connectedUserProjectWorkflowId,
        long mcpAutomationProjectConfigurationWorkflowId, Map<Long, Boolean> newRowEnabledByMcpServerId) {

        Optional<McpConnectedUserProjectWorkflow> rowOptional = mcpConnectedUserProjectWorkflowService.fetch(
            connectedUserProjectWorkflowId, mcpAutomationProjectConfigurationWorkflowId);

        if (rowOptional.isPresent()) {
            return;
        }

        boolean enabled = getNewRowEnabled(connectedUserId, mcpServerId, newRowEnabledByMcpServerId);

        try {
            nestedTransactionTemplate.executeWithoutResult(
                status -> mcpConnectedUserProjectWorkflowService.create(
                    connectedUserProjectWorkflowId, mcpAutomationProjectConfigurationWorkflowId, enabled));
        } catch (DuplicateKeyException duplicateKeyException) {
            log.debug(
                "MCP automation row for reference {} and link workflow {} was created concurrently",
                connectedUserProjectWorkflowId, mcpAutomationProjectConfigurationWorkflowId, duplicateKeyException);
        }
    }

    private void createTargetRows(
        McpConnectedUserProjectWorkflow sourceRow, long automationProjectConfigurationId, long mcpServerId,
        String workflowUuid) {

        for (McpAutomationProjectConfiguration targetLink : mcpAutomationProjectConfigurationService
            .getAutomationProjectConfigurationMcpAutomationProjectConfigurations(automationProjectConfigurationId)) {

            if (!Objects.equals(targetLink.getMcpServerId(), mcpServerId)) {
                continue;
            }

            for (McpAutomationProjectConfigurationWorkflow targetLinkWorkflow : mcpAutomationProjectConfigurationWorkflowService
                .getMcpAutomationProjectConfigurationWorkflows(targetLink.getId())) {

                if (!Objects.equals(targetLinkWorkflow.getWorkflowUuid(), workflowUuid)) {
                    continue;
                }

                Optional<McpConnectedUserProjectWorkflow> targetRowOptional =
                    mcpConnectedUserProjectWorkflowService.fetch(
                        sourceRow.getConnectedUserProjectWorkflowId(), targetLinkWorkflow.getId());

                if (targetRowOptional.isPresent()) {
                    continue;
                }

                mcpConnectedUserProjectWorkflowService.create(
                    sourceRow.getConnectedUserProjectWorkflowId(), targetLinkWorkflow.getId(), sourceRow.isEnabled());
            }
        }
    }

    private Map<String, ConnectedUserProjectWorkflow> getActivatedReferencesByAutomationUuid(long connectedUserId) {
        Map<String, ConnectedUserProjectWorkflow> referencesByAutomationUuid = new HashMap<>();

        for (ConnectedUserProject connectedUserProject : connectedUserProjectService
            .getConnectedUserConnectedUserProjects(connectedUserId)) {

            for (ConnectedUserProjectWorkflow connectedUserProjectWorkflow : connectedUserProjectWorkflowService
                .getConnectedUserProjectWorkflows(connectedUserProject.getId())) {

                if (isActivatedReference(connectedUserProjectWorkflow)) {
                    referencesByAutomationUuid.putIfAbsent(
                        connectedUserProjectWorkflow.getAutomationUuid(), connectedUserProjectWorkflow);
                }
            }
        }

        return referencesByAutomationUuid;
    }

    private boolean getNewRowEnabled(
        long connectedUserId, long mcpServerId, Map<Long, Boolean> newRowEnabledByMcpServerId) {

        Boolean enabled = newRowEnabledByMcpServerId.get(mcpServerId);

        if (enabled == null) {
            Optional<Boolean> connectedUserState = mcpServerMembership.getConnectedUserState(
                connectedUserId, mcpServerId);

            enabled = connectedUserState.orElse(true);

            newRowEnabledByMcpServerId.put(mcpServerId, enabled);
        }

        return enabled;
    }

    private List<SourceRow> getSourceRows(long connectedUserId, long previousAutomationProjectConfigurationId) {
        List<SourceRow> sourceRows = new ArrayList<>();

        for (McpConnectedUserProjectWorkflow row : mcpConnectedUserProjectWorkflowService
            .getConnectedUserMcpConnectedUserProjectWorkflows(connectedUserId)) {

            Optional<McpAutomationProjectConfigurationWorkflow> sourceLinkWorkflowOptional =
                mcpAutomationProjectConfigurationWorkflowService.fetch(
                    row.getMcpAutomationProjectConfigurationWorkflowId());

            if (sourceLinkWorkflowOptional.isEmpty()) {
                continue;
            }

            McpAutomationProjectConfigurationWorkflow sourceLinkWorkflow = sourceLinkWorkflowOptional.get();

            Optional<McpAutomationProjectConfiguration> sourceLinkOptional = mcpAutomationProjectConfigurationService
                .fetch(sourceLinkWorkflow.getMcpAutomationProjectConfigurationId());

            if (sourceLinkOptional.isEmpty()) {
                continue;
            }

            McpAutomationProjectConfiguration sourceLink = sourceLinkOptional.get();

            if (Objects.equals(
                sourceLink.getAutomationProjectConfigurationId(), previousAutomationProjectConfigurationId)) {

                sourceRows.add(new SourceRow(row, sourceLink.getMcpServerId(), sourceLinkWorkflow.getWorkflowUuid()));
            }
        }

        return sourceRows;
    }

    private Set<Long> getAssignedConfigurationIds(long connectedUserId) {
        Set<Long> assignedConfigurationIds = new HashSet<>();

        for (ConnectedUserProjectConfiguration assignment : connectedUserProjectConfigurationService
            .getConnectedUserAssignments(connectedUserId)) {

            assignedConfigurationIds.add(assignment.getConfigurationId());
        }

        return assignedConfigurationIds;
    }

    private static boolean isActivatedReference(ConnectedUserProjectWorkflow connectedUserProjectWorkflow) {
        return connectedUserProjectWorkflow.getAutomationUuid() != null && !connectedUserProjectWorkflow.isDangling();
    }

    private record SourceRow(McpConnectedUserProjectWorkflow row, long mcpServerId, String workflowUuid) {
    }
}

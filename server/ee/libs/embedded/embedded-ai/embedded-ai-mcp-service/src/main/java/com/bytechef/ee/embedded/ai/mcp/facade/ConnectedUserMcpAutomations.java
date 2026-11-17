/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.ai.mcp.dto.ConnectedUserMcpServerAutomationDTO;
import com.bytechef.ee.embedded.ai.mcp.service.McpAutomationProjectConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpAutomationProjectConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.dto.CallableAutomationDTO;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserMcpAutomationResolver;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowService;
import com.bytechef.platform.ai.tool.util.ToolNameUtils;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@Transactional(readOnly = true)
@ConditionalOnEEVersion
class ConnectedUserMcpAutomations {

    private static final Logger log = LoggerFactory.getLogger(ConnectedUserMcpAutomations.class);

    private static final String TOOL_DESCRIPTION = "toolDescription";

    private final AutomationProjectConfigurationService automationProjectConfigurationService;
    private final ConnectedUserMcpAutomationResolver connectedUserMcpAutomationResolver;
    private final ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService;
    private final ConnectedUserProjectService connectedUserProjectService;
    private final ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService;
    private final JobService jobService;
    private final McpAutomationProjectConfigurationService mcpAutomationProjectConfigurationService;
    private final McpAutomationProjectConfigurationWorkflowService mcpAutomationProjectConfigurationWorkflowService;
    private final McpConnectedUserProjectWorkflowService mcpConnectedUserProjectWorkflowService;
    private final TransactionTemplate nestedTransactionTemplate;
    private final PrincipalJobService principalJobService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    @SuppressWarnings("PMD.ExcessiveParameterList")
    ConnectedUserMcpAutomations(
        AutomationProjectConfigurationService automationProjectConfigurationService,
        ConnectedUserMcpAutomationResolver connectedUserMcpAutomationResolver,
        ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService,
        ConnectedUserProjectService connectedUserProjectService,
        ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService, JobService jobService,
        McpAutomationProjectConfigurationService mcpAutomationProjectConfigurationService,
        McpAutomationProjectConfigurationWorkflowService mcpAutomationProjectConfigurationWorkflowService,
        McpConnectedUserProjectWorkflowService mcpConnectedUserProjectWorkflowService,
        PlatformTransactionManager platformTransactionManager, PrincipalJobService principalJobService,
        ProjectService projectService, ProjectWorkflowService projectWorkflowService,
        WorkflowService workflowService) {

        this.automationProjectConfigurationService = automationProjectConfigurationService;
        this.connectedUserMcpAutomationResolver = connectedUserMcpAutomationResolver;
        this.connectedUserProjectConfigurationService = connectedUserProjectConfigurationService;
        this.connectedUserProjectService = connectedUserProjectService;
        this.connectedUserProjectWorkflowService = connectedUserProjectWorkflowService;
        this.jobService = jobService;
        this.mcpAutomationProjectConfigurationService = mcpAutomationProjectConfigurationService;
        this.mcpAutomationProjectConfigurationWorkflowService = mcpAutomationProjectConfigurationWorkflowService;
        this.mcpConnectedUserProjectWorkflowService = mcpConnectedUserProjectWorkflowService;

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        transactionTemplate.setReadOnly(true);

        this.nestedTransactionTemplate = transactionTemplate;
        this.principalJobService = principalJobService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
    }

    ConnectedUserMcpServerAutomationDTO getAutomation(long connectedUserId, long mcpConnectedUserProjectWorkflowId) {
        McpConnectedUserProjectWorkflow row = getConnectedUserRow(connectedUserId, mcpConnectedUserProjectWorkflowId);

        LinkedRow linkedRow = fetchLinkedRow(row).orElseThrow(
            () -> new IllegalArgumentException(
                "MCP automation row %d has no linked workflow".formatted(mcpConnectedUserProjectWorkflowId)));

        McpAutomationProjectConfiguration link = linkedRow.link();
        McpAutomationProjectConfigurationWorkflow linkWorkflow = linkedRow.linkWorkflow();

        long connectedUserProjectWorkflowId = connectedUserMcpAutomationResolver
            .fetchConnectedUserProjectWorkflowId(
                connectedUserId, link.getAutomationProjectConfigurationId(), linkWorkflow.getWorkflowUuid())
            .orElse(row.getConnectedUserProjectWorkflowId());

        return toAutomationDTO(connectedUserId, linkedRow, connectedUserProjectWorkflowId);
    }

    Map<Long, List<ConnectedUserMcpServerAutomationDTO>> getAutomationsByMcpServerId(long connectedUserId) {
        Map<Long, List<ConnectedUserMcpServerAutomationDTO>> automationsByMcpServerId = new HashMap<>();

        for (McpConnectedUserProjectWorkflow row : mcpConnectedUserProjectWorkflowService
            .getConnectedUserMcpConnectedUserProjectWorkflows(connectedUserId)) {

            Optional<LinkedRow> linkedRowOptional = fetchLinkedRow(row);

            if (linkedRowOptional.isEmpty()) {
                continue;
            }

            LinkedRow linkedRow = linkedRowOptional.get();

            McpAutomationProjectConfiguration link = linkedRow.link();
            McpAutomationProjectConfigurationWorkflow linkWorkflow = linkedRow.linkWorkflow();

            if (!linkWorkflow.isEnabled()) {
                continue;
            }

            Optional<Long> connectedUserProjectWorkflowIdOptional = connectedUserMcpAutomationResolver
                .fetchConnectedUserProjectWorkflowId(
                    connectedUserId, link.getAutomationProjectConfigurationId(), linkWorkflow.getWorkflowUuid());

            if (connectedUserProjectWorkflowIdOptional.isEmpty()) {
                continue;
            }

            List<ConnectedUserMcpServerAutomationDTO> automations = automationsByMcpServerId.computeIfAbsent(
                link.getMcpServerId(), key -> new ArrayList<>());

            automations.add(
                toIsolatedAutomationDTO(connectedUserId, linkedRow, connectedUserProjectWorkflowIdOptional.get()));
        }

        for (List<ConnectedUserMcpServerAutomationDTO> automations : automationsByMcpServerId.values()) {
            automations.sort(
                Comparator.comparing(ConnectedUserMcpServerAutomationDTO::name, String.CASE_INSENSITIVE_ORDER));
        }

        return automationsByMcpServerId;
    }

    AutomationCoverage getCoverage(long connectedUserId, long mcpServerId) {
        Set<Long> assignedConfigurationIds = new HashSet<>();

        for (ConnectedUserProjectConfiguration assignment : connectedUserProjectConfigurationService
            .getConnectedUserAssignments(connectedUserId)) {

            assignedConfigurationIds.add(assignment.getConfigurationId());
        }

        Set<String> activatedWorkflowUuids = getActivatedWorkflowUuids(connectedUserId);

        Set<String> coveredNames = new TreeSet<>();
        Set<String> missingNames = new TreeSet<>();

        for (McpAutomationProjectConfiguration link : mcpAutomationProjectConfigurationService
            .getMcpServerMcpAutomationProjectConfigurations(mcpServerId)) {

            if (!assignedConfigurationIds.contains(link.getAutomationProjectConfigurationId())) {
                continue;
            }

            AutomationProjectConfiguration configuration = automationProjectConfigurationService.getConfiguration(
                link.getAutomationProjectConfigurationId());

            for (McpAutomationProjectConfigurationWorkflow linkWorkflow : mcpAutomationProjectConfigurationWorkflowService
                .getMcpAutomationProjectConfigurationWorkflows(link.getId())) {

                if (!linkWorkflow.isEnabled()) {
                    continue;
                }

                String name = getToolName(linkWorkflow, fetchPinnedWorkflow(configuration, linkWorkflow));

                if (activatedWorkflowUuids.contains(linkWorkflow.getWorkflowUuid())) {
                    coveredNames.add(name);
                } else {
                    missingNames.add(name);
                }
            }
        }

        missingNames.removeAll(coveredNames);

        return new AutomationCoverage(coveredNames, missingNames);
    }

    List<McpConnectedUserProjectWorkflow> getMcpServerRows(long connectedUserId, long mcpServerId) {
        List<McpConnectedUserProjectWorkflow> rows = new ArrayList<>();

        for (McpConnectedUserProjectWorkflow row : mcpConnectedUserProjectWorkflowService
            .getConnectedUserMcpConnectedUserProjectWorkflows(connectedUserId)) {

            Optional<LinkedRow> linkedRowOptional = fetchLinkedRow(row);

            if (linkedRowOptional.isPresent()) {
                LinkedRow linkedRow = linkedRowOptional.get();

                McpAutomationProjectConfiguration link = linkedRow.link();

                if (Objects.equals(link.getMcpServerId(), mcpServerId)) {
                    rows.add(row);
                }
            }
        }

        return rows;
    }

    void requireConnectedUserRow(long connectedUserId, long mcpConnectedUserProjectWorkflowId) {
        getConnectedUserRow(connectedUserId, mcpConnectedUserProjectWorkflowId);
    }

    private Optional<LinkedRow> fetchLinkedRow(McpConnectedUserProjectWorkflow row) {
        Optional<McpAutomationProjectConfigurationWorkflow> linkWorkflowOptional =
            mcpAutomationProjectConfigurationWorkflowService
                .fetch(row.getMcpAutomationProjectConfigurationWorkflowId());

        if (linkWorkflowOptional.isEmpty()) {
            return Optional.empty();
        }

        McpAutomationProjectConfigurationWorkflow linkWorkflow = linkWorkflowOptional.get();

        Optional<McpAutomationProjectConfiguration> linkOptional = mcpAutomationProjectConfigurationService.fetch(
            linkWorkflow.getMcpAutomationProjectConfigurationId());

        return linkOptional.map(link -> new LinkedRow(row, link, linkWorkflow));
    }

    private @Nullable Workflow fetchPinnedWorkflow(
        AutomationProjectConfiguration configuration, McpAutomationProjectConfigurationWorkflow linkWorkflow) {

        Optional<ProjectWorkflow> projectWorkflowOptional = projectWorkflowService.fetchProjectWorkflow(
            configuration.getProjectId(), configuration.getProjectVersion(), linkWorkflow.getWorkflowUuid());

        return projectWorkflowOptional
            .map(projectWorkflow -> workflowService.getWorkflow(projectWorkflow.getWorkflowId()))
            .orElse(null);
    }

    private Set<String> getActivatedWorkflowUuids(long connectedUserId) {
        Set<String> activatedWorkflowUuids = new HashSet<>();

        for (ConnectedUserProject connectedUserProject : connectedUserProjectService
            .getConnectedUserConnectedUserProjects(connectedUserId)) {

            for (ConnectedUserProjectWorkflow connectedUserProjectWorkflow : connectedUserProjectWorkflowService
                .getConnectedUserProjectWorkflows(connectedUserProject.getId())) {

                String automationUuid = connectedUserProjectWorkflow.getAutomationUuid();

                if (automationUuid != null && !connectedUserProjectWorkflow.isDangling()) {
                    activatedWorkflowUuids.add(automationUuid);
                }
            }
        }

        return activatedWorkflowUuids;
    }

    private McpConnectedUserProjectWorkflow getConnectedUserRow(
        long connectedUserId, long mcpConnectedUserProjectWorkflowId) {

        for (McpConnectedUserProjectWorkflow row : mcpConnectedUserProjectWorkflowService
            .getConnectedUserMcpConnectedUserProjectWorkflows(connectedUserId)) {

            if (Objects.equals(row.getId(), mcpConnectedUserProjectWorkflowId)) {
                return row;
            }
        }

        throw new IllegalArgumentException(
            "MCP automation row %d does not belong to connected user %d".formatted(
                mcpConnectedUserProjectWorkflowId, connectedUserId));
    }

    private @Nullable Instant getLastExecutionDate(
        long connectedUserProjectWorkflowId, long projectId, String workflowUuid) {

        ConnectedUserProjectWorkflow reference = connectedUserProjectWorkflowService.getConnectedUserProjectWorkflow(
            connectedUserProjectWorkflowId);

        Long projectDeploymentId = reference.getProjectDeploymentId();

        if (projectDeploymentId == null) {
            return null;
        }

        List<String> workflowIds = new ArrayList<>();

        for (ProjectWorkflow projectWorkflow : projectWorkflowService.getProjectWorkflows(projectId, workflowUuid)) {
            workflowIds.add(projectWorkflow.getWorkflowId());
        }

        if (workflowIds.isEmpty()) {
            return null;
        }

        return principalJobService.fetchLastWorkflowJobId(projectDeploymentId, workflowIds, PlatformType.AUTOMATION)
            .map(jobService::getJob)
            .map(Job::getEndDate)
            .orElse(null);
    }

    private static @Nullable String getToolDescription(
        McpAutomationProjectConfigurationWorkflow linkWorkflow, @Nullable Workflow workflow) {

        Map<String, ?> parameters = linkWorkflow.getParameters();

        if (parameters != null && parameters.get(TOOL_DESCRIPTION) instanceof String toolDescription &&
            !toolDescription.isBlank()) {

            return toolDescription;
        }

        return workflow == null ? null : workflow.getDescription();
    }

    private static String getToolName(
        McpAutomationProjectConfigurationWorkflow linkWorkflow, @Nullable Workflow workflow) {

        String label = workflow == null ? null : workflow.getLabel();

        return Objects.requireNonNullElse(
            ToolNameUtils.getWorkflowToolName(linkWorkflow.getParameters(), label),
            linkWorkflow.getWorkflowUuid());
    }

    private ConnectedUserMcpServerAutomationDTO toIsolatedAutomationDTO(
        long connectedUserId, LinkedRow linkedRow, long connectedUserProjectWorkflowId) {

        try {
            return Objects.requireNonNull(
                nestedTransactionTemplate.execute(
                    status -> toAutomationDTO(connectedUserId, linkedRow, connectedUserProjectWorkflowId)));
        } catch (RuntimeException exception) {
            McpConnectedUserProjectWorkflow row = linkedRow.row();

            log.warn("Listing MCP automation row {} as unavailable", row.getId(), exception);

            McpAutomationProjectConfigurationWorkflow linkWorkflow = linkedRow.linkWorkflow();

            return new ConnectedUserMcpServerAutomationDTO(
                row.getId(), connectedUserProjectWorkflowId, "", linkWorkflow.getWorkflowUuid(),
                getToolName(linkWorkflow, null), getToolDescription(linkWorkflow, null), row.isEnabled(), false,
                null);
        }
    }

    private ConnectedUserMcpServerAutomationDTO toAutomationDTO(
        long connectedUserId, LinkedRow linkedRow, long connectedUserProjectWorkflowId) {

        McpAutomationProjectConfiguration link = linkedRow.link();
        McpAutomationProjectConfigurationWorkflow linkWorkflow = linkedRow.linkWorkflow();

        AutomationProjectConfiguration configuration = automationProjectConfigurationService.getConfiguration(
            link.getAutomationProjectConfigurationId());

        Project project = projectService.getProject(configuration.getProjectId());

        Workflow workflow = fetchPinnedWorkflow(configuration, linkWorkflow);

        String workflowUuid = linkWorkflow.getWorkflowUuid();

        boolean available = connectedUserMcpAutomationResolver
            .fetchCallableAutomation(connectedUserId, link.getAutomationProjectConfigurationId(), workflowUuid)
            .filter(CallableAutomationDTO::isToolCallable)
            .isPresent();

        McpConnectedUserProjectWorkflow row = linkedRow.row();

        return new ConnectedUserMcpServerAutomationDTO(
            row.getId(), connectedUserProjectWorkflowId, project.getName(), workflowUuid,
            getToolName(linkWorkflow, workflow), getToolDescription(linkWorkflow, workflow), row.isEnabled(), available,
            getLastExecutionDate(connectedUserProjectWorkflowId, configuration.getProjectId(), workflowUuid));
    }

    record AutomationCoverage(Set<String> coveredNames, Set<String> missingNames) {
    }

    private record LinkedRow(
        McpConnectedUserProjectWorkflow row, McpAutomationProjectConfiguration link,
        McpAutomationProjectConfigurationWorkflow linkWorkflow) {
    }
}

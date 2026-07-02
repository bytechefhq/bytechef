/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.exception.WorkflowErrorType;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.security.SkipAutomationAuthorization;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.ai.copilot.service.CopilotWorkflowGenerator;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflowConnection;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.CopilotChatContextDTO;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
@SkipAutomationAuthorization
public class ConnectedUserProjectFacadeImpl implements ConnectedUserProjectFacade {

    private static final String MARKER = "__EMBEDDED__";

    private final AutomationWorkflowProjectFacade automationWorkflowProjectFacade;
    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectedUserProjectService connectUserProjectService;
    private final ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager;
    private final ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService;
    private final ConnectedUserService connectedUserService;
    private final ConnectionService connectionService;
    private final @Nullable CopilotWorkflowGenerator copilotWorkflowGenerator;
    private final EnvironmentService environmentService;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationService integrationService;
    private final JobService jobService;
    private final PrincipalJobService principalJobService;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectFacade projectFacade;
    private final ProjectService projectService;
    private final ProjectWorkflowFacade projectWorkflowFacade;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowFacade workflowFacade;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationFacade workflowTestConfigurationFacade;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectFacadeImpl(
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade,
        ComponentDefinitionService componentDefinitionService,
        ConnectedUserProjectService connectUserProjectService,
        ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager,
        ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService,
        ConnectedUserService connectedUserService, ConnectionService connectionService,
        @Lazy @Nullable CopilotWorkflowGenerator copilotWorkflowGenerator, EnvironmentService environmentService,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationService integrationService, JobService jobService, PrincipalJobService principalJobService,
        ProjectDeploymentFacade projectDeploymentFacade, ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectFacade projectFacade,
        ProjectService projectService, ProjectWorkflowFacade projectWorkflowFacade,
        ProjectWorkflowService projectWorkflowService, WorkflowFacade workflowFacade, WorkflowService workflowService,
        WorkflowTestConfigurationFacade workflowTestConfigurationFacade,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.automationWorkflowProjectFacade = automationWorkflowProjectFacade;
        this.componentDefinitionService = componentDefinitionService;
        this.connectUserProjectService = connectUserProjectService;
        this.connectedUserProjectWorkflowManager = connectedUserProjectWorkflowManager;
        this.connectedUserProjectWorkflowService = connectedUserProjectWorkflowService;
        this.connectedUserService = connectedUserService;
        this.connectionService = connectionService;
        this.copilotWorkflowGenerator = copilotWorkflowGenerator;
        this.environmentService = environmentService;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationService = integrationService;
        this.jobService = jobService;
        this.principalJobService = principalJobService;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectFacade = projectFacade;
        this.projectService = projectService;
        this.projectWorkflowFacade = projectWorkflowFacade;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowFacade = workflowFacade;
        this.workflowService = workflowService;
        this.workflowTestConfigurationFacade = workflowTestConfigurationFacade;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    public String createProjectWorkflow(String externalUserId, String definition, Environment environment) {
        return connectedUserProjectWorkflowManager.createProjectWorkflow(externalUserId, definition, environment);
    }

    @Override
    public String copyWorkflowTemplate(String externalUserId, String workflowUuid, Environment environment) {
        boolean isPublishedCatalogWorkflowTemplate = automationWorkflowProjectFacade.getPublishedProjects()
            .stream()
            .flatMap(project -> CollectionUtils.stream(project.workflowTemplates()))
            .anyMatch(workflowTemplate -> Objects.equals(workflowTemplate.workflowUuid(), workflowUuid));

        if (!isPublishedCatalogWorkflowTemplate) {
            throw new IllegalArgumentException(
                "Not a published catalog workflow template: " + workflowUuid);
        }

        String publishedWorkflowId = projectWorkflowService.getLastPublishedWorkflowId(workflowUuid);

        Workflow workflow = workflowService.getWorkflow(publishedWorkflowId);

        return createProjectWorkflow(externalUserId, workflow.getDefinition(), environment);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String createProjectWorkflow(
        String externalUserId, String prompt, @Nullable String systemPrompt, Environment environment,
        boolean generate) {

        if (!generate) {
            return connectedUserProjectWorkflowManager.createProjectWorkflow(externalUserId, prompt, environment);
        }

        if (StringUtils.isBlank(prompt)) {
            throw new IllegalArgumentException("Prompt must not be blank");
        }

        if (copilotWorkflowGenerator == null) {
            throw new IllegalStateException(
                "AI Copilot is not enabled. Set bytechef.ai.copilot.enabled=true to use workflow generation.");
        }

        String definition = buildInitialDefinition(toWorkflowLabel(prompt));

        String workflowUuid = connectedUserProjectWorkflowManager.createProjectWorkflow(
            externalUserId, definition, environment);
        String workflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);

        Set<String> allowedComponentNames = resolveAllowedComponentNames(environment);

        copilotWorkflowGenerator.generateWorkflow(workflowId, prompt, systemPrompt, allowedComponentNames);

        return workflowUuid;
    }

    @Override
    public void deleteProjectWorkflow(String externalUserId, String workflowUuid, Environment environment) {
        ConnectedUserProject connectedUserProject = connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            externalUserId, environment);

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(
            connectedUserProject.getProjectId(), workflowUuid);

        Set<Long> connectionIds = new HashSet<>();

        for (ProjectWorkflow projectWorkflow : projectWorkflows) {
            connectedUserProjectWorkflowService
                .fetchConnectedUserProjectWorkflow(connectedUserProject.getId(), projectWorkflow.getId())
                .ifPresent(connectedUserProjectWorkflow -> {
                    connectionIds.addAll(
                        connectedUserProjectWorkflow.getConnections()
                            .stream()
                            .map(ConnectedUserProjectWorkflowConnection::getConnectionId)
                            .toList());

                    connectedUserProjectWorkflowService.delete(connectedUserProjectWorkflow.getId());
                });

            projectWorkflowFacade.deleteWorkflow(projectWorkflow.getWorkflowId());
        }

        for (Long connectionId : connectionIds) {
            connectionService.delete(connectionId);
        }
    }

    @Override
    public void deleteProjectWorkflow(long connectedUserProjectWorkflowId) {
        ConnectedUserProjectWorkflow connectedUserProjectWorkflow = connectedUserProjectWorkflowService
            .getConnectedUserProjectWorkflow(connectedUserProjectWorkflowId);

        ConnectedUserProject connectedUserProject = connectUserProjectService.getConnectedUserProject(
            connectedUserProjectWorkflow.getConnectedUserProjectId());

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserProject.getConnectedUserId());

        ProjectWorkflow projectWorkflow = projectWorkflowService.getProjectWorkflow(
            connectedUserProjectWorkflow.getProjectWorkflowId());

        deleteProjectWorkflow(
            connectedUser.getExternalId(), projectWorkflow.getUuidAsString(), connectedUser.getEnvironment());
    }

    @Override
    public void enableProjectWorkflow(
        String externalUserId, String workflowUuid, boolean enable, Long environmentId) {

        Environment environment = environmentId == null
            ? Environment.PRODUCTION : environmentService.getEnvironment(environmentId);

        ConnectedUserProject connectedUserProject = connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            externalUserId, environment);

        long projectDeploymentId = projectDeploymentService.getProjectDeploymentId(
            connectedUserProject.getProjectId(), environment);

        String workflowId = projectWorkflowService
            .fetchProjectWorkflowWorkflowId(projectDeploymentId, workflowUuid)
            .orElseThrow(() -> {
                boolean existsInProject = projectWorkflowService
                    .fetchLastProjectWorkflowId(connectedUserProject.getProjectId(), workflowUuid)
                    .isPresent();

                if (existsInProject) {
                    return new ConfigurationException(
                        "Workflow with workflowUuid '%s' is not in the active deployment; publish the project before %s it"
                            .formatted(workflowUuid, enable ? "enabling" : "disabling"),
                        WorkflowErrorType.WORKFLOW_NOT_DEPLOYED);
                }

                return new ConfigurationException(
                    "Workflow with workflowUuid '%s' does not exist".formatted(workflowUuid),
                    WorkflowErrorType.WORKFLOW_NOT_FOUND);
            });

        projectDeploymentFacade.enableProjectDeploymentWorkflow(
            connectedUserProject.getProjectId(), workflowId, enable, environment);
    }

    @Override
    public void enableProjectWorkflow(long connectedUserProjectWorkflowId, boolean enable) {
        ConnectedUserProjectWorkflow connectedUserProjectWorkflow = connectedUserProjectWorkflowService
            .getConnectedUserProjectWorkflow(connectedUserProjectWorkflowId);

        ConnectedUserProject connectedUserProject = connectUserProjectService.getConnectedUserProject(
            connectedUserProjectWorkflow.getConnectedUserProjectId());

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserProject.getConnectedUserId());

        ProjectWorkflow projectWorkflow = projectWorkflowService.getProjectWorkflow(
            connectedUserProjectWorkflow.getProjectWorkflowId());

        enableProjectWorkflow(
            connectedUser.getExternalId(), projectWorkflow.getUuidAsString(), enable,
            connectedUser.getEnvironmentId());
    }

    @Override
    public ConnectedUserProjectWorkflowDTO getConnectedUserProjectWorkflow(
        String externalUserId, String workflowUuid, Long environmentId) {

        Environment environment = environmentId == null
            ? Environment.PRODUCTION : environmentService.getEnvironment(environmentId);

        ConnectedUserProject connectedUserProject = connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            externalUserId, environment);

        ProjectWorkflow projectWorkflow = projectWorkflowService.getLastProjectWorkflow(
            connectedUserProject.getProjectId(), workflowUuid);

        ConnectedUserProjectWorkflow connectedUserProjectWorkflow = connectedUserProjectWorkflowService
            .getConnectedUserProjectWorkflow(connectedUserProject.getId(), projectWorkflow.getId());

        return new ConnectedUserProjectWorkflowDTO(
            connectedUserProject.getConnectedUserId(), connectedUserProjectWorkflow,
            isProjectDeploymentWorkflowEnabled(projectWorkflow, environment),
            getWorkflowLastExecutionDate(projectWorkflow.getWorkflowId()), projectWorkflow,
            workflowFacade.getWorkflow(projectWorkflow.getWorkflowId()));
    }

    @Override
    public List<ConnectedUserProjectWorkflowDTO> getConnectedUserProjectWorkflows(
        String externalUserId, Environment environment) {

        ConnectedUserProject connectedUserProject = connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            externalUserId, environment);

        return getConnectedUserProjectWorkflows(connectedUserProject, environment);
    }

    @Override
    public List<ConnectedUserProjectDTO> getConnectedUserProjects(Long connectedUserId, Environment environment) {
        return connectUserProjectService.getConnectedUserProjects(connectedUserId, environment)
            .stream()
            .filter(connectedUserProject -> {
                if (connectedUserId != null) {
                    return Objects.equals(connectedUserProject.getConnectedUserId(), connectedUserId);
                }

                return true;
            })
            .filter(connectedUserProject -> {
                if (environment != null) {
                    return projectDeploymentService
                        .fetchProjectDeployment(connectedUserProject.getProjectId(), environment)
                        .map(projectDeployment -> projectDeployment.getEnvironment() == environment)
                        .orElse(false);
                }

                return true;
            })
            .map(connectedUserProject -> {
                ConnectedUser connectedUser = connectedUserService.getConnectedUser(
                    connectedUserProject.getConnectedUserId());

                List<ConnectedUserProjectWorkflowDTO> connectedUserProjectWorkflows =
                    getConnectedUserProjectWorkflows(connectedUserProject, connectedUser.getEnvironment());

                ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(
                    connectedUserProject.getProjectId(), connectedUser.getEnvironment());

                return new ConnectedUserProjectDTO(
                    connectedUserProject, connectedUser, connectedUserProjectWorkflows,
                    getProjectDeploymentLastExecutionDate(projectDeployment.getId()), projectDeployment);
            })
            .toList();
    }

    @Override
    public List<ConnectedUserProjectDTO> getConnectedUserProjects(String externalUserId, Environment environment) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        return getConnectedUserProjects(connectedUser.getId(), environment);
    }

    @Override
    public CopilotChatContextDTO prepareCopilotChat(
        String externalUserId, String workflowUuid, Environment environment) {

        getConnectedUserProjectWorkflow(externalUserId, workflowUuid, (long) environment.ordinal());

        String workflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);

        Set<String> allowedComponentNames = resolveAllowedComponentNames(environment);

        return new CopilotChatContextDTO(workflowId, allowedComponentNames);
    }

    @Override
    public void publishProjectWorkflow(
        String externalUserId, String workflowUuid, String description, Long environmentId) {

        Environment environment = environmentId == null
            ? Environment.PRODUCTION : environmentService.getEnvironment(environmentId);

        ConnectedUserProject connectedUserProject = connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            externalUserId, environment);

        String workflowId = projectWorkflowService
            .fetchLastProjectWorkflowId(connectedUserProject.getProjectId(), workflowUuid)
            .orElseThrow(() -> new ConfigurationException(
                "Workflow with workflowUuid: %s not exist".formatted(workflowUuid),
                WorkflowErrorType.WORKFLOW_NOT_FOUND));

        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        List<ProjectDeploymentWorkflowConnection> connections = workflowTestConfigurationService
            .fetchWorkflowTestConfiguration(workflowId, environment.ordinal())
            .map(WorkflowTestConfiguration::getConnections)
            .orElse(List.of())
            .stream()
            .filter(workflowTestConfigurationConnection -> {
                Connection connection = connectionService.getConnection(
                    workflowTestConfigurationConnection.getConnectionId());

                return connection.getEnvironmentId() == environment.ordinal();
            })
            .map(workflowTestConfigurationConnection -> new ProjectDeploymentWorkflowConnection(
                workflowTestConfigurationConnection.getConnectionId(),
                workflowTestConfigurationConnection.getWorkflowConnectionKey(),
                workflowTestConfigurationConnection.getWorkflowNodeName()))
            .toList();

        int newProjectVersion = projectFacade.publishProject(connectedUserProject.getProjectId(), description, false);

        if (newProjectVersion == 2) {
            ProjectDeployment projectDeployment = new ProjectDeployment();

            projectDeployment.setEnabled(true);
            projectDeployment.setEnvironment(environment);

            ConnectedUser connectedUser = connectedUserService.getConnectedUser(
                connectedUserProject.getConnectedUserId());

            projectDeployment.setName(MARKER + connectedUser.getExternalId());

            projectDeployment.setProjectId(connectedUserProject.getProjectId());
            projectDeployment.setProjectVersion(1);

            projectDeploymentFacade.createProjectDeployment(projectDeployment, workflowId, connections);
        } else {
            projectDeploymentFacade.updateProjectDeployment(
                connectedUserProject.getProjectId(), newProjectVersion - 1, workflowUuid, connections,
                environmentId);
        }

        connectedUserProjectWorkflowService.incrementWorkflowVersion(
            connectedUserProject.getId(), projectWorkflow.getId());
    }

    @Override
    public void updateProjectWorkflow(
        String externalUserId, String workflowUuid, String definition, Environment environment) {

        connectedUserProjectWorkflowManager.updateProjectWorkflow(externalUserId, workflowUuid, definition,
            environment);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String updateProjectWorkflow(
        String externalUserId, String workflowUuid, String prompt, Environment environment, boolean generate) {

        if (!generate) {
            connectedUserProjectWorkflowManager.updateProjectWorkflow(externalUserId, workflowUuid, prompt,
                environment);

            return workflowUuid;
        }

        if (StringUtils.isBlank(prompt)) {
            throw new IllegalArgumentException("Prompt must not be blank");
        }

        if (copilotWorkflowGenerator == null) {
            throw new IllegalStateException(
                "AI Copilot is not enabled. Set bytechef.ai.copilot.enabled=true to use workflow generation.");
        }

        ConnectedUserProject connectedUserProject = connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            externalUserId, environment);

        ProjectWorkflow projectWorkflow = projectWorkflowService.getLastProjectWorkflow(
            connectedUserProject.getProjectId(), workflowUuid);

        Set<String> allowedComponentNames = resolveAllowedComponentNames(environment);

        copilotWorkflowGenerator.generateWorkflow(projectWorkflow.getWorkflowId(), prompt, null, allowedComponentNames);

        return workflowUuid;
    }

    @Override
    public void updateWorkflowConfigurationConnection(
        String externalUserId, String workflowUuid, String workflowNodeName, String workflowConnectionKey,
        long connectionId, Environment environment) {

        ConnectedUserProject connectedUserProject = connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            externalUserId, environment);

        String workflowId = projectWorkflowService
            .fetchLastProjectWorkflowId(connectedUserProject.getProjectId(), workflowUuid)
            .orElseThrow(() -> new ConfigurationException(
                "Workflow with workflowUuid: %s not exist".formatted(workflowUuid),
                WorkflowErrorType.WORKFLOW_NOT_FOUND));

        workflowTestConfigurationFacade.saveWorkflowTestConfigurationConnection(
            workflowId, workflowNodeName, workflowConnectionKey, connectionId, environment.ordinal());
    }

    private List<ConnectedUserProjectWorkflowDTO> getConnectedUserProjectWorkflows(
        ConnectedUserProject connectedUserProject, Environment environment) {

        Project project = projectService.getProject(connectedUserProject.getProjectId());

        List<ProjectWorkflow> latestProjectWorkflows = projectWorkflowService.getProjectWorkflows(
            project.getId(), project.getLastProjectVersion());

        List<String> latestWorkflowIds = latestProjectWorkflows.stream()
            .map(ProjectWorkflow::getWorkflowId)
            .toList();

        return workflowService.getWorkflows(latestWorkflowIds)
            .stream()
            .map(workflow -> {
                ProjectWorkflow latestProjectWorkflow = latestProjectWorkflows.stream()
                    .filter(curProjectWorkflow -> Objects.equals(curProjectWorkflow.getWorkflowId(), workflow.getId()))
                    .findFirst()
                    .orElseThrow();

                ConnectedUserProjectWorkflow connectedUserProjectWorkflow = connectedUserProjectWorkflowService
                    .getConnectedUserProjectWorkflow(connectedUserProject.getId(), latestProjectWorkflow.getId());

                return new ConnectedUserProjectWorkflowDTO(
                    connectedUserProject.getConnectedUserId(), connectedUserProjectWorkflow,
                    isProjectDeploymentWorkflowEnabled(latestProjectWorkflow, environment),
                    getWorkflowLastExecutionDate(latestProjectWorkflow.getWorkflowId()), latestProjectWorkflow,
                    new WorkflowDTO(workflow, List.of(), List.of()));
            })
            .toList();
    }

    private Instant getJobEndDate(Long jobId) {
        Job job = jobService.getJob(jobId);

        return job.getEndDate();
    }

    private Instant getProjectDeploymentLastExecutionDate(long projectDeploymentId) {
        return OptionalUtils.mapOrElse(
            principalJobService.fetchLastJobId(projectDeploymentId, PlatformType.AUTOMATION), this::getJobEndDate,
            null);
    }

    private Instant getWorkflowLastExecutionDate(String workflowId) {
        return OptionalUtils.mapOrElse(jobService.fetchLastWorkflowJob(workflowId), Job::getEndDate, null);
    }

    private boolean isProjectDeploymentWorkflowEnabled(ProjectWorkflow projectWorkflow, Environment environment) {
        // DRAFT status
        if (projectWorkflow.getProjectVersion() == 1) {
            return false;
        }

        projectWorkflow = projectWorkflowService
            .fetchProjectWorkflow(
                projectWorkflow.getProjectId(), projectWorkflow.getProjectVersion() - 1,
                projectWorkflow.getUuidAsString())
            .orElse(null);

        if (projectWorkflow == null) {
            return false;
        }

        long projectDeploymentId = projectDeploymentService.getProjectDeploymentId(
            projectWorkflow.getProjectId(), environment);

        return projectDeploymentWorkflowService.isProjectDeploymentWorkflowEnabled(
            projectDeploymentId, projectWorkflow.getWorkflowId());
    }

    Set<String> resolveAllowedComponentNames(Environment environment) {
        List<IntegrationInstanceConfiguration> enabledIntegrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(environment, true);

        List<Long> integrationIds = enabledIntegrationInstanceConfigurations.stream()
            .map(IntegrationInstanceConfiguration::getIntegrationId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        Set<String> allowedComponentNames = integrationService.getIntegrations(integrationIds)
            .stream()
            .map(Integration::getComponentName)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));

        componentDefinitionService.getComponentDefinitions()
            .stream()
            .filter(componentDefinition -> !componentDefinition.isConnectionRequired())
            .map(ComponentDefinition::getName)
            .forEach(allowedComponentNames::add);

        return allowedComponentNames;
    }

    private static String buildInitialDefinition(String label) {
        Map<String, Object> definitionMap = new LinkedHashMap<>();

        definitionMap.put("label", label);
        definitionMap.put("description", "");
        definitionMap.put("inputs", List.of());
        definitionMap.put("triggers", List.of());
        definitionMap.put("tasks", List.of());

        return JsonUtils.write(definitionMap);
    }

    private static String toWorkflowLabel(String prompt) {
        return StringUtils.abbreviate(StringUtils.normalizeSpace(prompt), 80);
    }
}

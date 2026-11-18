/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ai.copilot.service.CopilotWorkflowGenerator;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.exception.WorkflowErrorType;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.domain.ProjectVersion;
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
import com.bytechef.ee.embedded.configuration.audit.AutomationProjectAuditEvents;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationReleaseDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserAutomationProjectConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowReferenceValuesDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.dto.CopilotChatContextDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionableAutomationProjectDTO;
import com.bytechef.ee.embedded.configuration.exception.AutomationProjectConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.exception.CodeWorkflowNotCopyableException;
import com.bytechef.ee.embedded.configuration.exception.ConnectedUserProjectWorkflowErrorType;
import com.bytechef.ee.embedded.configuration.exception.InvalidWorkflowException;
import com.bytechef.ee.embedded.configuration.exception.MissingConnectionException;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceAttentionResolver.ReferenceState;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceDeploymentManager.ProvidedNames;
import com.bytechef.ee.embedded.configuration.repository.ConnectedUserProjectWorkflowRepository;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.dto.WorkflowTriggerDTO;
import com.bytechef.platform.configuration.facade.WorkflowFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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

    private static final Logger log = LoggerFactory.getLogger(ConnectedUserProjectFacadeImpl.class);

    private final AutomationProjectConfigurationResolver automationProjectConfigurationResolver;
    private final AutomationProjectConfigurationService automationProjectConfigurationService;
    private final AutomationProjectFacade automationProjectFacade;
    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService;
    private final ConnectedUserProjectService connectUserProjectService;
    private final ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade;
    private final ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager;
    private final ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository;
    private final ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService;
    private final ConnectedUserReferenceAttentionResolver connectedUserReferenceAttentionResolver;
    private final ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager;
    private final ConnectedUserService connectedUserService;
    private final ConnectedUserWorkflowValidator connectedUserWorkflowValidator;
    private final ConnectedUserWorkflowWebhookUrlResolver connectedUserWorkflowWebhookUrlResolver;
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
    private final TransactionTemplate transactionTemplate;
    private final WorkflowComponentResolver workflowComponentResolver;
    private final WorkflowFacade workflowFacade;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationFacade workflowTestConfigurationFacade;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public ConnectedUserProjectFacadeImpl(
        AutomationProjectConfigurationResolver automationProjectConfigurationResolver,
        AutomationProjectConfigurationService automationProjectConfigurationService,
        AutomationProjectFacade automationProjectFacade,
        ComponentDefinitionService componentDefinitionService,
        ConnectedUserProjectConfigurationService connectedUserProjectConfigurationService,
        ConnectedUserProjectService connectUserProjectService,
        ConnectedUserWorkflowReferenceFacade connectedUserWorkflowReferenceFacade,
        ConnectedUserProjectWorkflowManager connectedUserProjectWorkflowManager,
        ConnectedUserProjectWorkflowRepository connectedUserProjectWorkflowRepository,
        ConnectedUserProjectWorkflowService connectedUserProjectWorkflowService,
        ConnectedUserReferenceAttentionResolver connectedUserReferenceAttentionResolver,
        ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager,
        ConnectedUserService connectedUserService, ConnectedUserWorkflowValidator connectedUserWorkflowValidator,
        ConnectedUserWorkflowWebhookUrlResolver connectedUserWorkflowWebhookUrlResolver,
        @Lazy @Nullable CopilotWorkflowGenerator copilotWorkflowGenerator, EnvironmentService environmentService,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationService integrationService, JobService jobService,
        PlatformTransactionManager platformTransactionManager, PrincipalJobService principalJobService,
        ProjectDeploymentFacade projectDeploymentFacade, ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectFacade projectFacade,
        ProjectService projectService, ProjectWorkflowFacade projectWorkflowFacade,
        ProjectWorkflowService projectWorkflowService, WorkflowComponentResolver workflowComponentResolver,
        WorkflowFacade workflowFacade, WorkflowService workflowService,
        WorkflowTestConfigurationFacade workflowTestConfigurationFacade,
        WorkflowTestConfigurationService workflowTestConfigurationService) {
        this.automationProjectConfigurationResolver = automationProjectConfigurationResolver;
        this.automationProjectConfigurationService = automationProjectConfigurationService;
        this.automationProjectFacade = automationProjectFacade;
        this.componentDefinitionService = componentDefinitionService;
        this.connectedUserProjectConfigurationService = connectedUserProjectConfigurationService;
        this.connectUserProjectService = connectUserProjectService;
        this.connectedUserWorkflowReferenceFacade = connectedUserWorkflowReferenceFacade;
        this.connectedUserProjectWorkflowManager = connectedUserProjectWorkflowManager;
        this.connectedUserProjectWorkflowRepository = connectedUserProjectWorkflowRepository;
        this.connectedUserProjectWorkflowService = connectedUserProjectWorkflowService;
        this.connectedUserReferenceAttentionResolver = connectedUserReferenceAttentionResolver;
        this.connectedUserReferenceDeploymentManager = connectedUserReferenceDeploymentManager;
        this.connectedUserService = connectedUserService;
        this.connectedUserWorkflowValidator = connectedUserWorkflowValidator;
        this.connectedUserWorkflowWebhookUrlResolver = connectedUserWorkflowWebhookUrlResolver;
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
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
        this.workflowComponentResolver = workflowComponentResolver;
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
        Optional<AutomationProjectDTO> releasedProjectOptional = automationProjectFacade
            .getPublishedProjects(externalUserId, environment)
            .stream()
            .filter(project -> CollectionUtils.stream(project.workflowTemplates())
                .anyMatch(workflowTemplate -> Objects.equals(workflowTemplate.workflowUuid(), workflowUuid)))
            .findFirst();

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        if (releasedProjectOptional.isEmpty()) {
            checkWorkflowTemplateReleased(connectedUser, workflowUuid);

            throw new IllegalArgumentException("Not a published automation workflow template: " + workflowUuid);
        }

        AutomationProjectDTO releasedProject = releasedProjectOptional.get();

        if (releasedProject.codeWorkflowProject()) {
            throw new CodeWorkflowNotCopyableException(workflowUuid);
        }

        ConnectedUserProject connectedUserProject = connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            externalUserId, environment);

        connectUserProjectService.lockConnectedUserProject(connectedUserProject.getId());

        connectedUserProjectWorkflowService
            .fetchCopiedConnectedUserProjectWorkflow(connectedUserProject.getId(), workflowUuid)
            .ifPresent(connectedUserProjectWorkflow -> {
                ProjectWorkflow copiedProjectWorkflow = projectWorkflowService.getProjectWorkflow(
                    connectedUserProjectWorkflow.getProjectWorkflowId());

                throw new ConfigurationException(
                    "Automation workflow template %s is already copied".formatted(workflowUuid),
                    Map.of("copiedWorkflowUuid", copiedProjectWorkflow.getUuidAsString()),
                    ConnectedUserProjectWorkflowErrorType.WORKFLOW_TEMPLATE_ALREADY_COPIED);
            });

        AutomationProjectConfiguration configuration = automationProjectConfigurationResolver
            .resolveAndAssign(connectedUser, releasedProject.id())
            .filter(AutomationProjectConfiguration::isEnabled)
            .orElseThrow(() -> newWorkflowTemplateNotReleasedException(workflowUuid));

        String publishedWorkflowId = getPinnedWorkflowId(
            releasedProject.id(), configuration.getProjectVersion(), workflowUuid);

        Workflow workflow = workflowService.getWorkflow(publishedWorkflowId);

        return connectedUserProjectWorkflowManager.createProjectWorkflow(
            externalUserId, workflow.getDefinition(), environment, workflowUuid);
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

        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        Set<String> allowedComponentNames = resolveAllowedComponentNames(environment);

        try {
            copilotWorkflowGenerator.generateWorkflow(
                workflowId, projectWorkflow.getProjectId(), environment.ordinal(), prompt, systemPrompt,
                allowedComponentNames);
        } catch (RuntimeException runtimeException) {
            try {
                deleteProjectWorkflow(externalUserId, workflowUuid, environment);
            } catch (RuntimeException deleteException) {
                runtimeException.addSuppressed(deleteException);
            }

            throw runtimeException;
        }

        return workflowUuid;
    }

    @Override
    public void deleteConnectedUserProjects(long connectedUserId) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        Optional<ConnectedUserProject> fetchedConnectedUserProject = connectUserProjectService
            .fetchConnectUserProject(connectedUser.getExternalId(), connectedUser.getEnvironment());

        if (fetchedConnectedUserProject.isEmpty()) {
            return;
        }

        ConnectedUserProject connectedUserProject = fetchedConnectedUserProject.get();

        List<ConnectedUserProjectWorkflow> connectedUserProjectWorkflows = connectedUserProjectWorkflowService
            .getConnectedUserProjectWorkflows(connectedUserProject.getId());

        for (ConnectedUserProjectWorkflow connectedUserProjectWorkflow : connectedUserProjectWorkflows) {
            if (connectedUserProjectWorkflowRepository.existsById(connectedUserProjectWorkflow.getId())) {
                deleteProjectWorkflow(connectedUserProjectWorkflow.getId());
            }
        }

        connectUserProjectService.delete(connectedUserProject.getId());

        projectFacade.deleteProject(connectedUserProject.getProjectId());
    }

    @Override
    public void deleteProjectWorkflow(String externalUserId, String workflowUuid, Environment environment) {
        ConnectedUserProject connectedUserProject = connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            externalUserId, environment);

        projectDeploymentService.fetchProjectDeployment(connectedUserProject.getProjectId(), environment)
            .ifPresent(projectDeployment -> disableProjectDeploymentWorkflow(projectDeployment.getId(), workflowUuid));

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(
            connectedUserProject.getProjectId(), workflowUuid);

        for (ProjectWorkflow projectWorkflow : projectWorkflows) {
            connectedUserProjectWorkflowService
                .fetchConnectedUserProjectWorkflow(connectedUserProject.getId(), projectWorkflow.getId())
                .ifPresent(connectedUserProjectWorkflow -> connectedUserProjectWorkflowService
                    .delete(connectedUserProjectWorkflow.getId()));

            projectWorkflowFacade.deleteWorkflow(projectWorkflow.getWorkflowId());
        }
    }

    @Override
    @Audited(event = AutomationProjectAuditEvents.CONNECTED_USER_PROJECT_WORKFLOW_DELETED, data = {
        @AuditData(key = "connectedUserProjectWorkflowId", value = "#connectedUserProjectWorkflowId")
    })
    @PreAuthorize("isTenantAdmin()")
    public void deleteProjectWorkflow(long connectedUserProjectWorkflowId) {
        ConnectedUserProjectWorkflow connectedUserProjectWorkflow = connectedUserProjectWorkflowService
            .getConnectedUserProjectWorkflow(connectedUserProjectWorkflowId);

        ConnectedUserProject connectedUserProject = connectUserProjectService.getConnectedUserProject(
            connectedUserProjectWorkflow.getConnectedUserProjectId());

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserProject.getConnectedUserId());

        String automationUuid = connectedUserProjectWorkflow.getAutomationUuid();

        if (automationUuid != null) {
            connectedUserWorkflowReferenceFacade.deleteReference(
                connectedUser.getExternalId(), automationUuid, connectedUser.getEnvironment());

            return;
        }

        ProjectWorkflow projectWorkflow = projectWorkflowService.getProjectWorkflow(
            connectedUserProjectWorkflow.getProjectWorkflowId());

        deleteProjectWorkflow(
            connectedUser.getExternalId(), projectWorkflow.getUuidAsString(), connectedUser.getEnvironment());
    }

    @Override
    public @Nullable String enableProjectWorkflow(
        String externalUserId, String workflowUuid, boolean enable, Long environmentId) {
        Environment environment = environmentId == null
            ? Environment.PRODUCTION : environmentService.getEnvironment(environmentId);

        ConnectedUserProject connectedUserProject = connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            externalUserId, environment);

        boolean isReference = connectedUserProjectWorkflowRepository
            .findByConnectedUserProjectIdAndAutomationUuid(connectedUserProject.getId(), workflowUuid)
            .isPresent();

        if (isReference) {
            ConnectedUserProjectWorkflow reference = connectedUserWorkflowReferenceFacade.enableReference(
                externalUserId, workflowUuid, enable, environment);

            if (!enable) {
                return null;
            }

            ReferenceState referenceState = connectedUserReferenceAttentionResolver.resolve(reference);

            return referenceState.attentionReason();
        }

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

        if (enable) {
            int workflowErrorCount = connectedUserWorkflowValidator.getErrorCount(workflowId, environment);

            if (workflowErrorCount > 0) {
                throw new InvalidWorkflowException(workflowUuid, workflowErrorCount);
            }
        }

        projectDeploymentFacade.enableProjectDeploymentWorkflow(
            connectedUserProject.getProjectId(), workflowId, enable, environment);

        return null;
    }

    @Override
    @Audited(event = AutomationProjectAuditEvents.CONNECTED_USER_PROJECT_WORKFLOW_ENABLED_UPDATED, data = {
        @AuditData(key = "connectedUserProjectWorkflowId", value = "#connectedUserProjectWorkflowId"),
        @AuditData(key = "enabled", value = "#enable")
    })
    @PreAuthorize("isTenantAdmin()")
    public void enableProjectWorkflow(long connectedUserProjectWorkflowId, boolean enable) {
        ConnectedUserProjectWorkflow connectedUserProjectWorkflow = connectedUserProjectWorkflowService
            .getConnectedUserProjectWorkflow(connectedUserProjectWorkflowId);

        ConnectedUserProject connectedUserProject = connectUserProjectService.getConnectedUserProject(
            connectedUserProjectWorkflow.getConnectedUserProjectId());

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserProject.getConnectedUserId());

        String automationUuid = connectedUserProjectWorkflow.getAutomationUuid();

        if (automationUuid != null) {
            connectedUserWorkflowReferenceFacade.enableReference(
                connectedUser.getExternalId(), automationUuid, enable, connectedUser.getEnvironment());

            return;
        }

        ProjectWorkflow projectWorkflow = projectWorkflowService.getProjectWorkflow(
            connectedUserProjectWorkflow.getProjectWorkflowId());

        enableProjectWorkflow(
            connectedUser.getExternalId(), projectWorkflow.getUuidAsString(), enable,
            connectedUser.getEnvironmentId());
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<ConnectedUserAutomationProjectConfigurationDTO> getConnectedUserProjectConfigurations(
        long connectedUserId) {

        return connectedUserProjectConfigurationService.getConnectedUserAssignments(connectedUserId)
            .stream()
            .map(this::toConnectedUserAutomationProjectConfiguration)
            .sorted(Comparator.comparingLong(ConnectedUserAutomationProjectConfigurationDTO::projectId))
            .toList();
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

        WorkflowDTO workflowDTO = workflowFacade.getWorkflow(projectWorkflow.getWorkflowId());

        ConnectedUserProjectWorkflowDTO connectedUserProjectWorkflowDTO = new ConnectedUserProjectWorkflowDTO(
            connectedUserProject.getConnectedUserId(), connectedUserProjectWorkflow,
            isProjectDeploymentWorkflowEnabled(projectWorkflow, environment),
            getWorkflowLastExecutionDate(projectWorkflow), projectWorkflow, workflowDTO,
            toComponents(workflowDTO.getWorkflow()));

        Long projectDeploymentId = projectDeploymentService.fetchProjectDeployment(
            connectedUserProject.getProjectId(), environment)
            .map(ProjectDeployment::getId)
            .orElse(null);

        return connectedUserProjectWorkflowDTO.withStaticWebhookUrl(
            connectedUserWorkflowWebhookUrlResolver.resolve(projectDeploymentId, projectWorkflow.getUuidAsString()));
    }

    @Override
    public List<ConnectedUserProjectWorkflowDTO> getConnectedUserProjectWorkflows(
        String externalUserId, Environment environment) {
        ConnectedUserProject connectedUserProject = connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            externalUserId, environment);

        return getConnectedUserProjectWorkflows(connectedUserProject, environment);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<ConnectedUserProjectDTO> getConnectedUserProjects(Long connectedUserId, Environment environment) {
        return connectUserProjectService.getConnectedUserProjects(connectedUserId, environment)
            .stream()
            .map(connectedUserProject -> Map.entry(
                connectedUserProject, connectedUserService.getConnectedUser(connectedUserProject.getConnectedUserId())))
            .map(entry -> {
                ConnectedUserProject connectedUserProject = entry.getKey();
                ConnectedUser connectedUser = entry.getValue();

                List<ConnectedUserProjectWorkflowDTO> connectedUserProjectWorkflows =
                    getConnectedUserProjectWorkflows(connectedUserProject, connectedUser.getEnvironment());

                ProjectDeployment projectDeployment = projectDeploymentService
                    .fetchProjectDeployment(connectedUserProject.getProjectId(), connectedUser.getEnvironment())
                    .orElse(null);

                return new ConnectedUserProjectDTO(
                    connectedUserProject, connectedUser, connectedUserProjectWorkflows,
                    projectDeployment == null ? null : getProjectDeploymentLastExecutionDate(projectDeployment.getId()),
                    projectDeployment);
            })
            .toList();
    }

    @Override
    public List<ConnectedUserProjectDTO> getConnectedUserProjects(String externalUserId, Environment environment) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        return getConnectedUserProjects(connectedUser.getId(), environment);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Transactional(propagation = Propagation.SUPPORTS)
    public ConnectedUserWorkflowReferenceValuesDTO getProjectWorkflowReferenceValues(
        long connectedUserProjectWorkflowId) {

        return connectedUserWorkflowReferenceFacade.getReferenceValues(connectedUserProjectWorkflowId);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<ProvisionableAutomationProjectDTO> getProvisionableProjects(long connectedUserId) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        Map<String, ConnectedUserProjectWorkflow> referencesByWorkflowUuid = connectedUserWorkflowReferenceFacade
            .getConnectedUserWorkflows(connectedUserId)
            .stream()
            .collect(
                Collectors.toMap(
                    ConnectedUserProjectWorkflow::getAutomationUuid, Function.identity(),
                    (firstReference, secondReference) -> firstReference));

        Map<Long, List<AutomationProjectConfigurationReleaseDTO>> releasesByProjectId =
            automationProjectFacade
                .getPublishedProjectConfigurations(connectedUser.getExternalId(), connectedUser.getEnvironment())
                .stream()
                .collect(
                    Collectors.groupingBy(
                        release -> release.project()
                            .id(),
                        LinkedHashMap::new, Collectors.toList()));

        return releasesByProjectId.values()
            .stream()
            .map(releases -> toProvisionableProject(connectedUser, releases, referencesByWorkflowUuid))
            .sorted(
                Comparator.comparing(ProvisionableAutomationProjectDTO::name, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    @Override
    public CopilotChatContextDTO prepareCopilotChat(
        String externalUserId, String workflowUuid, Environment environment) {
        ConnectedUserProjectWorkflowDTO connectedUserProjectWorkflowDTO = getConnectedUserProjectWorkflow(
            externalUserId, workflowUuid, (long) environment.ordinal());

        String workflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);

        Set<String> allowedComponentNames = resolveAllowedComponentNames(environment);

        return new CopilotChatContextDTO(
            workflowId, connectedUserProjectWorkflowDTO.projectId(), allowedComponentNames);
    }

    @Override
    @Audited(event = AutomationProjectAuditEvents.CONNECTED_USER_PROJECT_WORKFLOWS_PROVISIONED, data = {
        @AuditData(key = "connectedUserId", value = "#connectedUserId"),
        @AuditData(key = "configurationId", value = "#configurationId"),
        @AuditData(key = "workflowUuids", value = "#workflows.![workflowUuid]")
    })
    @PreAuthorize("isTenantAdmin()")
    @Transactional(propagation = Propagation.SUPPORTS)
    public void provisionProjectWorkflows(
        long connectedUserId, long configurationId, List<ProvisionConnectedUserProjectWorkflowDTO> workflows) {

        if (workflows.isEmpty()) {
            throw new ConfigurationException(
                "No workflows to provision for connected user %s on configuration %s".formatted(
                    connectedUserId, configurationId),
                ConnectedUserProjectWorkflowErrorType.WORKFLOW_NOT_PROVISIONABLE);
        }

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        transactionTemplate.executeWithoutResult(
            transactionStatus -> assignConfiguration(connectedUser, configurationId, workflows));

        for (ProvisionConnectedUserProjectWorkflowDTO workflow : workflows) {
            Map<String, ?> inputs = workflow.inputs();

            try {
                connectedUserWorkflowReferenceFacade.getOrCreateReference(
                    connectedUser.getExternalId(), workflow.workflowUuid(), connectedUser.getEnvironment(),
                    workflow.connections(), inputs == null || inputs.isEmpty() ? null : inputs);
            } catch (MissingConnectionException missingConnectionException) {
                if (log.isDebugEnabled()) {
                    log.debug(
                        "Workflow {} provisioned for connected user {} without the {} connection",
                        workflow.workflowUuid(), connectedUserId, missingConnectionException.getComponentName());
                }
            }
        }
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

        Set<Long> entitledConnectionIds = connectedUserProjectWorkflowManager.getEntitledConnectionIds(
            connectedUserProject.getConnectedUserId(), environment);

        List<ProjectDeploymentWorkflowConnection> connections = workflowTestConfigurationService
            .fetchWorkflowTestConfiguration(workflowId, environment.ordinal())
            .map(WorkflowTestConfiguration::getConnections)
            .orElse(List.of())
            .stream()
            .filter(workflowTestConfigurationConnection -> entitledConnectionIds.contains(
                workflowTestConfigurationConnection.getConnectionId()))
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

            projectDeployment.setName(connectedUser.getExternalId());

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
    public void updateProjectWorkflowInputs(
        String externalUserId, String workflowUuid, Map<String, ?> inputs, Long environmentId) {
        Environment environment = environmentId == null
            ? Environment.PRODUCTION : environmentService.getEnvironment(environmentId);

        ConnectedUserProject connectedUserProject = connectedUserProjectWorkflowManager.getOrCreateConnectedUserProject(
            externalUserId, environment);

        Optional<ConnectedUserProjectWorkflow> reference = connectedUserProjectWorkflowRepository
            .findByConnectedUserProjectIdAndAutomationUuid(connectedUserProject.getId(), workflowUuid);

        if (reference.isPresent()) {
            connectedUserWorkflowReferenceFacade.updateReferenceInputs(
                externalUserId, workflowUuid, inputs, environment);

            return;
        }

        long projectDeploymentId = projectDeploymentService.getProjectDeploymentId(
            connectedUserProject.getProjectId(), environment);

        String workflowId = projectWorkflowService
            .fetchProjectWorkflowWorkflowId(projectDeploymentId, workflowUuid)
            .orElseThrow(() -> new ConfigurationException(
                "Workflow with workflowUuid: %s not exist".formatted(workflowUuid),
                WorkflowErrorType.WORKFLOW_NOT_FOUND));

        ProjectDeploymentWorkflow projectDeploymentWorkflow =
            projectDeploymentWorkflowService.getProjectDeploymentWorkflow(projectDeploymentId, workflowId);

        projectDeploymentWorkflow.setInputs(inputs);

        projectDeploymentWorkflowService.update(projectDeploymentWorkflow);
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
        String externalUserId, String workflowUuid, String prompt, @Nullable String systemPrompt,
        Environment environment, boolean generate) {
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

        copilotWorkflowGenerator.generateWorkflow(
            projectWorkflow.getWorkflowId(), connectedUserProject.getProjectId(), environment.ordinal(), prompt,
            systemPrompt, allowedComponentNames);

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

    private void assignConfiguration(
        ConnectedUser connectedUser, long configurationId, List<ProvisionConnectedUserProjectWorkflowDTO> workflows) {

        AutomationProjectConfiguration configuration =
            automationProjectConfigurationService.getConfiguration(configurationId);

        long projectId = configuration.getProjectId();

        ConnectedUserProject connectedUserProject = connectedUserProjectWorkflowManager
            .getOrCreateConnectedUserProject(connectedUser.getExternalId(), connectedUser.getEnvironment());

        connectUserProjectService.lockConnectedUserProject(Objects.requireNonNull(connectedUserProject.getId()));

        ProvisionableAutomationProjectDTO.Configuration provisionableConfiguration =
            getProvisionableConfiguration(connectedUser, projectId, configurationId);

        Optional<ConnectedUserProjectConfiguration> assignmentOptional =
            connectedUserProjectConfigurationService.fetchAssignment(connectedUser.getId(), projectId);

        if (assignmentOptional.isPresent()) {
            ConnectedUserProjectConfiguration assignment = assignmentOptional.get();

            if (!Objects.equals(assignment.getConfigurationId(), configurationId)) {
                throw new ConfigurationException(
                    ("Connected user id=%s is assigned to configuration id=%s of project id=%s; move the connected " +
                        "user to configuration id=%s instead").formatted(
                            connectedUser.getId(), assignment.getConfigurationId(), projectId, configurationId),
                    AutomationProjectConfigurationErrorType.CONFIGURATION_PROJECT_MISMATCH);
            }
        } else {
            automationProjectConfigurationResolver.assign(connectedUser, projectId, configurationId);
        }

        Set<String> provisionableWorkflowUuids = provisionableConfiguration.workflows()
            .stream()
            .map(ProvisionableAutomationProjectDTO.Workflow::workflowUuid)
            .collect(Collectors.toSet());

        for (ProvisionConnectedUserProjectWorkflowDTO workflow : workflows) {
            if (!provisionableWorkflowUuids.contains(workflow.workflowUuid())) {
                throw new ConfigurationException(
                    "Workflow %s cannot be provisioned for connected user %s on configuration %s".formatted(
                        workflow.workflowUuid(), connectedUser.getId(), configurationId),
                    ConnectedUserProjectWorkflowErrorType.WORKFLOW_NOT_PROVISIONABLE);
            }
        }
    }

    private ProvisionableAutomationProjectDTO.Configuration getProvisionableConfiguration(
        ConnectedUser connectedUser, long projectId, long configurationId) {

        return getProvisionableProjects(connectedUser.getId()).stream()
            .filter(project -> project.projectId() == projectId)
            .flatMap(project -> project.configurations()
                .stream())
            .filter(configuration -> configuration.id() == configurationId)
            .findFirst()
            .orElseThrow(() -> new ConfigurationException(
                ("Configuration id=%s of project id=%s cannot be provisioned for connected user id=%s: it is " +
                    "disabled, in another environment, or the project is not released to the connected user")
                        .formatted(configurationId, projectId, connectedUser.getId()),
                AutomationProjectConfigurationErrorType.CONFIGURATION_PROJECT_MISMATCH));
    }

    private ConnectedUserAutomationProjectConfigurationDTO toConnectedUserAutomationProjectConfiguration(
        ConnectedUserProjectConfiguration assignment) {

        AutomationProjectConfiguration configuration =
            automationProjectConfigurationService.getConfiguration(assignment.getConfigurationId());

        return new ConnectedUserAutomationProjectConfigurationDTO(
            assignment.getProjectId(), configuration.getId(), configuration.getName(),
            configuration.getProjectVersion(), configuration.isEnabled(), assignment.isEnabled());
    }

    private ProvisionableAutomationProjectDTO toProvisionableProject(
        ConnectedUser connectedUser, List<AutomationProjectConfigurationReleaseDTO> releases,
        Map<String, ConnectedUserProjectWorkflow> referencesByWorkflowUuid) {

        AutomationProjectDTO project = releases.getFirst()
            .project();

        Long assignedConfigurationId = connectedUserProjectConfigurationService
            .fetchAssignment(connectedUser.getId(), project.id())
            .map(ConnectedUserProjectConfiguration::getConfigurationId)
            .orElse(null);

        Long selfServeConfigurationId = automationProjectConfigurationResolver
            .resolve(connectedUser, project.id())
            .map(AutomationProjectConfiguration::getId)
            .orElse(null);

        List<ProvisionableAutomationProjectDTO.Configuration> configurations = releases.stream()
            .map(release -> toProvisionableConfiguration(release, selfServeConfigurationId, referencesByWorkflowUuid))
            .toList();

        ProvisionableAutomationProjectDTO.Configuration preselectedConfiguration = findConfiguration(
            configurations, assignedConfigurationId)
                .or(() -> findConfiguration(configurations, selfServeConfigurationId))
                .orElse(configurations.getFirst());

        return new ProvisionableAutomationProjectDTO(
            project.id(), project.name(), preselectedConfiguration.projectVersion(),
            preselectedConfiguration.workflows(), configurations, assignedConfigurationId);
    }

    private static Optional<ProvisionableAutomationProjectDTO.Configuration> findConfiguration(
        List<ProvisionableAutomationProjectDTO.Configuration> configurations,
        @Nullable Long configurationId) {

        return configurations.stream()
            .filter(configuration -> Objects.equals(configuration.id(), configurationId))
            .findFirst();
    }

    private static ProvisionableAutomationProjectDTO.Configuration toProvisionableConfiguration(
        AutomationProjectConfigurationReleaseDTO release, @Nullable Long selfServeConfigurationId,
        Map<String, ConnectedUserProjectWorkflow> referencesByWorkflowUuid) {

        AutomationProjectDTO project = release.project();

        List<ProvisionableAutomationProjectDTO.Workflow> workflows = project.workflowTemplates()
            .stream()
            .map(template -> toProvisionableWorkflow(template, referencesByWorkflowUuid.get(template.workflowUuid())))
            .toList();

        return new ProvisionableAutomationProjectDTO.Configuration(
            release.configurationId(), release.configurationName(),
            Objects.requireNonNull(project.lastPublishedVersion()),
            Objects.equals(release.configurationId(), selfServeConfigurationId), workflows);
    }

    private static ProvisionableAutomationProjectDTO.Workflow toProvisionableWorkflow(
        ConnectedUserWorkflowTemplateDTO template, @Nullable ConnectedUserProjectWorkflow reference) {

        List<ConnectedUserWorkflowTemplateDTO.Input> inputs = template.inputs()
            .stream()
            .filter(input -> !input.provided())
            .toList();

        return new ProvisionableAutomationProjectDTO.Workflow(
            template.workflowUuid(), template.label(), inputs, reference != null,
            reference != null && reference.isDangling(), getConnectionComponents(template));
    }

    private static List<ConnectedUserWorkflowTemplateDTO.Component> getConnectionComponents(
        ConnectedUserWorkflowTemplateDTO template) {

        Map<String, ConnectedUserWorkflowTemplateDTO.Component> componentsByName = new LinkedHashMap<>();

        for (ConnectedUserWorkflowTemplateDTO.Component component : CollectionUtils.concat(
            template.triggers(), template.components())) {

            if (component.connectionRequired() && !component.provided()) {
                componentsByName.putIfAbsent(component.name(), component);
            }
        }

        return List.copyOf(componentsByName.values());
    }

    private List<ConnectedUserProjectWorkflowDTO> getConnectedUserProjectWorkflows(
        ConnectedUserProject connectedUserProject, Environment environment) {
        Project project = projectService.getProject(connectedUserProject.getProjectId());

        List<ProjectWorkflow> latestProjectWorkflows = projectWorkflowService.getProjectWorkflows(
            project.getId(), project.getLastProjectVersion());

        List<String> latestWorkflowIds = latestProjectWorkflows.stream()
            .map(ProjectWorkflow::getWorkflowId)
            .toList();

        Map<UUID, Workflow> publishedWorkflows = getPublishedWorkflows(project);

        Long projectDeploymentId = projectDeploymentService.fetchProjectDeployment(project.getId(), environment)
            .map(ProjectDeployment::getId)
            .orElse(null);

        List<ConnectedUserProjectWorkflowDTO> copies = workflowService.getWorkflows(latestWorkflowIds)
            .stream()
            .map(workflow -> {
                ProjectWorkflow latestProjectWorkflow = latestProjectWorkflows.stream()
                    .filter(curProjectWorkflow -> Objects.equals(curProjectWorkflow.getWorkflowId(), workflow.getId()))
                    .findFirst()
                    .orElseThrow();

                ConnectedUserProjectWorkflow connectedUserProjectWorkflow = connectedUserProjectWorkflowService
                    .getConnectedUserProjectWorkflow(connectedUserProject.getId(), latestProjectWorkflow.getId());

                UUID projectWorkflowUuid = latestProjectWorkflow.getUuid();

                Workflow publishedWorkflow = projectWorkflowUuid == null
                    ? workflow : publishedWorkflows.getOrDefault(projectWorkflowUuid, workflow);

                ConnectedUserProjectWorkflowDTO connectedUserProjectWorkflowDTO = new ConnectedUserProjectWorkflowDTO(
                    connectedUserProject.getConnectedUserId(), connectedUserProjectWorkflow,
                    isProjectDeploymentWorkflowEnabled(latestProjectWorkflow, environment),
                    getWorkflowLastExecutionDate(latestProjectWorkflow), latestProjectWorkflow,
                    new WorkflowDTO(publishedWorkflow, List.of(), List.of()), toComponents(publishedWorkflow),
                    toInputs(workflow),
                    fetchInputValues(connectedUserProject.getProjectId(), latestProjectWorkflow, environment));

                return connectedUserProjectWorkflowDTO
                    .withAttentionReason(
                        getCopyAttentionReason(projectDeploymentId, latestProjectWorkflow.getUuidAsString(),
                            environment))
                    .withStaticWebhookUrl(
                        connectedUserWorkflowWebhookUrlResolver.resolve(
                            projectDeploymentId, latestProjectWorkflow.getUuidAsString()));
            })
            .toList();

        List<ConnectedUserProjectWorkflowDTO> references = getReferenceRows(connectedUserProject, environment);

        return Stream.concat(copies.stream(), references.stream())
            .toList();
    }

    private @Nullable String getCopyAttentionReason(
        @Nullable Long projectDeploymentId, @Nullable String workflowUuid, Environment environment) {

        if (projectDeploymentId == null || workflowUuid == null) {
            return null;
        }

        return projectWorkflowService.fetchProjectWorkflowWorkflowId(projectDeploymentId, workflowUuid)
            .map(workflowId -> connectedUserWorkflowValidator.getAttentionReason(workflowId, environment))
            .orElse(null);
    }

    private void checkWorkflowTemplateReleased(ConnectedUser connectedUser, String workflowUuid) {
        Optional<AutomationProjectDTO> publishedProjectOptional = automationProjectFacade
            .getPublishedProjects()
            .stream()
            .filter(project -> CollectionUtils.stream(project.workflowTemplates())
                .anyMatch(workflowTemplate -> Objects.equals(workflowTemplate.workflowUuid(), workflowUuid)))
            .findFirst();

        if (publishedProjectOptional.isEmpty()) {
            return;
        }

        long projectId = publishedProjectOptional.get()
            .id();

        Optional<AutomationProjectConfiguration> configurationOptional =
            automationProjectConfigurationResolver.resolve(connectedUser, projectId);

        boolean released = false;

        if (configurationOptional.isPresent() && configurationOptional.get()
            .isEnabled()) {

            AutomationProjectConfiguration configuration = configurationOptional.get();

            UUID uuid = UUID.fromString(workflowUuid);

            released = automationProjectConfigurationService.isWorkflowEnabled(configuration, uuid) &&
                projectWorkflowService.getProjectWorkflows(projectId, configuration.getProjectVersion())
                    .stream()
                    .anyMatch(projectWorkflow -> uuid.equals(projectWorkflow.getUuid()));
        }

        if (!released) {
            throw newWorkflowTemplateNotReleasedException(workflowUuid);
        }
    }

    private String getPinnedWorkflowId(long projectId, int pinnedVersion, String workflowUuid) {
        return projectWorkflowService.getProjectWorkflows(projectId, pinnedVersion)
            .stream()
            .filter(projectWorkflow -> Objects.equals(projectWorkflow.getUuidAsString(), workflowUuid))
            .findFirst()
            .map(ProjectWorkflow::getWorkflowId)
            .orElseThrow(() -> newWorkflowTemplateNotReleasedException(workflowUuid));
    }

    private static ConfigurationException newWorkflowTemplateNotReleasedException(String workflowUuid) {
        return new ConfigurationException(
            "Automation workflow template %s is not released".formatted(workflowUuid),
            ConnectedUserProjectWorkflowErrorType.WORKFLOW_TEMPLATE_NOT_RELEASED);
    }

    private Map<UUID, Workflow> getPublishedWorkflows(Project project) {
        ProjectVersion lastPublishedProjectVersion = project.getLastPublishedProjectVersion();

        if (lastPublishedProjectVersion == null) {
            return Map.of();
        }

        List<ProjectWorkflow> publishedProjectWorkflows = projectWorkflowService.getProjectWorkflows(
            project.getId(), lastPublishedProjectVersion.getVersion());

        Map<String, UUID> uuids = publishedProjectWorkflows.stream()
            .filter(projectWorkflow -> projectWorkflow.getUuid() != null)
            .collect(Collectors.toMap(
                ProjectWorkflow::getWorkflowId, ProjectWorkflow::getUuid, (first, second) -> first));

        List<String> publishedWorkflowIds = publishedProjectWorkflows.stream()
            .map(ProjectWorkflow::getWorkflowId)
            .toList();

        return workflowService.getWorkflows(publishedWorkflowIds)
            .stream()
            .filter(workflow -> uuids.containsKey(workflow.getId()))
            .collect(Collectors.toMap(
                workflow -> uuids.get(workflow.getId()), Function.identity(), (first, second) -> first));
    }

    private List<ConnectedUserProjectWorkflowDTO> getReferenceRows(
        ConnectedUserProject connectedUserProject, Environment environment) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserProject.getConnectedUserId());

        Map<String, ConnectedUserWorkflowTemplateDTO> templatesByUuid = automationProjectFacade
            .getPublishedProjects(connectedUser.getExternalId(), environment)
            .stream()
            .flatMap(project -> project.workflowTemplates()
                .stream())
            .collect(Collectors.toMap(
                ConnectedUserWorkflowTemplateDTO::workflowUuid, Function.identity(), (first, second) -> first));

        return connectedUserWorkflowReferenceFacade.getConnectedUserWorkflows(connectedUser.getId())
            .stream()
            .filter(connectedUserProjectWorkflow -> connectedUserProjectWorkflow.getAutomationUuid() != null)
            .map(reference -> {
                ConnectedUserWorkflowTemplateDTO template = Optional
                    .ofNullable(templatesByUuid.get(reference.getAutomationUuid()))
                    .or(() -> fetchReferenceDeploymentTemplate(connectedUser.getId(), reference))
                    .orElse(null);

                String label = template == null ? reference.getAutomationUuid() : template.label();
                String description = template == null ? "" : Objects.requireNonNullElse(template.description(), "");

                Workflow workflow = new Workflow(
                    reference.getAutomationUuid(),
                    JsonUtils.write(Map.of("label", label, "description", description)), Workflow.Format.JSON);

                ReferenceState referenceState = connectedUserReferenceAttentionResolver.resolve(reference);

                List<ConnectedUserWorkflowTemplateDTO.Input> inputs = template == null ? List.of() : template.inputs();
                List<ConnectedUserWorkflowTemplateDTO.Component> components =
                    template == null ? List.of() : template.components();
                List<ConnectedUserWorkflowTemplateDTO.Component> connectionComponents = template == null
                    ? List.of()
                    : CollectionUtils.concat(
                        Objects.requireNonNullElse(template.triggers(), List.of()),
                        Objects.requireNonNullElse(template.components(), List.of()));

                ConnectedUserProjectWorkflowDTO connectedUserProjectWorkflowDTO =
                    ConnectedUserProjectWorkflowDTO.ofReference(
                        connectedUser.getId(), reference,
                        new WorkflowDTO(workflow, List.of(), getReferenceWorkflowTriggers(reference)), components,
                        inputs,
                        withoutProvidedInputValues(referenceState.inputs(), inputs),
                        withoutProvidedConnections(referenceState.connections(), connectionComponents),
                        referenceState.attentionReason());

                connectedUserProjectWorkflowDTO = connectedUserProjectWorkflowDTO.withLastExecutionDate(
                    getReferenceLastExecutionDate(reference));

                return connectedUserProjectWorkflowDTO.withStaticWebhookUrl(
                    reference.isDangling() ? null : connectedUserWorkflowWebhookUrlResolver.resolve(
                        reference.getProjectDeploymentId(), reference.getAutomationUuid()));
            })
            .toList();
    }

    @Nullable
    private Instant getReferenceLastExecutionDate(ConnectedUserProjectWorkflow reference) {
        Long projectDeploymentId = reference.getProjectDeploymentId();

        if (projectDeploymentId == null) {
            return null;
        }

        Optional<ProjectDeployment> projectDeploymentOptional = projectDeploymentService.fetchProjectDeployment(
            projectDeploymentId);

        if (projectDeploymentOptional.isEmpty()) {
            return null;
        }

        ProjectDeployment projectDeployment = projectDeploymentOptional.get();

        List<String> workflowIds = projectWorkflowService
            .getProjectWorkflows(projectDeployment.getProjectId(), reference.getAutomationUuid())
            .stream()
            .map(ProjectWorkflow::getWorkflowId)
            .toList();

        if (workflowIds.isEmpty()) {
            return null;
        }

        return OptionalUtils.mapOrElse(
            principalJobService.fetchLastWorkflowJobId(projectDeploymentId, workflowIds, PlatformType.AUTOMATION),
            this::getJobEndDate, null);
    }

    private List<WorkflowTriggerDTO> getReferenceWorkflowTriggers(ConnectedUserProjectWorkflow reference) {
        Long projectDeploymentId = reference.getProjectDeploymentId();

        if (reference.isDangling() || projectDeploymentId == null) {
            return List.of();
        }

        return projectWorkflowService.fetchProjectWorkflowWorkflowId(projectDeploymentId, reference.getAutomationUuid())
            .map(workflowFacade::getWorkflow)
            .map(WorkflowDTO::getTriggers)
            .orElse(List.of());
    }

    private Optional<ConnectedUserWorkflowTemplateDTO> fetchReferenceDeploymentTemplate(
        long connectedUserId, ConnectedUserProjectWorkflow reference) {

        Long projectDeploymentId = reference.getProjectDeploymentId();

        if (reference.isDangling() || projectDeploymentId == null) {
            return Optional.empty();
        }

        String automationUuid = reference.getAutomationUuid();

        Optional<ProjectDeployment> projectDeploymentOptional = connectedUserReferenceDeploymentManager
            .fetchDeployment(projectDeploymentId);

        if (projectDeploymentOptional.isEmpty()) {
            return Optional.empty();
        }

        ProjectDeployment projectDeployment = projectDeploymentOptional.get();

        ProvidedNames providedNames = connectedUserReferenceDeploymentManager.getProvidedNames(
            connectedUserId, projectDeployment.getProjectId(), automationUuid);

        return projectWorkflowService.fetchProjectWorkflowWorkflowId(projectDeploymentId, automationUuid)
            .map(workflowService::getWorkflow)
            .map(workflow -> new ConnectedUserWorkflowTemplateDTO(
                automationUuid, workflow.getLabel(), workflow.getDescription(), null,
                workflowComponentResolver.getTriggerComponents(workflow),
                workflowComponentResolver.getTaskComponents(workflow), toInputs(workflow), null))
            .map(template -> template.withProvided(providedNames.componentNames(), providedNames.inputNames()));
    }

    private static Map<String, Long> withoutProvidedConnections(
        Map<String, Long> connections, List<ConnectedUserWorkflowTemplateDTO.Component> components) {

        Map<String, Long> userConnections = new LinkedHashMap<>(connections);

        for (ConnectedUserWorkflowTemplateDTO.Component component : components) {
            if (component.provided()) {
                userConnections.remove(component.name());
            }
        }

        return userConnections;
    }

    private static Map<String, ?> withoutProvidedInputValues(
        Map<String, ?> inputValues, List<ConnectedUserWorkflowTemplateDTO.Input> inputs) {

        Map<String, Object> userInputValues = new LinkedHashMap<>(inputValues);

        for (ConnectedUserWorkflowTemplateDTO.Input input : inputs) {
            if (input.provided()) {
                userInputValues.remove(input.name());
            }
        }

        return userInputValues;
    }

    private static List<ConnectedUserWorkflowTemplateDTO.Input> toInputs(Workflow workflow) {
        List<Workflow.Input> inputs = workflow.getInputs();

        return inputs.stream()
            .map(input -> new ConnectedUserWorkflowTemplateDTO.Input(
                input.name(), input.label(), input.type(), input.required()))
            .toList();
    }

    private void disableProjectDeploymentWorkflow(long projectDeploymentId, String workflowUuid) {
        projectWorkflowService.fetchProjectWorkflowWorkflowId(projectDeploymentId, workflowUuid)
            .filter(workflowId -> projectDeploymentWorkflowService.isProjectDeploymentWorkflowEnabled(
                projectDeploymentId, workflowId))
            .ifPresent(workflowId -> projectDeploymentFacade.enableProjectDeploymentWorkflow(
                projectDeploymentId, workflowId, false));
    }

    private Map<String, ?> fetchInputValues(long projectId, ProjectWorkflow projectWorkflow, Environment environment) {
        String workflowUuid = projectWorkflow.getUuidAsString();

        if (workflowUuid == null) {
            return Map.of();
        }

        return projectDeploymentService.fetchProjectDeployment(projectId, environment)
            .flatMap(projectDeployment -> projectWorkflowService
                .fetchProjectWorkflowWorkflowId(projectDeployment.getId(), workflowUuid)
                .flatMap(workflowId -> projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(
                    projectDeployment.getId(), workflowId)))
            .map(ProjectDeploymentWorkflow::getInputs)
            .orElseGet(Map::of);
    }

    private List<ConnectedUserWorkflowTemplateDTO.Component> toComponents(Workflow workflow) {
        return workflowComponentResolver.getComponents(workflow);
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

    private Instant getWorkflowLastExecutionDate(ProjectWorkflow projectWorkflow) {
        List<String> workflowIds = projectWorkflowService
            .getProjectWorkflows(projectWorkflow.getProjectId(), projectWorkflow.getUuidAsString())
            .stream()
            .map(ProjectWorkflow::getWorkflowId)
            .toList();

        return OptionalUtils.mapOrElse(jobService.fetchLastWorkflowJob(workflowIds), Job::getEndDate, null);
    }

    private boolean isProjectDeploymentWorkflowEnabled(ProjectWorkflow projectWorkflow, Environment environment) {
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

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.security.SkipAutomationAuthorization;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectVersionWorkflowChangeService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.embedded.configuration.audit.AutomationProjectAuditEvents;
import com.bytechef.ee.embedded.configuration.audit.WorkflowAuditEvents;
import com.bytechef.ee.embedded.configuration.domain.AutomationProject;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectCategoryDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectConfigurationReleaseDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectTagDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationProjectVersionDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceDeploymentManager.ProvidedNames;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.dto.VersionWorkflowChange;
import com.bytechef.platform.configuration.dto.VersionWorkflowChanges;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.configuration.workflow.WorkflowPreDeleteListener;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class AutomationProjectFacadeImpl implements AutomationProjectFacade {
    private static final String DEFAULT_DEFINITION = """
        {
            "label": "New Workflow",
            "description": "",
            "inputs": [],
            "triggers": [],
            "tasks": []
        }
        """;

    private final AutomationProjectConfigurationResolver automationProjectConfigurationResolver;
    private final AutomationProjectConfigurationService automationProjectConfigurationService;
    private final AutomationProjectService automationProjectService;
    private final CategoryService categoryService;
    private final ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager;
    private final ConnectedUserReferenceRolloutManager connectedUserReferenceRolloutManager;
    private final ConnectedUserService connectedUserService;
    private final EmbeddedPermissionEvaluator embeddedPermissionEvaluator;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationService integrationService;
    private final ProjectService projectService;
    private final ProjectVersionWorkflowChangeService projectVersionWorkflowChangeService;
    private final ProjectWorkflowFacade projectWorkflowFacade;
    private final ProjectWorkflowService projectWorkflowService;
    private final TagService tagService;
    private final WorkflowComponentResolver workflowComponentResolver;
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService;
    private final List<WorkflowPreDeleteListener> workflowPreDeleteListeners;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public AutomationProjectFacadeImpl(
        AutomationProjectConfigurationResolver automationProjectConfigurationResolver,
        AutomationProjectConfigurationService automationProjectConfigurationService,
        AutomationProjectService automationProjectService, CategoryService categoryService,
        ConnectedUserReferenceDeploymentManager connectedUserReferenceDeploymentManager,
        ConnectedUserReferenceRolloutManager connectedUserReferenceRolloutManager,
        ConnectedUserService connectedUserService, EmbeddedPermissionEvaluator embeddedPermissionEvaluator,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationService integrationService, ProjectService projectService,
        ProjectVersionWorkflowChangeService projectVersionWorkflowChangeService,
        ProjectWorkflowFacade projectWorkflowFacade, ProjectWorkflowService projectWorkflowService,
        TagService tagService, WorkflowComponentResolver workflowComponentResolver,
        WorkflowNodeTestOutputService workflowNodeTestOutputService, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService,
        List<WorkflowPreDeleteListener> workflowPreDeleteListeners) {
        this.automationProjectConfigurationResolver = automationProjectConfigurationResolver;
        this.automationProjectConfigurationService = automationProjectConfigurationService;
        this.automationProjectService = automationProjectService;
        this.categoryService = categoryService;
        this.connectedUserReferenceDeploymentManager = connectedUserReferenceDeploymentManager;
        this.connectedUserReferenceRolloutManager = connectedUserReferenceRolloutManager;
        this.connectedUserService = connectedUserService;
        this.embeddedPermissionEvaluator = embeddedPermissionEvaluator;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationService = integrationService;
        this.projectService = projectService;
        this.projectVersionWorkflowChangeService = projectVersionWorkflowChangeService;
        this.projectWorkflowFacade = projectWorkflowFacade;
        this.projectWorkflowService = projectWorkflowService;
        this.tagService = tagService;
        this.workflowComponentResolver = workflowComponentResolver;
        this.workflowNodeTestOutputService = workflowNodeTestOutputService;
        this.workflowService = workflowService;
        this.workflowPreDeleteListeners = workflowPreDeleteListeners;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Audited(event = AutomationProjectAuditEvents.AUTOMATION_PROJECT_CREATED, data = {
        @AuditData(key = "projectId", value = "#result"),
        @AuditData(key = "name", value = "#name"),
        @AuditData(key = "category", value = "#category"),
        @AuditData(key = "permissionExpression", value = "#permissionExpression")
    })
    public long createProject(
        String name, String description, String category, List<String> tags, String permissionExpression) {
        Project project = new Project();

        project.setName(name);
        project.setDescription(description);
        project.setWorkspaceId(Workspace.DEFAULT_WORKSPACE_ID);
        project.setCategoryId(resolveCategory(category));
        project.setTagIds(resolveTags(tags));

        project = projectService.create(project);

        automationProjectService.create(
            project.getId(), normalizePermissionExpression(permissionExpression));

        return project.getId();
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Audited(event = AutomationProjectAuditEvents.AUTOMATION_CREATED, data = {
        @AuditData(key = "projectId", value = "#projectId"),
        @AuditData(key = "workflowUuid", value = "#result"),
        @AuditData(key = "permissionExpression", value = "#permissionExpression")
    })
    public String createProjectWorkflow(long projectId, String definition, String permissionExpression) {
        getProjectOfAutomationProject(projectId);

        ProjectWorkflow projectWorkflow = projectWorkflowFacade.addWorkflow(
            projectId, StringUtils.isEmpty(definition) ? DEFAULT_DEFINITION : definition);

        String normalized = normalizePermissionExpression(permissionExpression);

        if (normalized != null) {
            automationProjectService.updateWorkflowPermissionExpression(
                projectId, projectWorkflow.getUuid(), normalized);
        }

        return projectWorkflow.getUuidAsString();
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Audited(event = AutomationProjectAuditEvents.AUTOMATION_PROJECT_DELETED, data = {
        @AuditData(key = "projectId", value = "#projectId")
    })
    public void deleteProject(long projectId) {
        getProjectOfAutomationProject(projectId);

        connectedUserReferenceRolloutManager.deleteReferenceDeployments(projectId);

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(projectId);

        for (ProjectWorkflow projectWorkflow : projectWorkflows) {
            for (WorkflowPreDeleteListener workflowPreDeleteListener : workflowPreDeleteListeners) {
                workflowPreDeleteListener.onWorkflowPreDelete(projectWorkflow.getWorkflowId());
            }
        }

        projectWorkflowService.delete(
            projectWorkflows.stream()
                .map(ProjectWorkflow::getId)
                .toList());

        workflowService.delete(
            projectWorkflows.stream()
                .map(ProjectWorkflow::getWorkflowId)
                .toList());

        workflowTestConfigurationService.delete(
            projectWorkflows.stream()
                .map(ProjectWorkflow::getWorkflowId)
                .toList());

        automationProjectService.delete(projectId);

        projectService.delete(projectId);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Audited(event = AutomationProjectAuditEvents.AUTOMATION_DELETED, data = {
        @AuditData(key = "workflowUuid", value = "#workflowUuid")
    })
    public void deleteProjectWorkflow(String workflowUuid) {
        String lastWorkflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);

        ProjectWorkflow lastProjectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(lastWorkflowId);

        Project project = getProjectOfAutomationProject(lastProjectWorkflow.getProjectId());

        projectWorkflowService.fetchProjectWorkflow(project.getId(), project.getLastProjectVersion(), workflowUuid)
            .ifPresent(draftProjectWorkflow -> projectWorkflowFacade.deleteWorkflow(
                draftProjectWorkflow.getWorkflowId()));
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Audited(event = AutomationProjectAuditEvents.AUTOMATION_DUPLICATED, data = {
        @AuditData(key = "sourceWorkflowUuid", value = "#workflowUuid"),
        @AuditData(key = "workflowUuid", value = "#result")
    })
    public String duplicateProjectWorkflow(String workflowUuid) {
        String workflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);

        ProjectWorkflow sourceProjectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        long projectId = sourceProjectWorkflow.getProjectId();

        getProjectOfAutomationProject(projectId);

        Workflow sourceWorkflow = workflowService.getWorkflow(workflowId);

        String sourceDefinition = sourceWorkflow.getDefinition();

        Map<String, Object> definitionMap = JsonUtils.read(sourceDefinition, new TypeReference<>() {});

        definitionMap.compute("label", (k, label) -> (label == null ? "Workflow" : label.toString()) + " (Copy)");

        String newDefinition = JsonUtils.writeWithDefaultPrettyPrinter(definitionMap);

        ProjectWorkflow newProjectWorkflow = projectWorkflowFacade.addWorkflow(projectId, newDefinition);

        workflowTestConfigurationService.copyWorkflowTestConfigurations(
            workflowId, newProjectWorkflow.getWorkflowId());

        return newProjectWorkflow.getUuidAsString();
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Audited(event = AutomationProjectAuditEvents.AUTOMATION_PROJECT_DUPLICATED, data = {
        @AuditData(key = "sourceProjectId", value = "#projectId"),
        @AuditData(key = "projectId", value = "#result")
    })
    public long duplicateProject(long projectId) {
        Project sourceProject = getProjectOfAutomationProject(projectId);

        Project newProject = new Project();

        newProject.setName(sourceProject.getName() + " (Copy)");
        newProject.setDescription(sourceProject.getDescription());
        newProject.setWorkspaceId(Workspace.DEFAULT_WORKSPACE_ID);
        newProject.setCategoryId(sourceProject.getCategoryId());
        newProject.setTagIds(sourceProject.getTagIds());

        newProject = projectService.create(newProject);

        AutomationProject sourceAutomationProject =
            automationProjectService.getAutomationProject(projectId);

        automationProjectService.create(
            newProject.getId(), sourceAutomationProject.getPermissionExpression());

        List<ProjectWorkflow> sourceProjectWorkflows = projectWorkflowService.getProjectWorkflows(
            sourceProject.getId(), sourceProject.getLastProjectVersion());

        for (ProjectWorkflow sourceProjectWorkflow : sourceProjectWorkflows) {
            Workflow sourceWorkflow = workflowService.getWorkflow(sourceProjectWorkflow.getWorkflowId());

            projectWorkflowFacade.addWorkflow(newProject.getId(), sourceWorkflow.getDefinition());
        }

        return newProject.getId();
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<AutomationProjectVersionDTO> getProjectVersions(long projectId) {
        Project project = getProjectOfAutomationProject(projectId);

        return project.getProjectVersions()
            .stream()
            .sorted(Comparator.comparingInt(ProjectVersion::getVersion)
                .reversed())
            .map(projectVersion -> new AutomationProjectVersionDTO(
                projectVersion.getVersion(),
                projectVersion.getStatus() == ProjectVersion.Status.PUBLISHED ? "PUBLISHED" : "DRAFT",
                Objects.toString(projectVersion.getPublishedDate(), null), projectVersion.getDescription()))
            .toList();
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public VersionWorkflowChange getProjectVersionWorkflowChange(
        long projectId, int projectVersion, String workflowUuid) {

        getProjectOfAutomationProject(projectId);

        return projectVersionWorkflowChangeService.getVersionWorkflowChange(projectId, projectVersion, workflowUuid);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public VersionWorkflowChanges getProjectVersionWorkflowChanges(long projectId, int projectVersion) {
        getProjectOfAutomationProject(projectId);

        return projectVersionWorkflowChangeService.getVersionWorkflowChanges(projectId, projectVersion);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public AutomationProjectDTO getProject(long projectId) {
        Project project = getProjectOfAutomationProject(projectId);

        List<AutomationProjectConfiguration> configurations =
            automationProjectConfigurationService.getConfigurations(projectId);

        return toDTO(
            project, automationProjectService.getAutomationProject(projectId), configurations.size());
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<AutomationProjectDTO> getProjects() {
        return getProjectsWithConfigurationCounts(null);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<AutomationProjectDTO> getProjects(long environmentId) {
        return getProjectsWithConfigurationCounts(environmentId);
    }

    private List<AutomationProjectDTO> getProjectsWithConfigurationCounts(@Nullable Long environmentId) {
        Map<Long, AutomationProject> automationProjects = getAutomationProjects();
        Map<Long, Long> configurationCounts =
            getConfigurationCounts(automationProjects.keySet(), environmentId);

        return getAutomationProjectProjects(automationProjects.keySet())
            .stream()
            .map(project -> toDTO(
                project, automationProjects.get(project.getId()),
                Math.toIntExact(configurationCounts.getOrDefault(project.getId(), 0L))))
            .toList();
    }

    @Override
    @SkipAutomationAuthorization
    public List<AutomationProjectDTO> getPublishedProjects() {
        Map<Long, AutomationProject> automationProjects = getAutomationProjects();

        return getAutomationProjectProjects(automationProjects.keySet())
            .stream()
            .map(project -> toPublishedDTO(project, automationProjects.get(project.getId())))
            .toList();
    }

    @Override
    @SkipAutomationAuthorization
    public List<AutomationProjectDTO> getPublishedProjects(String externalUserId, Environment environment) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        Map<String, Long> integrationIdsByComponentName = getIntegrationIdsByComponentName(environment, connectedUser);

        Map<Long, AutomationProject> automationProjects = getAutomationProjects();

        List<AutomationProjectDTO> projects = new ArrayList<>();

        for (Project project : getAutomationProjectProjects(automationProjects.keySet())) {
            Optional<AutomationProjectConfiguration> configurationOptional =
                automationProjectConfigurationResolver.resolve(connectedUser, project.getId());

            if (configurationOptional.isEmpty() || !configurationOptional.get()
                .isEnabled()) {

                continue;
            }

            AutomationProjectConfiguration configuration = configurationOptional.get();

            AutomationProjectDTO projectDTO = toPublishedDTO(
                project, automationProjects.get(project.getId()), configuration.getProjectVersion(),
                configuration.isAutomationHubVisible(),
                projectWorkflow -> automationProjectConfigurationService.isWorkflowEnabled(
                    configuration, projectWorkflow.getUuid()));

            if (embeddedPermissionEvaluator.evaluate(projectDTO.permissionExpression(), connectedUser)) {
                projects.add(
                    filterWorkflowTemplates(projectDTO, connectedUser, integrationIdsByComponentName, configuration));
            }
        }

        return projects;
    }

    @Override
    @SkipAutomationAuthorization
    public List<AutomationProjectConfigurationReleaseDTO> getPublishedProjectConfigurations(
        String externalUserId, Environment environment) {

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        Map<String, Long> integrationIdsByComponentName = getIntegrationIdsByComponentName(environment, connectedUser);

        Map<Long, AutomationProject> automationProjects = getAutomationProjects();

        List<AutomationProjectConfigurationReleaseDTO> releases = new ArrayList<>();

        for (Project project : getAutomationProjectProjects(automationProjects.keySet())) {
            AutomationProject automationProject = automationProjects.get(project.getId());

            if (!embeddedPermissionEvaluator.evaluate(
                automationProject.getPermissionExpression(), connectedUser)) {

                continue;
            }

            List<AutomationProjectConfiguration> configurations =
                automationProjectConfigurationService.getConfigurations(
                    project.getId(), connectedUser.getEnvironmentId());

            for (AutomationProjectConfiguration configuration : configurations) {
                if (!configuration.isEnabled()) {
                    continue;
                }

                AutomationProjectDTO projectDTO = toPublishedDTO(
                    project, automationProject, configuration.getProjectVersion(),
                    configuration.isAutomationHubVisible(),
                    projectWorkflow -> automationProjectConfigurationService.isWorkflowEnabled(
                        configuration, projectWorkflow.getUuid()));

                releases.add(
                    new AutomationProjectConfigurationReleaseDTO(
                        configuration.getId(), configuration.getName(),
                        filterWorkflowTemplates(
                            projectDTO, connectedUser, integrationIdsByComponentName, configuration)));
            }
        }

        return releases;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Audited(event = AutomationProjectAuditEvents.AUTOMATION_PROJECT_PUBLISHED, data = {
        @AuditData(key = "projectId", value = "#projectId"),
        @AuditData(key = "projectVersion", value = "#result")
    })
    public int publishProject(long projectId, @Nullable String description) {
        Project project = getProjectOfAutomationProject(projectId);

        int oldProjectVersion = project.getLastProjectVersion();

        List<ProjectWorkflow> oldProjectWorkflows = projectWorkflowService.getProjectWorkflows(
            project.getId(), oldProjectVersion);

        int newProjectVersion = projectService.publishProject(project.getId(), description, false);

        for (ProjectWorkflow oldProjectWorkflow : oldProjectWorkflows) {
            String oldWorkflowId = oldProjectWorkflow.getWorkflowId();

            Workflow duplicatedWorkflow = workflowService.duplicateWorkflow(oldWorkflowId);

            oldProjectWorkflow.setProjectVersion(newProjectVersion);
            oldProjectWorkflow.setWorkflowId(duplicatedWorkflow.getId());

            projectWorkflowService.publishWorkflow(
                project.getId(), oldProjectVersion, oldWorkflowId, oldProjectWorkflow);

            workflowTestConfigurationService.updateWorkflowId(oldWorkflowId, duplicatedWorkflow.getId());
            workflowNodeTestOutputService.updateWorkflowId(oldWorkflowId, duplicatedWorkflow.getId());
        }

        return newProjectVersion;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Audited(event = AutomationProjectAuditEvents.AUTOMATION_PROJECT_UPDATED, data = {
        @AuditData(key = "projectId", value = "#projectId"),
        @AuditData(key = "name", value = "#name"),
        @AuditData(key = "category", value = "#category"),
        @AuditData(key = "permissionExpression", value = "#permissionExpression")
    })
    public void updateProject(
        long projectId, String name, String description, String category, List<String> tags,
        String permissionExpression) {
        Project project = getProjectOfAutomationProject(projectId);

        project.setName(name);
        project.setDescription(description);
        project.setCategoryId(resolveCategory(category));
        project.setTagIds(resolveTags(tags));

        projectService.update(project);

        if (permissionExpression != null) {
            automationProjectService.updatePermissionExpression(
                projectId, normalizePermissionExpression(permissionExpression));
        }
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public void updateProjectWorkflow(String workflowUuid, String label, String description) {
        String workflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);

        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        getProjectOfAutomationProject(projectWorkflow.getProjectId());

        Workflow workflow = workflowService.getWorkflow(workflowId);

        Map<String, Object> definitionMap = JsonUtils.read(workflow.getDefinition(), new TypeReference<>() {});

        definitionMap.put("label", label);
        definitionMap.put("description", description == null ? "" : description);

        workflowService.update(
            workflowId, JsonUtils.writeWithDefaultPrettyPrinter(definitionMap), workflow.getVersion());
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Audited(event = WorkflowAuditEvents.AUTOMATION_PERMISSION_EXPRESSION_UPDATED, data = {
        @AuditData(key = "workflowUuid", value = "#workflowUuid"),
        @AuditData(key = "permissionExpression", value = "#permissionExpression")
    })
    public void updateProjectWorkflowPermissionExpression(String workflowUuid, String permissionExpression) {
        String workflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);

        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        getProjectOfAutomationProject(projectWorkflow.getProjectId());

        automationProjectService.updateWorkflowPermissionExpression(
            projectWorkflow.getProjectId(), projectWorkflow.getUuid(),
            normalizePermissionExpression(permissionExpression));
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<AutomationProjectCategoryDTO> getCategories() {
        Set<Long> categoryIds = getProjects().stream()
            .map(AutomationProjectDTO::categoryId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        return categoryService.getCategories()
            .stream()
            .filter(category -> categoryIds.contains(category.getId()))
            .map(category -> new AutomationProjectCategoryDTO(
                Objects.requireNonNull(category.getId(), "category id"), category.getName()))
            .toList();
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<AutomationProjectTagDTO> getTags() {
        Set<Long> tagIds = getProjects().stream()
            .flatMap(project -> project.tagIds()
                .stream())
            .collect(Collectors.toSet());

        return tagService.getTags()
            .stream()
            .filter(tag -> tagIds.contains(tag.getId()))
            .map(tag -> new AutomationProjectTagDTO(
                Objects.requireNonNull(tag.getId(), "tag id"), tag.getName()))
            .toList();
    }

    private Map<String, Long> getIntegrationIdsByComponentName(Environment environment, ConnectedUser connectedUser) {
        List<Long> integrationIds = integrationInstanceConfigurationService
            .getIntegrationInstanceConfigurations(environment, true)
            .stream()
            .map(IntegrationInstanceConfiguration::getIntegrationId)
            .distinct()
            .toList();

        if (integrationIds.isEmpty()) {
            return Map.of();
        }

        Map<String, Long> integrationIdsByComponentName = new HashMap<>();

        for (Integration integration : integrationService.getIntegrations(integrationIds)) {
            if (!embeddedPermissionEvaluator.evaluate(integration.getPermissionExpression(), connectedUser)) {
                continue;
            }

            integrationIdsByComponentName.merge(integration.getComponentName(), integration.getId(), Math::min);
        }

        return integrationIdsByComponentName;
    }

    private AutomationProjectDTO filterWorkflowTemplates(
        AutomationProjectDTO project, ConnectedUser connectedUser,
        Map<String, Long> integrationIdsByComponentName, AutomationProjectConfiguration configuration) {

        List<ConnectedUserWorkflowTemplateDTO> visibleTemplates = project.workflowTemplates()
            .stream()
            .filter(template -> embeddedPermissionEvaluator.evaluate(
                template.permissionExpression(), connectedUser))
            .map(template -> template.withIntegrationIds(integrationIdsByComponentName))
            .map(template -> markProvided(template, configuration))
            .toList();

        return new AutomationProjectDTO(
            project.id(), project.name(), project.description(), project.categoryId(), project.tagIds(),
            project.published(), project.version(), project.lastPublishedVersion(), visibleTemplates,
            project.permissionExpression(), project.automationHubVisible(), project.configurationCount(),
            project.lastPublishedDate());
    }

    private ConnectedUserWorkflowTemplateDTO markProvided(
        ConnectedUserWorkflowTemplateDTO template, AutomationProjectConfiguration configuration) {

        ProvidedNames providedNames = connectedUserReferenceDeploymentManager.getProvidedNames(
            configuration, template.workflowUuid());

        return template.withProvided(providedNames.componentNames(), providedNames.inputNames());
    }

    private static List<ConnectedUserWorkflowTemplateDTO.Input> toInputs(Workflow workflow) {
        List<Workflow.Input> inputs = workflow.getInputs();

        return inputs.stream()
            .map(input -> new ConnectedUserWorkflowTemplateDTO.Input(
                input.name(), input.label(), input.type(), input.required()))
            .toList();
    }

    private static String normalizePermissionExpression(String permissionExpression) {
        return StringUtils.isBlank(permissionExpression) ? null : permissionExpression.trim();
    }

    private List<Project> getAutomationProjectProjects(Set<Long> projectIds) {
        if (projectIds.isEmpty()) {
            return List.of();
        }

        return projectService.getProjects(List.copyOf(projectIds))
            .stream()
            .filter(project -> Objects.equals(project.getWorkspaceId(), Workspace.DEFAULT_WORKSPACE_ID))
            .sorted(Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    private Map<Long, AutomationProject> getAutomationProjects() {
        return automationProjectService.getAutomationProjects()
            .stream()
            .collect(Collectors.toMap(AutomationProject::getProjectId, Function.identity()));
    }

    private Project getProjectOfAutomationProject(long projectId) {
        Project project = projectService.getProject(projectId);

        Optional<AutomationProject> automationProject =
            automationProjectService.fetchAutomationProject(projectId);

        if (automationProject.isEmpty()) {
            throw new IllegalArgumentException(
                "Project with id " + projectId + " is not an automation project");
        }

        return project;
    }

    private Long resolveCategory(String categoryName) {
        if (StringUtils.isBlank(categoryName)) {
            return null;
        }

        Category category = categoryService.save(new Category(categoryName));

        return category.getId();
    }

    private List<Long> resolveTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return List.of();
        }

        List<Tag> tags = tagService.save(
            tagNames.stream()
                .map(Tag::new)
                .toList());

        return tags.stream()
            .map(Tag::getId)
            .toList();
    }

    private Map<Long, Long> getConfigurationCounts(Set<Long> projectIds, @Nullable Long environmentId) {
        return automationProjectConfigurationService.getConfigurations(projectIds)
            .stream()
            .filter(configuration -> environmentId == null || configuration.getEnvironmentId() == environmentId)
            .collect(
                Collectors.groupingBy(AutomationProjectConfiguration::getProjectId, Collectors.counting()));
    }

    private AutomationProjectDTO toDTO(
        Project project, AutomationProject automationProject, int configurationCount) {

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(
            project.getId(), project.getLastProjectVersion());

        Map<UUID, String> workflowPermissionExpressions =
            automationProjectService.getWorkflowPermissionExpressions(project.getId());

        List<ConnectedUserWorkflowTemplateDTO> workflowTemplates = projectWorkflows.stream()
            .map(projectWorkflow -> {
                Workflow workflow = workflowService.getWorkflow(projectWorkflow.getWorkflowId());

                return new ConnectedUserWorkflowTemplateDTO(
                    projectWorkflow.getUuidAsString(), workflow.getLabel(), workflow.getDescription(),
                    Objects.toString(workflow.getLastModifiedDate(), null),
                    workflowComponentResolver.getTriggerComponents(workflow),
                    workflowComponentResolver.getTaskComponents(workflow), toInputs(workflow),
                    workflowPermissionExpressions.get(projectWorkflow.getUuid()), projectWorkflow.getWorkflowId());
            })
            .filter(Objects::nonNull)
            .toList();

        ProjectVersion lastPublishedProjectVersion = project.getLastPublishedProjectVersion();

        boolean published = lastPublishedProjectVersion != null;
        Integer lastPublishedVersion = published ? lastPublishedProjectVersion.getVersion() : null;

        return new AutomationProjectDTO(
            project.getId(), project.getName(), project.getDescription(), project.getCategoryId(),
            project.getTagIds(), published, project.getLastProjectVersion(), lastPublishedVersion, workflowTemplates,
            automationProject.getPermissionExpression(), true, configurationCount,
            project.getLastPublishedDate());
    }

    private AutomationProjectDTO toPublishedDTO(
        Project project, AutomationProject automationProject) {

        ProjectVersion lastPublishedProjectVersion = project.getLastPublishedProjectVersion();

        return toPublishedDTO(
            project, automationProject,
            lastPublishedProjectVersion == null ? null : lastPublishedProjectVersion.getVersion(), true,
            projectWorkflow -> true);
    }

    private AutomationProjectDTO toPublishedDTO(
        Project project, AutomationProject automationProject, @Nullable Integer publishedVersion,
        boolean automationHubVisible, Predicate<ProjectWorkflow> projectWorkflowFilter) {

        List<ConnectedUserWorkflowTemplateDTO> workflowTemplates;

        if (publishedVersion == null) {
            workflowTemplates = List.of();
        } else {
            List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(
                project.getId(), publishedVersion);

            Map<UUID, String> workflowPermissionExpressions =
                automationProjectService.getWorkflowPermissionExpressions(project.getId());

            workflowTemplates = projectWorkflows.stream()
                .filter(projectWorkflowFilter)
                .map(projectWorkflow -> {
                    Workflow workflow = workflowService.getWorkflow(projectWorkflow.getWorkflowId());

                    return new ConnectedUserWorkflowTemplateDTO(
                        projectWorkflow.getUuidAsString(), workflow.getLabel(), workflow.getDescription(),
                        Objects.toString(workflow.getLastModifiedDate(), null),
                        workflowComponentResolver.getTriggerComponents(workflow),
                        workflowComponentResolver.getTaskComponents(workflow), toInputs(workflow),
                        workflowPermissionExpressions.get(projectWorkflow.getUuid()));
                })
                .filter(Objects::nonNull)
                .toList();
        }

        return new AutomationProjectDTO(
            project.getId(), project.getName(), project.getDescription(), project.getCategoryId(),
            project.getTagIds(), publishedVersion != null, project.getLastProjectVersion(), publishedVersion,
            workflowTemplates, automationProject.getPermissionExpression(), automationHubVisible, 0,
            project.getLastPublishedDate());
    }
}

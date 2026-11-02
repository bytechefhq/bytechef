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
import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProject;
import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProjectConfiguration;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectCategoryDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectTagDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectVersionDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserReferenceDeploymentManager.ProvidedNames;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.AutomationWorkflowProjectConfigurationService;
import com.bytechef.ee.embedded.configuration.service.AutomationWorkflowProjectService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
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
@SkipAutomationAuthorization
public class AutomationWorkflowProjectFacadeImpl implements AutomationWorkflowProjectFacade {
    private static final String DEFAULT_DEFINITION = """
        {
            "label": "New Workflow",
            "description": "",
            "inputs": [],
            "triggers": [],
            "tasks": []
        }
        """;

    private final AutomationWorkflowProjectConfigurationService automationWorkflowProjectConfigurationService;
    private final AutomationWorkflowProjectService automationWorkflowProjectService;
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
    public AutomationWorkflowProjectFacadeImpl(
        AutomationWorkflowProjectConfigurationService automationWorkflowProjectConfigurationService,
        AutomationWorkflowProjectService automationWorkflowProjectService, CategoryService categoryService,
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
        this.automationWorkflowProjectConfigurationService = automationWorkflowProjectConfigurationService;
        this.automationWorkflowProjectService = automationWorkflowProjectService;
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
    public long createProject(
        String name, String description, String category, List<String> tags, String permissionExpression) {
        Project project = new Project();

        project.setName(name);
        project.setDescription(description);
        project.setWorkspaceId(Workspace.DEFAULT_WORKSPACE_ID);
        project.setCategoryId(resolveCategory(category));
        project.setTagIds(resolveTags(tags));

        project = projectService.create(project);

        automationWorkflowProjectService.create(
            project.getId(), normalizePermissionExpression(permissionExpression));

        return project.getId();
    }

    @Override
    public String createProjectWorkflow(long projectId, String definition, String permissionExpression) {
        getProjectOfAutomationWorkflowProject(projectId);

        ProjectWorkflow projectWorkflow = projectWorkflowFacade.addWorkflow(
            projectId, StringUtils.isEmpty(definition) ? DEFAULT_DEFINITION : definition);

        String normalized = normalizePermissionExpression(permissionExpression);

        if (normalized != null) {
            automationWorkflowProjectService.updateWorkflowPermissionExpression(
                projectId, projectWorkflow.getUuid(), normalized);
        }

        return projectWorkflow.getUuidAsString();
    }

    @Override
    public void deleteProject(long projectId) {
        getProjectOfAutomationWorkflowProject(projectId);

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

        automationWorkflowProjectService.delete(projectId);

        projectService.delete(projectId);
    }

    @Override
    public void deleteProjectWorkflow(String workflowUuid) {
        String lastWorkflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);

        ProjectWorkflow lastProjectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(lastWorkflowId);

        Project project = getProjectOfAutomationWorkflowProject(lastProjectWorkflow.getProjectId());

        projectWorkflowService.fetchProjectWorkflow(project.getId(), project.getLastProjectVersion(), workflowUuid)
            .ifPresent(draftProjectWorkflow -> projectWorkflowFacade.deleteWorkflow(
                draftProjectWorkflow.getWorkflowId()));
    }

    @Override
    public String duplicateProjectWorkflow(String workflowUuid) {
        String workflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);

        ProjectWorkflow sourceProjectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        long projectId = sourceProjectWorkflow.getProjectId();

        getProjectOfAutomationWorkflowProject(projectId);

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
    public long duplicateProject(long projectId) {
        Project sourceProject = getProjectOfAutomationWorkflowProject(projectId);

        Project newProject = new Project();

        newProject.setName(sourceProject.getName() + " (Copy)");
        newProject.setDescription(sourceProject.getDescription());
        newProject.setWorkspaceId(Workspace.DEFAULT_WORKSPACE_ID);
        newProject.setCategoryId(sourceProject.getCategoryId());
        newProject.setTagIds(sourceProject.getTagIds());

        newProject = projectService.create(newProject);

        AutomationWorkflowProject sourceAutomationWorkflowProject =
            automationWorkflowProjectService.getAutomationWorkflowProject(projectId);

        automationWorkflowProjectService.create(
            newProject.getId(), sourceAutomationWorkflowProject.getPermissionExpression());

        List<ProjectWorkflow> sourceProjectWorkflows = projectWorkflowService.getProjectWorkflows(
            sourceProject.getId(), sourceProject.getLastProjectVersion());

        for (ProjectWorkflow sourceProjectWorkflow : sourceProjectWorkflows) {
            Workflow sourceWorkflow = workflowService.getWorkflow(sourceProjectWorkflow.getWorkflowId());

            projectWorkflowFacade.addWorkflow(newProject.getId(), sourceWorkflow.getDefinition());
        }

        return newProject.getId();
    }

    @Override
    public List<AutomationWorkflowProjectVersionDTO> getProjectVersions(long projectId) {
        Project project = getProjectOfAutomationWorkflowProject(projectId);

        return project.getProjectVersions()
            .stream()
            .sorted(Comparator.comparingInt(ProjectVersion::getVersion)
                .reversed())
            .map(projectVersion -> new AutomationWorkflowProjectVersionDTO(
                projectVersion.getVersion(),
                projectVersion.getStatus() == ProjectVersion.Status.PUBLISHED ? "PUBLISHED" : "DRAFT",
                Objects.toString(projectVersion.getPublishedDate(), null), projectVersion.getDescription()))
            .toList();
    }

    @Override
    public VersionWorkflowChange getProjectVersionWorkflowChange(
        long projectId, int projectVersion, String workflowUuid) {

        getProjectOfAutomationWorkflowProject(projectId);

        return projectVersionWorkflowChangeService.getVersionWorkflowChange(projectId, projectVersion, workflowUuid);
    }

    @Override
    public VersionWorkflowChanges getProjectVersionWorkflowChanges(long projectId, int projectVersion) {
        getProjectOfAutomationWorkflowProject(projectId);

        return projectVersionWorkflowChangeService.getVersionWorkflowChanges(projectId, projectVersion);
    }

    @Override
    public AutomationWorkflowProjectDTO getProject(long projectId) {
        Project project = getProjectOfAutomationWorkflowProject(projectId);

        List<AutomationWorkflowProjectConfiguration> configurations =
            automationWorkflowProjectConfigurationService.getConfigurations(projectId);

        return toDTO(
            project, automationWorkflowProjectService.getAutomationWorkflowProject(projectId), configurations.size());
    }

    @Override
    public List<AutomationWorkflowProjectDTO> getProjects() {
        Map<Long, AutomationWorkflowProject> automationWorkflowProjects = getAutomationWorkflowProjects();
        Map<Long, Long> configurationCounts = getConfigurationCounts(automationWorkflowProjects.keySet());

        return getAutomationWorkflowProjectProjects(automationWorkflowProjects.keySet())
            .stream()
            .map(project -> toDTO(
                project, automationWorkflowProjects.get(project.getId()),
                Math.toIntExact(configurationCounts.getOrDefault(project.getId(), 0L))))
            .toList();
    }

    @Override
    public List<AutomationWorkflowProjectDTO> getPublishedProjects() {
        Map<Long, AutomationWorkflowProject> automationWorkflowProjects = getAutomationWorkflowProjects();

        return getAutomationWorkflowProjectProjects(automationWorkflowProjects.keySet())
            .stream()
            .map(project -> toPublishedDTO(project, automationWorkflowProjects.get(project.getId())))
            .toList();
    }

    @Override
    public List<AutomationWorkflowProjectDTO> getPublishedProjects(String externalUserId, Environment environment) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        Map<String, Long> integrationIdsByComponentName = getIntegrationIdsByComponentName(environment, connectedUser);

        Map<Long, AutomationWorkflowProject> automationWorkflowProjects = getAutomationWorkflowProjects();

        List<AutomationWorkflowProjectDTO> projects = new ArrayList<>();

        for (Project project : getAutomationWorkflowProjectProjects(automationWorkflowProjects.keySet())) {
            Optional<AutomationWorkflowProjectConfiguration> configurationOptional =
                automationWorkflowProjectConfigurationService.fetchConfiguration(
                    project.getId(), environment.ordinal());

            if (configurationOptional.isEmpty() || !configurationOptional.get()
                .isEnabled()) {

                continue;
            }

            AutomationWorkflowProjectConfiguration configuration = configurationOptional.get();

            AutomationWorkflowProjectDTO projectDTO = toPublishedDTO(
                project, automationWorkflowProjects.get(project.getId()), configuration.getProjectVersion(),
                configuration.isAutomationHubVisible(),
                projectWorkflow -> automationWorkflowProjectConfigurationService.isWorkflowEnabled(
                    configuration, projectWorkflow.getUuid()));

            if (embeddedPermissionEvaluator.evaluate(projectDTO.permissionExpression(), connectedUser)) {
                projects.add(
                    filterWorkflowTemplates(projectDTO, connectedUser, integrationIdsByComponentName, configuration));
            }
        }

        return projects;
    }

    @Override
    public int publishProject(long projectId, @Nullable String description) {
        Project project = getProjectOfAutomationWorkflowProject(projectId);

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
    public void updateProject(
        long projectId, String name, String description, String category, List<String> tags,
        String permissionExpression) {
        Project project = getProjectOfAutomationWorkflowProject(projectId);

        project.setName(name);
        project.setDescription(description);
        project.setCategoryId(resolveCategory(category));
        project.setTagIds(resolveTags(tags));

        projectService.update(project);

        if (permissionExpression != null) {
            automationWorkflowProjectService.updatePermissionExpression(
                projectId, normalizePermissionExpression(permissionExpression));
        }
    }

    @Override
    public void updateProjectWorkflow(String workflowUuid, String label, String description) {
        String workflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);

        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        getProjectOfAutomationWorkflowProject(projectWorkflow.getProjectId());

        Workflow workflow = workflowService.getWorkflow(workflowId);

        Map<String, Object> definitionMap = JsonUtils.read(workflow.getDefinition(), new TypeReference<>() {});

        definitionMap.put("label", label);
        definitionMap.put("description", description == null ? "" : description);

        workflowService.update(
            workflowId, JsonUtils.writeWithDefaultPrettyPrinter(definitionMap), workflow.getVersion());
    }

    @Override
    public void updateProjectWorkflowPermissionExpression(String workflowUuid, String permissionExpression) {
        String workflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);

        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        getProjectOfAutomationWorkflowProject(projectWorkflow.getProjectId());

        automationWorkflowProjectService.updateWorkflowPermissionExpression(
            projectWorkflow.getProjectId(), projectWorkflow.getUuid(),
            normalizePermissionExpression(permissionExpression));
    }

    @Override
    public List<AutomationWorkflowProjectCategoryDTO> getCategories() {
        Set<Long> categoryIds = getProjects().stream()
            .map(AutomationWorkflowProjectDTO::categoryId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        return categoryService.getCategories()
            .stream()
            .filter(category -> categoryIds.contains(category.getId()))
            .map(category -> new AutomationWorkflowProjectCategoryDTO(
                Objects.requireNonNull(category.getId(), "category id"), category.getName()))
            .toList();
    }

    @Override
    public List<AutomationWorkflowProjectTagDTO> getTags() {
        Set<Long> tagIds = getProjects().stream()
            .flatMap(project -> project.tagIds()
                .stream())
            .collect(Collectors.toSet());

        return tagService.getTags()
            .stream()
            .filter(tag -> tagIds.contains(tag.getId()))
            .map(tag -> new AutomationWorkflowProjectTagDTO(
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

    private AutomationWorkflowProjectDTO filterWorkflowTemplates(
        AutomationWorkflowProjectDTO project, ConnectedUser connectedUser,
        Map<String, Long> integrationIdsByComponentName, AutomationWorkflowProjectConfiguration configuration) {

        List<ConnectedUserWorkflowTemplateDTO> visibleTemplates = project.workflowTemplates()
            .stream()
            .filter(template -> embeddedPermissionEvaluator.evaluate(
                template.permissionExpression(), connectedUser))
            .map(template -> template.withIntegrationIds(integrationIdsByComponentName))
            .map(template -> markProvided(template, configuration))
            .toList();

        return new AutomationWorkflowProjectDTO(
            project.id(), project.name(), project.description(), project.categoryId(), project.tagIds(),
            project.published(), project.version(), project.lastPublishedVersion(), visibleTemplates,
            project.permissionExpression(), project.automationHubVisible(), project.configurationCount(),
            project.lastPublishedDate());
    }

    private ConnectedUserWorkflowTemplateDTO markProvided(
        ConnectedUserWorkflowTemplateDTO template, AutomationWorkflowProjectConfiguration configuration) {

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

    private List<Project> getAutomationWorkflowProjectProjects(Set<Long> projectIds) {
        if (projectIds.isEmpty()) {
            return List.of();
        }

        return projectService.getProjects(List.copyOf(projectIds))
            .stream()
            .filter(project -> Objects.equals(project.getWorkspaceId(), Workspace.DEFAULT_WORKSPACE_ID))
            .sorted(Comparator.comparing(Project::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    private Map<Long, AutomationWorkflowProject> getAutomationWorkflowProjects() {
        return automationWorkflowProjectService.getAutomationWorkflowProjects()
            .stream()
            .collect(Collectors.toMap(AutomationWorkflowProject::getProjectId, Function.identity()));
    }

    private Project getProjectOfAutomationWorkflowProject(long projectId) {
        Project project = projectService.getProject(projectId);

        Optional<AutomationWorkflowProject> automationWorkflowProject =
            automationWorkflowProjectService.fetchAutomationWorkflowProject(projectId);

        if (automationWorkflowProject.isEmpty()) {
            throw new IllegalArgumentException(
                "Project with id " + projectId + " is not an automation workflow project");
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

    private Map<Long, Long> getConfigurationCounts(Set<Long> projectIds) {
        return automationWorkflowProjectConfigurationService.getConfigurations(projectIds)
            .stream()
            .collect(
                Collectors.groupingBy(AutomationWorkflowProjectConfiguration::getProjectId, Collectors.counting()));
    }

    private AutomationWorkflowProjectDTO toDTO(
        Project project, AutomationWorkflowProject automationWorkflowProject, int configurationCount) {

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(
            project.getId(), project.getLastProjectVersion());

        Map<UUID, String> workflowPermissionExpressions =
            automationWorkflowProjectService.getWorkflowPermissionExpressions(project.getId());

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

        return new AutomationWorkflowProjectDTO(
            project.getId(), project.getName(), project.getDescription(), project.getCategoryId(),
            project.getTagIds(), published, project.getLastProjectVersion(), lastPublishedVersion, workflowTemplates,
            automationWorkflowProject.getPermissionExpression(), true, configurationCount,
            project.getLastPublishedDate());
    }

    private AutomationWorkflowProjectDTO toPublishedDTO(
        Project project, AutomationWorkflowProject automationWorkflowProject) {

        ProjectVersion lastPublishedProjectVersion = project.getLastPublishedProjectVersion();

        return toPublishedDTO(
            project, automationWorkflowProject,
            lastPublishedProjectVersion == null ? null : lastPublishedProjectVersion.getVersion(), true,
            projectWorkflow -> true);
    }

    private AutomationWorkflowProjectDTO toPublishedDTO(
        Project project, AutomationWorkflowProject automationWorkflowProject, @Nullable Integer publishedVersion,
        boolean automationHubVisible, Predicate<ProjectWorkflow> projectWorkflowFilter) {

        List<ConnectedUserWorkflowTemplateDTO> workflowTemplates;

        if (publishedVersion == null) {
            workflowTemplates = List.of();
        } else {
            List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(
                project.getId(), publishedVersion);

            Map<UUID, String> workflowPermissionExpressions =
                automationWorkflowProjectService.getWorkflowPermissionExpressions(project.getId());

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

        return new AutomationWorkflowProjectDTO(
            project.getId(), project.getName(), project.getDescription(), project.getCategoryId(),
            project.getTagIds(), publishedVersion != null, project.getLastProjectVersion(), publishedVersion,
            workflowTemplates, automationWorkflowProject.getPermissionExpression(), automationHubVisible, 0,
            project.getLastPublishedDate());
    }
}

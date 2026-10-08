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
import com.bytechef.automation.configuration.domain.SystemProjects;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.security.SkipAutomationAuthorization;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.embedded.configuration.domain.AutomationWorkflowProject;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectCategoryDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectTagDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectVersionDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.event.AutomationWorkflowProjectPublishedEvent;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.AutomationWorkflowProjectService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.configuration.workflow.WorkflowPreDeleteListener;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
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
    private static final String MARKER = SystemProjects.EMBEDDED_AUTOMATION_NAME_PREFIX;

    private static final String DEFAULT_DEFINITION = """
        {
            "label": "New Workflow",
            "description": "",
            "inputs": [],
            "triggers": [],
            "tasks": []
        }
        """;

    private final ApplicationEventPublisher applicationEventPublisher;
    private final AutomationWorkflowProjectService automationWorkflowProjectService;
    private final CategoryService categoryService;
    private final ConnectedUserReferenceRolloutManager connectedUserReferenceRolloutManager;
    private final ConnectedUserService connectedUserService;
    private final EmbeddedPermissionEvaluator embeddedPermissionEvaluator;
    private final ProjectService projectService;
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
        ApplicationEventPublisher applicationEventPublisher,
        AutomationWorkflowProjectService automationWorkflowProjectService, CategoryService categoryService,
        ConnectedUserReferenceRolloutManager connectedUserReferenceRolloutManager,
        ConnectedUserService connectedUserService, EmbeddedPermissionEvaluator embeddedPermissionEvaluator,
        ProjectService projectService,
        ProjectWorkflowFacade projectWorkflowFacade, ProjectWorkflowService projectWorkflowService,
        TagService tagService, WorkflowComponentResolver workflowComponentResolver,
        WorkflowNodeTestOutputService workflowNodeTestOutputService, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService,
        List<WorkflowPreDeleteListener> workflowPreDeleteListeners) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.automationWorkflowProjectService = automationWorkflowProjectService;
        this.categoryService = categoryService;
        this.connectedUserReferenceRolloutManager = connectedUserReferenceRolloutManager;
        this.connectedUserService = connectedUserService;
        this.embeddedPermissionEvaluator = embeddedPermissionEvaluator;
        this.projectService = projectService;
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
        String name, String description, String category, List<String> tags, String permissionExpression,
        @Nullable Boolean automationHubVisible) {
        if (name != null && name.startsWith("__EMBEDDED")) {
            throw new IllegalArgumentException("Project name must not start with '__EMBEDDED': " + name);
        }

        Project project = new Project();

        project.setName(MARKER + name);
        project.setDescription(description);
        project.setWorkspaceId(Workspace.DEFAULT_WORKSPACE_ID);
        project.setCategoryId(resolveCategory(category));
        project.setTagIds(resolveTags(tags));

        project = projectService.create(project);

        automationWorkflowProjectService.create(
            project.getId(), normalizePermissionExpression(permissionExpression),
            automationHubVisible == null || automationHubVisible);

        return project.getId();
    }

    @Override
    public String createProjectWorkflow(long projectId, String definition, String permissionExpression) {
        getMarkedProject(projectId);

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
        getMarkedProject(projectId);

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

        Project project = getMarkedProject(lastProjectWorkflow.getProjectId());

        projectWorkflowService.fetchProjectWorkflow(project.getId(), project.getLastProjectVersion(), workflowUuid)
            .ifPresent(draftProjectWorkflow -> projectWorkflowFacade.deleteWorkflow(
                draftProjectWorkflow.getWorkflowId()));
    }

    @Override
    public String duplicateProjectWorkflow(String workflowUuid) {
        String workflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);

        ProjectWorkflow sourceProjectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        long projectId = sourceProjectWorkflow.getProjectId();

        getMarkedProject(projectId);

        Workflow sourceWorkflow = workflowService.getWorkflow(workflowId);

        String sourceDefinition = sourceWorkflow.getDefinition();

        Map<String, Object> definitionMap = JsonUtils.read(sourceDefinition, new TypeReference<>() {});

        definitionMap.compute("label", (k, label) -> (label == null ? "Workflow" : label.toString()) + " (Copy)");

        String newDefinition = JsonUtils.writeWithDefaultPrettyPrinter(definitionMap);

        ProjectWorkflow newProjectWorkflow = projectWorkflowFacade.addWorkflow(projectId, newDefinition);

        return newProjectWorkflow.getUuidAsString();
    }

    @Override
    public long duplicateProject(long projectId) {
        Project sourceProject = getMarkedProject(projectId);

        String sourceDisplayName = sourceProject.getName()
            .substring(MARKER.length());

        Project newProject = new Project();

        newProject.setName(MARKER + sourceDisplayName + " (Copy)");
        newProject.setDescription(sourceProject.getDescription());
        newProject.setWorkspaceId(Workspace.DEFAULT_WORKSPACE_ID);
        newProject.setCategoryId(sourceProject.getCategoryId());
        newProject.setTagIds(sourceProject.getTagIds());

        newProject = projectService.create(newProject);

        AutomationWorkflowProject sourceAutomationWorkflowProject =
            automationWorkflowProjectService.getAutomationWorkflowProject(projectId);

        automationWorkflowProjectService.create(
            newProject.getId(), sourceAutomationWorkflowProject.getPermissionExpression(),
            sourceAutomationWorkflowProject.isAutomationHubVisible());

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
        Project project = getMarkedProject(projectId);

        return project.getProjectVersions()
            .stream()
            .map(projectVersion -> new AutomationWorkflowProjectVersionDTO(
                projectVersion.getVersion(),
                projectVersion.getStatus() == ProjectVersion.Status.PUBLISHED ? "PUBLISHED" : "DRAFT",
                Objects.toString(projectVersion.getPublishedDate(), null)))
            .toList();
    }

    @Override
    public AutomationWorkflowProjectDTO getProject(long projectId) {
        Project project = getMarkedProject(projectId);

        return toDTO(project, automationWorkflowProjectService.getAutomationWorkflowProject(projectId));
    }

    @Override
    public List<AutomationWorkflowProjectDTO> getProjects() {
        Map<Long, AutomationWorkflowProject> automationWorkflowProjects = getAutomationWorkflowProjects();

        return getAutomationWorkflowProjectProjects(automationWorkflowProjects.keySet())
            .stream()
            .map(project -> toDTO(project, automationWorkflowProjects.get(project.getId())))
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

        return getPublishedProjects().stream()
            .filter(project -> embeddedPermissionEvaluator.evaluate(project.permissionExpression(), connectedUser))
            .map(project -> filterWorkflowTemplates(project, connectedUser))
            .toList();
    }

    @Override
    public void publishProject(long projectId) {
        Project project = getMarkedProject(projectId);

        int oldProjectVersion = project.getLastProjectVersion();

        List<ProjectWorkflow> oldProjectWorkflows = projectWorkflowService.getProjectWorkflows(
            project.getId(), oldProjectVersion);

        int newProjectVersion = projectService.publishProject(project.getId(), null, false);

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

        applicationEventPublisher.publishEvent(new AutomationWorkflowProjectPublishedEvent(projectId));
    }

    @Override
    public void updateProject(
        long projectId, String name, String description, String category, List<String> tags,
        String permissionExpression, @Nullable Boolean automationHubVisible) {
        Project project = getMarkedProject(projectId);

        project.setName(MARKER + name);
        project.setDescription(description);
        project.setCategoryId(resolveCategory(category));
        project.setTagIds(resolveTags(tags));

        projectService.update(project);

        if (permissionExpression != null) {
            automationWorkflowProjectService.updatePermissionExpression(
                projectId, normalizePermissionExpression(permissionExpression));
        }

        if (automationHubVisible != null) {
            automationWorkflowProjectService.updateAutomationHubVisible(projectId, automationHubVisible);
        }
    }

    @Override
    public void updateProjectWorkflow(String workflowUuid, String label, String description) {
        String workflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);

        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        getMarkedProject(projectWorkflow.getProjectId());

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

        getMarkedProject(projectWorkflow.getProjectId());

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

    private AutomationWorkflowProjectDTO filterWorkflowTemplates(
        AutomationWorkflowProjectDTO project, ConnectedUser connectedUser) {
        List<ConnectedUserWorkflowTemplateDTO> visibleTemplates = project.workflowTemplates()
            .stream()
            .filter(template -> embeddedPermissionEvaluator.evaluate(
                template.permissionExpression(), connectedUser))
            .toList();

        return new AutomationWorkflowProjectDTO(
            project.id(), project.name(), project.description(), project.categoryId(), project.tagIds(),
            project.published(), project.version(), project.lastPublishedVersion(), visibleTemplates,
            project.permissionExpression(), project.automationHubVisible());
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
            .sorted(Comparator.comparing(Project::getName))
            .toList();
    }

    private Map<Long, AutomationWorkflowProject> getAutomationWorkflowProjects() {
        return automationWorkflowProjectService.getAutomationWorkflowProjects()
            .stream()
            .collect(Collectors.toMap(AutomationWorkflowProject::getProjectId, Function.identity()));
    }

    private Project getMarkedProject(long projectId) {
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

    private AutomationWorkflowProjectDTO toDTO(Project project, AutomationWorkflowProject automationWorkflowProject) {
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

        String markedName = project.getName();
        String displayName = markedName.substring(MARKER.length());

        ProjectVersion lastPublishedProjectVersion = project.getLastPublishedProjectVersion();

        boolean published = lastPublishedProjectVersion != null;
        Integer lastPublishedVersion = published ? lastPublishedProjectVersion.getVersion() : null;

        return new AutomationWorkflowProjectDTO(
            project.getId(), displayName, project.getDescription(), project.getCategoryId(),
            project.getTagIds(), published, project.getLastProjectVersion(), lastPublishedVersion, workflowTemplates,
            automationWorkflowProject.getPermissionExpression(), automationWorkflowProject.isAutomationHubVisible());
    }

    private AutomationWorkflowProjectDTO toPublishedDTO(
        Project project, AutomationWorkflowProject automationWorkflowProject) {
        ProjectVersion lastPublishedProjectVersion = project.getLastPublishedProjectVersion();

        List<ConnectedUserWorkflowTemplateDTO> workflowTemplates;

        if (lastPublishedProjectVersion == null) {
            workflowTemplates = List.of();
        } else {
            List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(
                project.getId(), lastPublishedProjectVersion.getVersion());

            Map<UUID, String> workflowPermissionExpressions =
                automationWorkflowProjectService.getWorkflowPermissionExpressions(project.getId());

            workflowTemplates = projectWorkflows.stream()
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

        String markedName = project.getName();
        String displayName = markedName.substring(MARKER.length());

        boolean published = lastPublishedProjectVersion != null;
        Integer lastPublishedVersion = published ? lastPublishedProjectVersion.getVersion() : null;

        return new AutomationWorkflowProjectDTO(
            project.getId(), displayName, project.getDescription(), project.getCategoryId(),
            project.getTagIds(), published, project.getLastProjectVersion(), lastPublishedVersion, workflowTemplates,
            automationWorkflowProject.getPermissionExpression(), automationWorkflowProject.isAutomationHubVisible());
    }
}

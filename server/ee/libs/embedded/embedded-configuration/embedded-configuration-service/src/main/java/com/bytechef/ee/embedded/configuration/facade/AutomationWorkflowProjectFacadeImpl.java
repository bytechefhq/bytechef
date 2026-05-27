/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.ai.copilot.service.CopilotWorkflowGenerator;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectCategoryDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectTagDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectVersionDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
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
public class AutomationWorkflowProjectFacadeImpl implements AutomationWorkflowProjectFacade {

    private static final String MARKER = "__EMBEDDED_AUTOMATION__";

    private static final String DEFAULT_DEFINITION = """
        {
            "label": "New Workflow",
            "description": "",
            "inputs": [],
            "triggers": [],
            "tasks": []
        }
        """;

    private final CategoryService categoryService;
    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectedUserProjectFacade connectedUserProjectFacade;
    private final @Nullable CopilotWorkflowGenerator copilotWorkflowGenerator;
    private final ProjectService projectService;
    private final ProjectWorkflowFacade projectWorkflowFacade;
    private final ProjectWorkflowService projectWorkflowService;
    private final TagService tagService;
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectFacadeImpl(
        CategoryService categoryService, ComponentDefinitionService componentDefinitionService,
        ConnectedUserProjectFacade connectedUserProjectFacade,
        @Nullable CopilotWorkflowGenerator copilotWorkflowGenerator, ProjectService projectService,
        ProjectWorkflowFacade projectWorkflowFacade, ProjectWorkflowService projectWorkflowService,
        TagService tagService, WorkflowNodeTestOutputService workflowNodeTestOutputService,
        WorkflowService workflowService, WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.categoryService = categoryService;
        this.componentDefinitionService = componentDefinitionService;
        this.connectedUserProjectFacade = connectedUserProjectFacade;
        this.copilotWorkflowGenerator = copilotWorkflowGenerator;
        this.projectService = projectService;
        this.projectWorkflowFacade = projectWorkflowFacade;
        this.projectWorkflowService = projectWorkflowService;
        this.tagService = tagService;
        this.workflowNodeTestOutputService = workflowNodeTestOutputService;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    public String copyWorkflowTemplate(String externalUserId, String workflowUuid, Environment environment) {
        boolean isPublishedCatalogWorkflowTemplate = getPublishedProjects()
            .stream()
            .flatMap(project -> CollectionUtils.stream(project.workflowTemplates()))
            .anyMatch(workflowTemplate -> Objects.equals(workflowTemplate.workflowUuid(), workflowUuid));

        if (!isPublishedCatalogWorkflowTemplate) {
            throw new IllegalArgumentException(
                "Not a published catalog workflow template: " + workflowUuid);
        }

        String publishedWorkflowId = projectWorkflowService.getLastPublishedWorkflowId(workflowUuid);

        Workflow workflow = workflowService.getWorkflow(publishedWorkflowId);

        return connectedUserProjectFacade.createProjectWorkflow(externalUserId, workflow.getDefinition(), environment);
    }

    @Override
    public String generateProjectWorkflow(String externalUserId, String prompt, Environment environment) {
        if (StringUtils.isBlank(prompt)) {
            throw new IllegalArgumentException("Prompt must not be blank");
        }

        if (copilotWorkflowGenerator == null) {
            throw new IllegalStateException(
                "AI Copilot is not enabled. Set bytechef.ai.copilot.enabled=true to use workflow generation.");
        }

        String workflowUuid = connectedUserProjectFacade.createProjectWorkflow(
            externalUserId, DEFAULT_DEFINITION, environment);
        String workflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);

        copilotWorkflowGenerator.generateWorkflow(workflowId, prompt);

        return workflowUuid;
    }

    @Override
    public long createProject(String name, String description, String category, List<String> tags) {
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

        return project.getId();
    }

    @Override
    public String createProjectWorkflow(long projectId, String definition) {
        getMarkedProject(projectId);

        ProjectWorkflow projectWorkflow = projectWorkflowFacade.addWorkflow(
            projectId, StringUtils.isEmpty(definition) ? DEFAULT_DEFINITION : definition);

        return projectWorkflow.getWorkflowId();
    }

    @Override
    public void deleteProject(long projectId) {
        getMarkedProject(projectId);

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(projectId);

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

        projectService.delete(projectId);
    }

    @Override
    public void deleteProjectWorkflow(String workflowUuid) {
        projectWorkflowFacade.deleteWorkflow(workflowUuid);
    }

    @Override
    public String duplicateProjectWorkflow(String workflowId) {
        ProjectWorkflow sourceProjectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        long projectId = sourceProjectWorkflow.getProjectId();

        getMarkedProject(projectId);

        Workflow sourceWorkflow = workflowService.getWorkflow(workflowId);

        String sourceDefinition = sourceWorkflow.getDefinition();

        Map<String, Object> definitionMap = JsonUtils.read(sourceDefinition, new TypeReference<>() {});

        definitionMap.compute("label", (k, label) -> (label == null ? "Workflow" : label.toString()) + " (Copy)");

        String newDefinition = JsonUtils.writeWithDefaultPrettyPrinter(definitionMap);

        ProjectWorkflow newProjectWorkflow = projectWorkflowFacade.addWorkflow(projectId, newDefinition);

        return newProjectWorkflow.getWorkflowId();
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
        return toDTO(getMarkedProject(projectId));
    }

    @Override
    public List<AutomationWorkflowProjectDTO> getProjects() {
        return projectService
            .getProjects()
            .stream()
            .filter(project -> project.getName() != null && project.getName()
                .startsWith(MARKER)
                && Objects.equals(project.getWorkspaceId(), Workspace.DEFAULT_WORKSPACE_ID))
            .map(this::toDTO)
            .toList();
    }

    @Override
    public List<AutomationWorkflowProjectDTO> getPublishedProjects() {
        return projectService
            .getProjects()
            .stream()
            .filter(project -> project.getName() != null && Strings.CS.startsWith(project.getName(), MARKER) &&
                Objects.equals(project.getWorkspaceId(), Workspace.DEFAULT_WORKSPACE_ID))
            .map(this::toPublishedDTO)
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
    }

    @Override
    public void updateProject(
        long projectId, String name, String description, String category, List<String> tags) {

        Project project = getMarkedProject(projectId);

        project.setName(MARKER + name);
        project.setDescription(description);
        project.setCategoryId(resolveCategory(category));
        project.setTagIds(resolveTags(tags));

        projectService.update(project);
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
            .map(category -> new AutomationWorkflowProjectCategoryDTO(category.getId(), category.getName()))
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
            .map(tag -> new AutomationWorkflowProjectTagDTO(tag.getId(), tag.getName()))
            .toList();
    }

    private List<ConnectedUserWorkflowTemplateDTO.Component> getTaskComponents(Workflow workflow) {
        Map<String, WorkflowNodeType> componentsByName = new LinkedHashMap<>();

        for (WorkflowTask workflowTask : workflow.getTasks(true)) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

            if (workflowNodeType.operation() != null
                && !componentsByName.containsKey(workflowNodeType.name())) {

                componentsByName.put(workflowNodeType.name(), workflowNodeType);
            }
        }

        return resolveComponents(componentsByName);
    }

    private List<ConnectedUserWorkflowTemplateDTO.Component> getTriggerComponents(Workflow workflow) {
        Map<String, WorkflowNodeType> componentsByName = new LinkedHashMap<>();

        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.of(workflow)) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            if (workflowNodeType.operation() != null
                && !componentsByName.containsKey(workflowNodeType.name())) {

                componentsByName.put(workflowNodeType.name(), workflowNodeType);
            }
        }

        return resolveComponents(componentsByName);
    }

    private List<ConnectedUserWorkflowTemplateDTO.Component> resolveComponents(
        Map<String, WorkflowNodeType> componentsByName) {

        List<ConnectedUserWorkflowTemplateDTO.Component> components = new ArrayList<>();

        for (WorkflowNodeType workflowNodeType : componentsByName.values()) {
            Optional<ComponentDefinition> componentDefinitionOptional =
                componentDefinitionService.fetchComponentDefinition(
                    workflowNodeType.name(), workflowNodeType.version());

            if (componentDefinitionOptional.isPresent()) {
                ComponentDefinition componentDefinition = componentDefinitionOptional.get();

                components.add(
                    new ConnectedUserWorkflowTemplateDTO.Component(
                        componentDefinition.getName(), componentDefinition.getTitle(), componentDefinition.getIcon()));
            }
        }

        return components;
    }

    private Project getMarkedProject(long projectId) {
        Project project = projectService.getProject(projectId);

        if (project.getName() == null || !Strings.CS.startsWith(project.getName(), MARKER)) {
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

    private AutomationWorkflowProjectDTO toDTO(Project project) {
        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(
            project.getId(), project.getLastProjectVersion());

        List<ConnectedUserWorkflowTemplateDTO> workflowTemplates = projectWorkflows.stream()
            .map(projectWorkflow -> {
                Workflow workflow = workflowService.getWorkflow(projectWorkflow.getWorkflowId());

                return new ConnectedUserWorkflowTemplateDTO(
                    projectWorkflow.getWorkflowId(), workflow.getLabel(), workflow.getDescription(),
                    Objects.toString(workflow.getLastModifiedDate(), null), getTriggerComponents(workflow),
                    getTaskComponents(workflow));
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
            project.getTagIds(), published, project.getLastProjectVersion(), lastPublishedVersion, workflowTemplates);
    }

    private AutomationWorkflowProjectDTO toPublishedDTO(Project project) {
        ProjectVersion lastPublishedProjectVersion = project.getLastPublishedProjectVersion();

        List<ConnectedUserWorkflowTemplateDTO> workflowTemplates;

        if (lastPublishedProjectVersion == null) {
            workflowTemplates = List.of();
        } else {
            List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(
                project.getId(), lastPublishedProjectVersion.getVersion());

            workflowTemplates = projectWorkflows.stream()
                .map(projectWorkflow -> {
                    Workflow workflow = workflowService.getWorkflow(projectWorkflow.getWorkflowId());

                    return new ConnectedUserWorkflowTemplateDTO(
                        projectWorkflow.getUuidAsString(), workflow.getLabel(), workflow.getDescription(),
                        Objects.toString(workflow.getLastModifiedDate(), null), getTriggerComponents(workflow),
                        getTaskComponents(workflow));
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
            project.getTagIds(), published, project.getLastProjectVersion(), lastPublishedVersion, workflowTemplates);
    }
}

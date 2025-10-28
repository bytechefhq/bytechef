/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectVersion.Status;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.SharedTemplate;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectTemplateDTO;
import com.bytechef.automation.configuration.dto.ProjectTemplateDTO.ComponentDefinitionTuple;
import com.bytechef.automation.configuration.dto.ProjectTemplateDTO.WorkflowInfo;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.dto.SharedProjectDTO;
import com.bytechef.automation.configuration.service.PreBuiltTemplateService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.SharedTemplateService;
import com.bytechef.automation.configuration.util.ComponentDefinitionHelper;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.category.service.CategoryService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.file.storage.SharedTemplateFileStorage;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Transactional
@Service
public class ProjectFacadeImpl implements ProjectFacade {

    private static final Logger logger = LoggerFactory.getLogger(ProjectFacadeImpl.class);

    private final CategoryService categoryService;
    private final ComponentDefinitionHelper componentDefinitionHelper;
    private final PreBuiltTemplateService preBuiltTemplateService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectWorkflowFacade projectWorkflowFacade;
    private final String publicUrl;
    private final SharedTemplateFileStorage sharedTemplateFileStorage;
    private final SharedTemplateService sharedTemplateService;
    private final TagService tagService;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;
    private final WorkflowNodeTestOutputService workflowNodeTestOutputService;

    @SuppressFBWarnings("EI2")
    public ProjectFacadeImpl(
        ApplicationProperties applicationProperties, CategoryService categoryService,
        ComponentDefinitionHelper componentDefinitionHelper, PreBuiltTemplateService preBuiltTemplateService,
        ProjectWorkflowService projectWorkflowService, ProjectDeploymentService projectDeploymentService,
        ProjectService projectService, ProjectDeploymentFacade projectDeploymentFacade,
        ProjectWorkflowFacade projectWorkflowFacade, SharedTemplateFileStorage sharedTemplateFileStorage,
        SharedTemplateService sharedTemplateService, TagService tagService, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService,
        WorkflowNodeTestOutputService workflowNodeTestOutputService) {

        this.categoryService = categoryService;
        this.componentDefinitionHelper = componentDefinitionHelper;
        this.preBuiltTemplateService = preBuiltTemplateService;
        this.projectWorkflowService = projectWorkflowService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectWorkflowFacade = projectWorkflowFacade;
        this.publicUrl = applicationProperties.getPublicUrl();
        this.sharedTemplateFileStorage = sharedTemplateFileStorage;
        this.sharedTemplateService = sharedTemplateService;
        this.tagService = tagService;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
        this.workflowNodeTestOutputService = workflowNodeTestOutputService;
    }

    @Override
    public long createProject(ProjectDTO projectDTO) {
        Project project = projectDTO.toProject();
        Category category = projectDTO.category();

        if (category != null) {
            category = categoryService.save(category);

            project.setCategory(category);
        }

        List<Tag> tags = checkTags(projectDTO.tags());

        if (!tags.isEmpty()) {
            project.setTags(tags);
        }

        project = projectService.create(project);

        return project.getId();
    }

    @Override
    public void deleteProject(long id) {
        List<ProjectDeployment> projectDeployments = projectDeploymentService.getProjectDeployments(id);

        for (ProjectDeployment projectDeployment : projectDeployments) {
            projectDeploymentFacade.deleteProjectDeployment(projectDeployment.getId());
        }

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(id);

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

        projectService.delete(id);

// TODO find a way to delete ll tags not referenced anymore
//        project.getTagIds()
//            .forEach(tagService::delete);
    }

    @Override
    public void deleteSharedProject(long id) {
        Project project = projectService.getProject(id);

        SharedTemplate sharedTemplate = sharedTemplateService.getSharedTemplate(
            UUID.fromString(Objects.requireNonNull(project.getUuid())));

        sharedTemplateFileStorage.deleteFile(sharedTemplate.getTemplate());

        sharedTemplate.setTemplate(null);

        sharedTemplateService.update(sharedTemplate);
    }

    @Override
    public ProjectDTO duplicateProject(long id) {
        Project project = projectService.getProject(id);

        Project newProject = new Project();

        newProject.setName(generateName(project.getName()));
        newProject.setTagIds(project.getTagIds());
        newProject.setWorkspaceId(project.getWorkspaceId());

        List<String> workflowIds = copyWorkflowIds(
            projectWorkflowService.getProjectWorkflowIds(project.getId(), project.getLastProjectVersion()));

        newProject = projectService.create(newProject);

        for (String workflowId : workflowIds) {
            projectWorkflowService.addWorkflow(newProject.getId(), newProject.getLastProjectVersion(), workflowId);
        }

        return toProjectDTO(newProject);
    }

    @Override
    public byte[] exportProject(long id) {
        return createTemplate(id, null, false);
    }

    @Override
    public void exportSharedProject(long id, String description) {
        Project project = projectService.getProject(id);

        String fileName = "project_" + project.getUuid() + ".zip";

        byte[] projectData = createTemplate(id, description, true);

        FileEntry fileEntry =
            sharedTemplateFileStorage.storeFileContent(fileName, new ByteArrayInputStream(projectData));

        sharedTemplateService.save(UUID.fromString(Objects.requireNonNull(project.getUuid())), fileEntry);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDTO getProject(long id) {
        Project project = projectService.getProject(id);

        return toProjectDTO(project);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectTemplateDTO getProjectTemplate(String id, boolean sharedProject) {
        byte[] data;

        if (sharedProject) {
            SharedTemplate sharedTemplate = sharedTemplateService.getSharedTemplate(UUID.fromString(id));

            if (sharedTemplate.getTemplate() == null) {
                throw new IllegalStateException("Shared template is not available");
            }

            try (InputStream inputStream = sharedTemplateFileStorage.getInputStream(sharedTemplate.getTemplate())) {
                data = inputStream.readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException("Failed to import shared project", e);
            }
        } else {
            data = preBuiltTemplateService.getPrebuiltTemplateData(id);
        }

        TemplateFiles templateFiles = readTemplate(data, true);

        ProjectInfo projectInfo = JsonUtils.read(templateFiles.projectJson, ProjectInfo.class);
        Template template = JsonUtils.read(templateFiles.templateJson, Template.class);

        List<Workflow> workflows = getWorkflows(templateFiles);

        List<ComponentDefinitionTuple> components = getComponentDefinitions(workflows);

        ProjectTemplateDTO.ProjectInfo project = new ProjectTemplateDTO.ProjectInfo(
            projectInfo.name, projectInfo.description);
        List<WorkflowInfo> workflowInfos = workflows.stream()
            .map(workflow -> new WorkflowInfo(workflow.getId(), workflow.getLabel(), workflow.getDescription()))
            .toList();

        List<String> categories = template.categories == null || template.categories.isEmpty()
            ? List.of("other") : template.categories;

        return new ProjectTemplateDTO(
            template.authorName, template.authorEmail, template.authorRole, template.authorSocialLinks,
            categories, components, template.description, id, template.lastModifiedDate, project,
            template.projectVersion, publicUrl, workflowInfos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectTemplateDTO> getPreBuiltProjectTemplates(String query, String category) {
        return preBuiltTemplateService.getFiles("projects")
            .stream()
            .map(fileItem -> {
                try {
                    ProjectTemplateDTO projectTemplateDTO = getProjectTemplate(
                        EncodingUtils.base64EncodeToString(fileItem.path()), false);

                    if (StringUtils.isEmpty(query) && StringUtils.isEmpty(category)) {
                        return projectTemplateDTO;
                    } else {
                        if (StringUtils.isNotEmpty(query)) {
                            ProjectTemplateDTO.ProjectInfo project = projectTemplateDTO.project();

                            if (StringUtils.containsIgnoreCase(projectTemplateDTO.description(), query) ||
                                StringUtils.containsIgnoreCase(project.name(), query) ||
                                StringUtils.containsIgnoreCase(project.description(), query)) {

                                return projectTemplateDTO;
                            }
                        }

                        if (StringUtils.isNotEmpty(category)) {
                            List<String> categories = projectTemplateDTO.categories();

                            if (categories != null && categories.contains(category)) {
                                return projectTemplateDTO;
                            }
                        }

                        return null;
                    }
                } catch (Exception e) {
                    logger.error("Failed to get workflow template", e);

                    return null;
                }

            })
            .filter(Objects::nonNull)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDTO> getProjects(Long categoryId, Boolean projectDeployments, Long tagId, Status status) {
        return getProjects(null, categoryId, tagId, projectDeployments, status, true, null);
    }

    @Override
    @Transactional(readOnly = true)
    public SharedProjectDTO getSharedProject(String projectUuid) {
        SharedProjectDTO sharedProjectDTO = null;
        Optional<SharedTemplate> sharedTemplateOptional = sharedTemplateService.fetchSharedTemplate(
            UUID.fromString(projectUuid));

        if (sharedTemplateOptional.isPresent()) {
            SharedTemplate sharedTemplate = sharedTemplateOptional.get();

            if (sharedTemplate.getTemplate() == null) {
                sharedProjectDTO = new SharedProjectDTO(false);
            } else {
                try (InputStream inputStream = sharedTemplateFileStorage.getInputStream(sharedTemplate.getTemplate())) {
                    TemplateFiles templateFiles = readTemplate(inputStream.readAllBytes(), true);

                    Template template = JsonUtils.read(templateFiles.templateJson, Template.class);

                    sharedProjectDTO = new SharedProjectDTO(
                        template.description, true, template.projectVersion, publicUrl);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to import shared project", e);
                }
            }
        }

        return sharedProjectDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectDTO> getWorkspaceProjects(
        Boolean apiCollections, Long categoryId, boolean includeAllFields, Boolean projectDeployments, Status status,
        Long tagId, long workspaceId) {

        return getProjects(
            apiCollections, categoryId, tagId, projectDeployments, status, includeAllFields, workspaceId);
    }

    @Override
    public List<ProjectWorkflowDTO> getWorkspaceProjectWorkflows(long workspaceId) {
        List<Long> projectIds = projectService.getProjects(null, null, null, null, null, workspaceId)
            .stream()
            .map(Project::getId)
            .toList();

        return projectWorkflowService.getProjectWorkflows(projectIds)
            .stream()
            .map(projectWorkflow -> new ProjectWorkflowDTO(
                workflowService.getWorkflow(projectWorkflow.getWorkflowId()), projectWorkflow))
            .toList();
    }

    @Override
    public long importProject(byte[] projectData, long workspaceId) {
        return importProjectTemplate(projectData, workspaceId);
    }

    @Override
    public long importProjectTemplate(String id, long workspaceId, boolean sharedProject) {
        byte[] data;

        if (sharedProject) {
            SharedTemplate sharedTemplate = sharedTemplateService.getSharedTemplate(UUID.fromString(id));

            try (InputStream inputStream = sharedTemplateFileStorage.getInputStream(sharedTemplate.getTemplate())) {
                data = inputStream.readAllBytes();
            } catch (IOException e) {
                throw new RuntimeException("Failed to import shared project", e);
            }
        } else {
            data = preBuiltTemplateService.getPrebuiltTemplateData(id);
        }

        return importProjectTemplate(data, workspaceId);
    }

    @Override
    public int publishProject(long id, String description, boolean syncWithGit) {
        Project project = projectService.getProject(id);

        int oldProjectVersion = project.getLastProjectVersion();

        List<ProjectWorkflow> oldProjectWorkflows = projectWorkflowService.getProjectWorkflows(
            project.getId(), oldProjectVersion);

        int newProjectVersion = projectService.publishProject(id, description, syncWithGit);

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
    public void updateProject(ProjectDTO projectDTO) {
        List<Tag> tags = checkTags(projectDTO.tags());

        Project project = projectDTO.toProject();
        Category category = projectDTO.category();

        if (category != null) {
            category = categoryService.save(category);

            project.setCategory(category);
        }

        project.setTags(tags);

        projectService.update(project);
    }

    private List<Tag> checkTags(List<Tag> tags) {
        return CollectionUtils.isEmpty(tags) ? Collections.emptyList() : tagService.save(tags);
    }

    private List<String> copyWorkflowIds(List<String> workflowIds) {
        List<String> newWorkflowIds = new ArrayList<>();

        for (String workflowId : workflowIds) {
            Workflow workflow = workflowService.getWorkflow(workflowId);

            workflow = workflowService.create(
                workflow.getDefinition(), workflow.getFormat(), workflow.getSourceType());

            newWorkflowIds.add(workflow.getId());
        }
        return newWorkflowIds;
    }

    private byte[] createTemplate(long id, String description, boolean sharedTemplate) {
        Project project = projectService.getProject(id);

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getProjectWorkflows(
            id, project.getLastProjectVersion());

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {

            if (sharedTemplate) {
                ZipEntry zipEntry = new ZipEntry("template.json");

                zipOutputStream.putNextEntry(zipEntry);

                String templateJson = JsonUtils.write(new Template(description, project.getLastProjectVersion()));

                zipOutputStream.write(templateJson.getBytes(StandardCharsets.UTF_8));

                zipOutputStream.closeEntry();
            }

            ZipEntry projectZipEntry = new ZipEntry("project.json");

            zipOutputStream.putNextEntry(projectZipEntry);

            String projectJson = JsonUtils.write(new ProjectInfo(project));

            zipOutputStream.write(projectJson.getBytes(StandardCharsets.UTF_8));

            zipOutputStream.closeEntry();

            for (ProjectWorkflow projectWorkflow : projectWorkflows) {
                Workflow workflow = workflowService.getWorkflow(projectWorkflow.getWorkflowId());

                ZipEntry workflowZipEntry = new ZipEntry(String.format("workflow-%s.json", projectWorkflow.getUuid()));

                zipOutputStream.putNextEntry(workflowZipEntry);

                String workflowJson = JsonUtils.write(workflow.getDefinition());

                zipOutputStream.write(workflowJson.getBytes(StandardCharsets.UTF_8));

                zipOutputStream.closeEntry();
            }

            zipOutputStream.finish();

            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to export project", e);
        }
    }

    private String generateName(String oldName) {
        List<Project> projects = projectService.getProjects();
        int addendum = 0;

        for (Project curProject : projects) {
            String name = curProject.getName();

            if (name.startsWith(oldName)) {
                addendum++;
            }
        }

        return oldName + " (%s)".formatted(addendum);
    }

    private Category getCategory(Project project) {
        return project.getCategoryId() == null ? null : categoryService.getCategory(project.getCategoryId());
    }

    private List<ProjectDTO> getProjects(
        Boolean apiCollections, Long categoryId, Long tagId, Boolean projectDeployments, Status status,
        boolean includeAllFields, Long workspaceId) {

        List<Project> projects = projectService.getProjects(
            apiCollections, categoryId, projectDeployments, tagId, status, workspaceId);

        if (includeAllFields) {
            return CollectionUtils.map(
                projects,
                project -> new ProjectDTO(
                    CollectionUtils.findFirstFilterOrElse(
                        categoryService.getCategories(
                            projects.stream()
                                .map(Project::getCategoryId)
                                .filter(Objects::nonNull)
                                .toList()),
                        category -> Objects.equals(project.getCategoryId(), category.getId()),
                        null),
                    project,
                    projectWorkflowService.getProjectProjectWorkflowIds(project.getId(),
                        project.getLastProjectVersion()),
                    CollectionUtils.filter(
                        tagService.getTags(
                            projects.stream()
                                .flatMap(curProject -> CollectionUtils.stream(curProject.getTagIds()))
                                .filter(Objects::nonNull)
                                .toList()),
                        tag -> CollectionUtils.contains(project.getTagIds(), tag.getId()))));
        } else {
            return CollectionUtils.map(projects, ProjectDTO::new);
        }
    }

    private List<Workflow> getWorkflows(TemplateFiles templateFiles) {
        List<Workflow> workflows = new ArrayList<>();

        for (String workflowJson : templateFiles.workflowJsons) {
            workflows.add(
                new Workflow(
                    String.valueOf(UUID.randomUUID()), JsonUtils.read(workflowJson, String.class),
                    Workflow.Format.JSON));
        }

        return workflows;
    }

    private List<ComponentDefinitionTuple> getComponentDefinitions(List<Workflow> workflows) {
        List<ComponentDefinitionTuple> componentDefinitions = new ArrayList<>();

        for (Workflow workflow : workflows) {
            String id = Objects.requireNonNull(workflow.getId());

            componentDefinitions.add(
                new ComponentDefinitionTuple(id, componentDefinitionHelper.getComponentDefinitions(workflow)));
        }

        return componentDefinitions;
    }

    private long importProjectTemplate(byte[] projectData, long workspaceId) {
        TemplateFiles templateFiles = readTemplate(projectData, false);

        ProjectInfo projectInfo = JsonUtils.read(templateFiles.projectJson, ProjectInfo.class);

        Project project = new Project();

        project.setName(projectInfo.name());
        project.setDescription(projectInfo.description());
        project.setWorkspaceId(workspaceId);

        ProjectDTO projectDTO = new ProjectDTO(project);

        long projectId = createProject(projectDTO);

        for (String workflowJson : templateFiles.workflowJsons) {
            String definition = JsonUtils.read(workflowJson, String.class);

            projectWorkflowFacade.addWorkflow(projectId, definition);
        }

        return projectId;
    }

    private TemplateFiles readTemplate(byte[] data, boolean sharedTemplate) {
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(data))) {
            String projectJson = null;
            String templateJson = null;
            ZipEntry zipEntry;
            List<String> workflowJsons = new ArrayList<>();

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                byte[] entryData = zipInputStream.readAllBytes();

                String name = zipEntry.getName();

                if ("project.json".equals(name)) {
                    projectJson = new String(entryData, StandardCharsets.UTF_8);
                } else if ("template.json".equals(name)) {
                    templateJson = new String(entryData, StandardCharsets.UTF_8);
                } else if (name.startsWith("workflow-") && name.endsWith(".json")) {
                    workflowJsons.add(new String(entryData, StandardCharsets.UTF_8));
                }

                zipInputStream.closeEntry();
            }

            if (projectJson == null || sharedTemplate && (templateJson == null) || workflowJsons.isEmpty()) {
                throw new RuntimeException("Missing files in a shared project file");
            }

            return new TemplateFiles(templateJson, projectJson, workflowJsons);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shared project", e);
        }
    }

    private ProjectDTO toProjectDTO(Project project) {
        return new ProjectDTO(
            getCategory(project), project,
            projectWorkflowService.getProjectProjectWorkflowIds(project.getId(), project.getLastProjectVersion()),
            tagService.getTags(project.getTagIds()));
    }

    record ProjectInfo(String name, String description) {

        ProjectInfo(Project project) {
            this(project.getName(), project.getDescription());
        }
    }

    private record Template(
        String authorName, String authorRole, String authorEmail, String authorSocialLinks, List<String> categories,
        String description, Instant lastModifiedDate, Integer projectVersion) {

        public Template(String description, int lastProjectVersion) {
            this(null, null, null, null, Collections.emptyList(), description, Instant.now(), lastProjectVersion);
        }
    }

    private record TemplateFiles(String templateJson, String projectJson, List<String> workflowJsons) {
    }
}

/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.SystemProjects;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.project.ProjectHandler;
import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.configuration.domain.ProjectCodeWorkflow;
import com.bytechef.ee.automation.configuration.exception.CodeWorkflowErrorType;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowDraftService;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.category.domain.Category;
import com.bytechef.platform.codeworkflow.loader.automation.ProjectHandlerLoader;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.tag.domain.Tag;
import com.bytechef.platform.tag.service.TagService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class ProjectCodeWorkflowFacadeImpl implements ProjectCodeWorkflowFacade {

    private static final String EMBEDDED_AUTOMATION_MARKER = SystemProjects.EMBEDDED_AUTOMATION_NAME_PREFIX;

    private final CacheManager cacheManager;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final ProjectFacade projectFacade;
    private final ProjectCodeWorkflowService projectCodeWorkflowService;
    private final CodeWorkflowContainerService codeWorkflowContainerService;
    private final CodeWorkflowFileStorage codeWorkflowFileStorage;
    private final TagService tagService;
    private final ProjectCodeWorkflowDraftService projectCodeWorkflowDraftService;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final boolean javaEnabled;

    @SuppressFBWarnings("EI")
    public ProjectCodeWorkflowFacadeImpl(
        ApplicationProperties applicationProperties, CacheManager cacheManager, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, ProjectFacade projectFacade,
        ProjectCodeWorkflowService projectCodeWorkflowService,
        CodeWorkflowContainerService codeWorkflowContainerService,
        CodeWorkflowFileStorage codeWorkflowFileStorage,
        ProjectCodeWorkflowDraftService projectCodeWorkflowDraftService, TagService tagService,
        ProjectDeploymentFacade projectDeploymentFacade, ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService) {

        this.cacheManager = cacheManager;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.projectFacade = projectFacade;
        this.projectCodeWorkflowService = projectCodeWorkflowService;
        this.codeWorkflowContainerService = codeWorkflowContainerService;
        this.codeWorkflowFileStorage = codeWorkflowFileStorage;
        this.projectCodeWorkflowDraftService = projectCodeWorkflowDraftService;
        this.tagService = tagService;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.javaEnabled = applicationProperties.getWorkflow()
            .getCodeWorkflow()
            .isJavaEnabled();
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public Project createEmptyCodeWorkflow(long workspaceId, String name, Language language) {
        return createEmptyCodeWorkflow(workspaceId, name, language, null, null, null);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public Project createEmptyCodeWorkflow(
        long workspaceId, String name, Language language, @Nullable String description, @Nullable Long categoryId,
        @Nullable List<String> tags) {
        validateName(name);

        if (language != Language.JAVASCRIPT && language != Language.PYTHON) {
            throw new ConfigurationException(
                "Create-empty supports JavaScript and Python only",
                CodeWorkflowErrorType.LANGUAGE_NOT_SUPPORTED);
        }

        if (projectService.fetchProject(name, workspaceId)
            .isPresent()) {

            throw new ConfigurationException(
                "A project named '" + name + "' already exists",
                CodeWorkflowErrorType.CODE_WORKFLOW_ALREADY_EXISTS);
        }

        String template = readTemplate(language).replace("__NAME__", name);

        byte[] bytes = template.getBytes(StandardCharsets.UTF_8);

        ProjectDefinition projectDefinition;

        try {
            projectDefinition = loadProjectDefinition(language, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Project project = createProject(workspaceId, projectDefinition, description, categoryId, tags);

        projectCodeWorkflowDraftService.saveDraft(project, projectDefinition, bytes, language);

        return project;
    }

    @Transactional(readOnly = true)
    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<Project> getCodeWorkflowProjects() {
        List<Long> projectIds = projectCodeWorkflowService.getCodeWorkflowProjectIds();

        if (projectIds.isEmpty()) {
            return List.of();
        }

        return projectService.getProjects(projectIds)
            .stream()
            .filter(project -> !isEmbeddedBridgeProject(project))
            .toList();
    }

    @Transactional(readOnly = true)
    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public String getCodeWorkflowSource(long projectId) {
        CodeWorkflowContainer codeWorkflowContainer = getCodeWorkflowContainer(projectId);

        if (codeWorkflowContainer.getLanguage() == Language.JAVA) {
            throw new ConfigurationException(
                "Java code workflows have no editable source", CodeWorkflowErrorType.LANGUAGE_NOT_SUPPORTED);
        }

        return codeWorkflowFileStorage.readCodeWorkflowFileContent(codeWorkflowContainer.getWorkflows());
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void save(long workspaceId, byte[] bytes, Language language) {
        publish(workspaceId, bytes, language);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void save(long workspaceId, byte[] bytes, Language language, Environment environment, boolean enable) {
        PublishedProject publishedProject = publish(workspaceId, bytes, language);

        if (enable) {
            enableProjectVersion(publishedProject.project(), publishedProject.projectVersion(), environment);
        }
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void updateCodeWorkflowSource(long projectId, String content) {
        CodeWorkflowContainer codeWorkflowContainer = getCodeWorkflowContainer(projectId);

        Language language = codeWorkflowContainer.getLanguage();

        if (language == Language.JAVA) {
            throw new ConfigurationException(
                "Java code workflows have no editable source", CodeWorkflowErrorType.LANGUAGE_NOT_SUPPORTED);
        }

        Project project = projectService.getProject(projectId);

        if (isEmbeddedBridgeProject(project)) {
            throw new ConfigurationException(
                "Embedded-bridge catalog projects cannot be edited in the code editor; redeploy them through the " +
                    "embedded deploy endpoint instead",
                CodeWorkflowErrorType.EMBEDDED_BRIDGE_PROJECT_NOT_EDITABLE);
        }

        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        ProjectDefinition projectDefinition;

        try {
            projectDefinition = loadProjectDefinition(language, bytes);
        } catch (Exception e) {
            throw new ConfigurationException(
                "Failed to load code workflow source: " + e.getMessage(), CodeWorkflowErrorType.SOURCE_LOAD_FAILED);
        }

        if (!Objects.equals(projectDefinition.getName(), project.getName())) {
            throw new ConfigurationException(
                "Renaming a code workflow by editing its source is not supported (expected name '"
                    + project.getName() + "')",
                CodeWorkflowErrorType.CODE_WORKFLOW_NAME_MISMATCH);
        }

        projectCodeWorkflowDraftService.saveDraft(project, projectDefinition, bytes, language);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void deployCodeWorkflowSource(long projectId, Language language, String content) {
        if (language == Language.JAVA) {
            throw new ConfigurationException(
                "Java code workflows have no editable source", CodeWorkflowErrorType.LANGUAGE_NOT_SUPPORTED);
        }

        Project project = projectService.getProject(projectId);

        String projectName = project.getName();

        validateName(projectName);

        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        ProjectDefinition projectDefinition = readProjectDefinition(language, bytes);

        if (!Objects.equals(projectDefinition.getName(), projectName)) {
            content = rewriteDeclaredName(content, projectDefinition.getName(), projectName);

            bytes = content.getBytes(StandardCharsets.UTF_8);

            projectDefinition = readProjectDefinition(language, bytes);

            if (!Objects.equals(projectDefinition.getName(), projectName)) {
                throw new ConfigurationException(
                    "Could not rename the code workflow to '" + projectName +
                        "'; declare the project name as a plain string literal",
                    CodeWorkflowErrorType.CODE_WORKFLOW_NAME_MISMATCH);
            }
        }

        projectCodeWorkflowDraftService.saveDraft(project, projectDefinition, bytes, language);
    }

    private PublishedProject publish(long workspaceId, byte[] bytes, Language language) {
        if (language == Language.RUBY) {
            throw new ConfigurationException(
                "Uploading of Ruby code workflows is temporarily disabled",
                CodeWorkflowErrorType.LANGUAGE_NOT_SUPPORTED);
        }

        if (!javaEnabled && language == Language.JAVA) {
            throw new ConfigurationException(
                "Uploading of Java code workflows is disabled",
                CodeWorkflowErrorType.JAVA_CODE_WORKFLOW_UPLOAD_DISABLED);
        }

        ProjectDefinition projectDefinition;

        try {
            projectDefinition = loadProjectDefinition(language, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Project project = projectService.fetchProject(projectDefinition.getName(), workspaceId)
            .map(curProject -> updateProject(curProject, projectDefinition))
            .orElseGet(() -> createProject(workspaceId, projectDefinition));

        int projectVersion = deployInto(project, projectDefinition, bytes, language);

        return new PublishedProject(project, projectVersion);
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.indexOf('"') >= 0 || name.indexOf('\\') >= 0
            || name.indexOf('\n') >= 0 || name.indexOf('\r') >= 0) {

            throw new ConfigurationException(
                "Invalid code workflow name: must not be blank or contain quotes, backslashes, or newlines",
                CodeWorkflowErrorType.INVALID_CODE_WORKFLOW_NAME);
        }
    }

    private ProjectDefinition readProjectDefinition(Language language, byte[] bytes) {
        try {
            return loadProjectDefinition(language, bytes);
        } catch (Exception e) {
            throw new ConfigurationException(
                "Failed to load code workflow source: " + e.getMessage(), CodeWorkflowErrorType.SOURCE_LOAD_FAILED);
        }
    }

    private static String rewriteDeclaredName(String content, String declaredName, String newName) {
        for (String quote : List.of("\"", "'")) {
            String literal = quote + declaredName + quote;

            int index = content.indexOf(literal);

            if (index >= 0) {
                return content.substring(0, index) + quote + newName + quote +
                    content.substring(index + literal.length());
            }
        }

        return content;
    }

    private int deployInto(Project project, ProjectDefinition projectDefinition, byte[] bytes, Language language) {
        int projectVersion = project.getLastProjectVersion();

        projectCodeWorkflowDraftService.saveDraft(project, projectDefinition, bytes, language);

        projectFacade.publishProject(Objects.requireNonNull(project.getId()), null, false);

        return projectVersion;
    }

    private void enableProjectVersion(Project project, int projectVersion, Environment environment) {
        long projectId = Objects.requireNonNull(project.getId());

        Optional<ProjectDeployment> projectDeployment = projectDeploymentService.fetchProjectDeployment(
            projectId, environment);

        if (projectDeployment.isPresent()) {
            moveProjectDeployment(projectDeployment.get(), projectVersion, environment);
        } else {
            createEnabledProjectDeployment(project, projectVersion, environment);
        }
    }

    private void createEnabledProjectDeployment(Project project, int projectVersion, Environment environment) {
        long projectId = Objects.requireNonNull(project.getId());

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setEnvironment(environment);
        projectDeployment.setName(project.getName());
        projectDeployment.setProjectId(projectId);
        projectDeployment.setProjectVersion(projectVersion);

        List<String> workflowIds = projectWorkflowService.getProjectWorkflows(projectId, projectVersion)
            .stream()
            .map(ProjectWorkflow::getWorkflowId)
            .toList();

        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows = workflowIds.stream()
            .map(ProjectCodeWorkflowFacadeImpl::toProjectDeploymentWorkflow)
            .toList();

        long projectDeploymentId = projectDeploymentFacade.createProjectDeployment(
            projectDeployment, projectDeploymentWorkflows, List.of());

        for (String workflowId : workflowIds) {
            projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentId, workflowId, true);
        }

        projectDeploymentFacade.enableProjectDeployment(projectDeploymentId, true);
    }

    private void moveProjectDeployment(
        ProjectDeployment projectDeployment, int projectVersion, Environment environment) {

        long projectDeploymentId = Objects.requireNonNull(projectDeployment.getId());
        long projectId = Objects.requireNonNull(projectDeployment.getProjectId());

        List<ProjectWorkflow> allProjectWorkflows = projectWorkflowService.getProjectWorkflows(projectId);

        Map<String, String> workflowIdUuids = allProjectWorkflows.stream()
            .collect(
                Collectors.toMap(
                    ProjectWorkflow::getWorkflowId, ProjectWorkflow::getUuidAsString,
                    (firstUuid, secondUuid) -> firstUuid));

        Set<String> deployedWorkflowUuids = projectDeploymentWorkflowService
            .getProjectDeploymentWorkflows(projectDeploymentId)
            .stream()
            .map(projectDeploymentWorkflow -> workflowIdUuids.get(projectDeploymentWorkflow.getWorkflowId()))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        List<String> newWorkflowIds = allProjectWorkflows.stream()
            .filter(projectWorkflow -> projectWorkflow.getProjectVersion() == projectVersion)
            .filter(projectWorkflow -> !deployedWorkflowUuids.contains(projectWorkflow.getUuidAsString()))
            .map(ProjectWorkflow::getWorkflowId)
            .toList();

        projectDeploymentFacade.updateProjectDeployment(
            projectId, projectVersion, null, List.of(), (long) environment.ordinal());

        for (String newWorkflowId : newWorkflowIds) {
            projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentId, newWorkflowId, true);
        }

        if (!projectDeployment.isEnabled()) {
            projectDeploymentFacade.enableProjectDeployment(projectDeploymentId, true);
        }
    }

    private static ProjectDeploymentWorkflow toProjectDeploymentWorkflow(String workflowId) {
        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setConnections(List.of());
        projectDeploymentWorkflow.setInputs(Map.of());
        projectDeploymentWorkflow.setWorkflowId(workflowId);

        return projectDeploymentWorkflow;
    }

    private CodeWorkflowContainer getCodeWorkflowContainer(long projectId) {
        ProjectCodeWorkflow projectCodeWorkflow = projectCodeWorkflowService.getProjectCodeWorkflow(projectId);

        return codeWorkflowContainerService.getCodeWorkflowContainer(projectCodeWorkflow.getCodeWorkflowContainerId());
    }

    private static boolean isEmbeddedBridgeProject(Project project) {
        String name = project.getName();

        return name != null && name.startsWith(EMBEDDED_AUTOMATION_MARKER);
    }

    private Project createProject(long workspaceId, ProjectDefinition projectDefinition) {
        return createProject(workspaceId, projectDefinition, null, null, null);
    }

    private Project createProject(
        long workspaceId, ProjectDefinition projectDefinition, @Nullable String description,
        @Nullable Long categoryId, @Nullable List<String> tags) {

        Project project = new Project();

        project.setDescription(
            description == null || description.isBlank()
                ? projectDefinition.getDescription()
                    .orElse(null)
                : description);
        project.setName(projectDefinition.getName());
        project.setWorkspaceId(workspaceId);

        if (categoryId != null) {
            project.setCategory(new Category(categoryId));
        }

        if (tags != null && !tags.isEmpty()) {
            project.setTags(
                tagService.save(
                    tags.stream()
                        .map(Tag::new)
                        .toList()));
        }

        return projectService.create(project);
    }

    /**
     * Security Note: PATH_TRAVERSAL_IN - Temporary files are created with system-generated names in the temp directory,
     * not user-controlled paths. Access is restricted to administrators.
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    private ProjectDefinition loadProjectDefinition(Language language, byte[] bytes) throws IOException {
        Path path = Files.createTempFile("code_workflow_project", language.getExtension());

        Files.write(path, bytes);

        URI uri = path.toUri();

        try {
            ProjectHandler projectHandler = ProjectHandlerLoader.loadProjectHandler(
                uri.toURL(), language, uri.toString() + UUID.randomUUID(), cacheManager);

            return projectHandler.getDefinition();
        } finally {
            Files.delete(path);
        }
    }

    private Project updateProject(Project project, ProjectDefinition projectDefinition) {
        project.setDescription(
            projectDefinition.getDescription()
                .orElse(null));

        return projectService.update(project);
    }

    private static String readTemplate(Language language) {
        String resource = "code-workflow-templates/starter." + language.getExtension();

        try (InputStream inputStream = ProjectCodeWorkflowFacadeImpl.class.getClassLoader()
            .getResourceAsStream(resource)) {

            if (inputStream == null) {
                throw new IllegalStateException("Missing starter template: " + resource);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private record PublishedProject(Project project, int projectVersion) {
    }
}

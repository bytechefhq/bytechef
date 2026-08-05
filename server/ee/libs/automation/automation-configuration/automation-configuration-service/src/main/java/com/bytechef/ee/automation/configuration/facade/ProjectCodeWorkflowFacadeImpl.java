/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.project.ProjectHandler;
import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Workflow.CodeWorkflow;
import com.bytechef.ee.automation.configuration.domain.ProjectCodeWorkflow;
import com.bytechef.ee.automation.configuration.exception.CodeWorkflowErrorType;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.codeworkflow.loader.automation.ProjectHandlerLoader;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
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

    /**
     * Embedded-bridge catalog projects are name-prefixed with this marker (owned by the embedded module's
     * {@code AutomationWorkflowProjectFacadeImpl}). They are deliberately kept out of the code editor: editor draft
     * saves would interleave containers into the same {@code project_code_workflow} table the bridge's one-deploy-back
     * uuid carry-forward reads, minting new workflow uuids on the next bridge redeploy and permanently dangling
     * connected-user references.
     */
    private static final String EMBEDDED_AUTOMATION_MARKER = "__EMBEDDED_AUTOMATION__";

    private final CacheManager cacheManager;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final CodeWorkflowContainerFacade codeWorkflowContainerFacade;
    private final ProjectCodeWorkflowService projectCodeWorkflowService;
    private final CodeWorkflowContainerService codeWorkflowContainerService;
    private final CodeWorkflowFileStorage codeWorkflowFileStorage;
    private final WorkflowService workflowService;
    private final boolean javaEnabled;
    private final ProjectHandlerLoader.JavaLoader javaLoader;

    @SuppressFBWarnings("EI")
    public ProjectCodeWorkflowFacadeImpl(
        ApplicationProperties applicationProperties, CacheManager cacheManager, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, CodeWorkflowContainerFacade codeWorkflowContainerFacade,
        ProjectCodeWorkflowService projectCodeWorkflowService,
        CodeWorkflowContainerService codeWorkflowContainerService,
        CodeWorkflowFileStorage codeWorkflowFileStorage, WorkflowService workflowService) {

        this.cacheManager = cacheManager;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.codeWorkflowContainerFacade = codeWorkflowContainerFacade;
        this.projectCodeWorkflowService = projectCodeWorkflowService;
        this.codeWorkflowContainerService = codeWorkflowContainerService;
        this.codeWorkflowFileStorage = codeWorkflowFileStorage;
        this.workflowService = workflowService;
        this.javaEnabled = applicationProperties.getWorkflow()
            .getCodeWorkflow()
            .isJavaEnabled();
        this.javaLoader = toLoaderJavaLoader(applicationProperties);
    }

    private static ProjectHandlerLoader.JavaLoader toLoaderJavaLoader(ApplicationProperties applicationProperties) {
        ApplicationProperties.Workflow workflow = applicationProperties.getWorkflow();

        CodeWorkflow codeWorkflow = workflow.getCodeWorkflow();

        return codeWorkflow.getJavaLoader() == CodeWorkflow.JavaLoader.ESPRESSO
            ? ProjectHandlerLoader.JavaLoader.ESPRESSO
            : ProjectHandlerLoader.JavaLoader.CLASS_LOADER;
    }

    /**
     * Creates a code-backed project from scratch by rendering the language starter template (substituting the requested
     * project name), creating the project, and saving the rendered script as a draft (never publishing it). Restricted
     * to administrators, mirroring {@link #save}, because loading the rendered script runs it on the server.
     *
     * <p>
     * This method is create-only: a project of the requested name is rejected up front via a global, case-insensitive
     * {@link ProjectService#fetchProject(String)} lookup, so an existing project (in any workspace, code- or
     * visual-backed) is never reused or overwritten.
     */
    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public Project createEmptyCodeWorkflow(long workspaceId, String name, Language language) {
        if (name == null || name.isBlank() || name.indexOf('"') >= 0 || name.indexOf('\\') >= 0
            || name.indexOf('\n') >= 0 || name.indexOf('\r') >= 0) {

            throw new ConfigurationException(
                "Invalid code workflow name: must not be blank or contain quotes, backslashes, or newlines",
                CodeWorkflowErrorType.INVALID_CODE_WORKFLOW_NAME);
        }

        if (language != Language.JAVASCRIPT && language != Language.PYTHON && language != Language.RUBY) {
            throw new ConfigurationException(
                "Create-empty supports JavaScript, Python and Ruby only",
                CodeWorkflowErrorType.LANGUAGE_NOT_SUPPORTED);
        }

        if (projectService.fetchProject(name)
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

        Project project = createProject(workspaceId, projectDefinition);

        saveDraft(project, projectDefinition, bytes, language);

        return project;
    }

    /**
     * Returns every project that has at least one code workflow deployed, resolved from the distinct project ids
     * recorded on {@code project_code_workflow}.
     */
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

    /**
     * Returns the stored source text of the code workflow backing {@code projectId}, so it can be shown in an editor.
     * Java-backed containers have no editable source (they are compiled jars), so those are rejected.
     */
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

    /**
     * Deploying a code workflow loads and executes the uploaded artifact (a JAR or polyglot script) on the server, so
     * it is restricted to administrators. The guard lives here on the facade so it protects every caller, not only the
     * REST entry point.
     */
    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void save(long workspaceId, byte[] bytes, Language language) {
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

        Project project = projectService.fetchProject(projectDefinition.getName())
            .map(curProject -> updateProject(curProject, projectDefinition))
            .orElseGet(() -> createProject(workspaceId, projectDefinition));

        deployInto(project, projectDefinition, bytes, language);
    }

    /**
     * Re-deploys new source onto an already-resolved {@code project} rather than resolving the target project by name
     * (as {@link #save} does for uploads). Renaming a project by editing its source is not supported, so the caller
     * must have already verified the incoming {@link ProjectDefinition#getName()} matches {@code project}'s name.
     */
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

        saveDraft(project, projectDefinition, bytes, language);
    }

    private void saveDraft(Project project, ProjectDefinition projectDefinition, byte[] bytes, Language language) {
        long projectId = Objects.requireNonNull(project.getId());
        int draftProjectVersion = project.getLastProjectVersion();

        ProjectCodeWorkflow latestProjectCodeWorkflow = projectCodeWorkflowService.fetchProjectCodeWorkflow(projectId)
            .orElse(null);

        CodeWorkflowContainerFacade.CodeWorkflowReconciliation reconciliation;

        if (latestProjectCodeWorkflow != null && latestProjectCodeWorkflow.getProjectVersion() == draftProjectVersion) {
            CodeWorkflowContainer codeWorkflowContainer = codeWorkflowContainerService.getCodeWorkflowContainer(
                latestProjectCodeWorkflow.getCodeWorkflowContainerId());

            reconciliation = codeWorkflowContainerFacade.update(
                codeWorkflowContainer, projectDefinition.getVersion(), projectDefinition.getWorkflows(), bytes,
                PlatformType.AUTOMATION);
        } else {
            Map<String, String> reusableWorkflowNameIds = latestProjectCodeWorkflow == null
                ? Map.of()
                : resolveDraftWorkflowNameIds(latestProjectCodeWorkflow, projectId, draftProjectVersion);

            reconciliation = codeWorkflowContainerFacade.create(
                projectDefinition.getName(), projectDefinition.getVersion(), projectDefinition.getWorkflows(),
                language, bytes, PlatformType.AUTOMATION, reusableWorkflowNameIds);

            projectCodeWorkflowService.create(reconciliation.codeWorkflowContainer(), project);
        }

        for (String workflowId : reconciliation.addedWorkflowNameIds()
            .values()) {

            projectWorkflowService.addWorkflow(projectId, draftProjectVersion, workflowId);
        }

        for (String workflowId : reconciliation.removedWorkflowNameIds()
            .values()) {

            projectWorkflowService.delete(projectId, draftProjectVersion, workflowId);
            workflowService.delete(workflowId);
        }
    }

    /**
     * Maps the published container's workflow names onto the current draft version's workflow ids. The facade publish
     * duplicates each workflow into the new draft version under a new workflow id but preserves the ProjectWorkflow
     * uuid across both rows, so the chain is: published container name -> published workflow id -> row uuid at the
     * container's (published) version -> draft-version row with the same uuid -> draft workflow id.
     */
    private Map<String, String> resolveDraftWorkflowNameIds(
        ProjectCodeWorkflow publishedProjectCodeWorkflow, long projectId, int draftProjectVersion) {

        CodeWorkflowContainer publishedCodeWorkflowContainer = codeWorkflowContainerService.getCodeWorkflowContainer(
            publishedProjectCodeWorkflow.getCodeWorkflowContainerId());

        Map<String, String> publishedWorkflowNameIds = publishedCodeWorkflowContainer.getWorkflowNameIds();

        Map<String, String> workflowIdUuids = projectWorkflowService.getProjectWorkflows(
            projectId, publishedProjectCodeWorkflow.getProjectVersion())
            .stream()
            .collect(Collectors.toMap(ProjectWorkflow::getWorkflowId, ProjectWorkflow::getUuidAsString));

        Map<String, String> uuidDraftWorkflowIds = projectWorkflowService.getProjectWorkflows(
            projectId, draftProjectVersion)
            .stream()
            .collect(Collectors.toMap(ProjectWorkflow::getUuidAsString, ProjectWorkflow::getWorkflowId));

        Map<String, String> draftWorkflowNameIds = new HashMap<>();

        for (Map.Entry<String, String> entry : publishedWorkflowNameIds.entrySet()) {
            String uuid = workflowIdUuids.get(entry.getValue());

            String draftWorkflowId = uuid == null ? null : uuidDraftWorkflowIds.get(uuid);

            if (draftWorkflowId != null) {
                draftWorkflowNameIds.put(entry.getKey(), draftWorkflowId);
            }
        }

        return draftWorkflowNameIds;
    }

    private void deployInto(Project project, ProjectDefinition projectDefinition, byte[] bytes, Language language) {
        CodeWorkflowContainer codeWorkflowContainer = codeWorkflowContainerFacade.create(
            projectDefinition.getName(), projectDefinition.getVersion(), projectDefinition.getWorkflows(),
            language, bytes, PlatformType.AUTOMATION);

        projectCodeWorkflowService.create(codeWorkflowContainer, project);

        Map<String, String> workflowNameIds = codeWorkflowContainer.getWorkflowNameIds();

        for (Map.Entry<String, String> entry : workflowNameIds.entrySet()) {
            projectWorkflowService.addWorkflow(project.getId(), project.getLastProjectVersion(), entry.getValue());
        }

        projectService.publishProject(project.getId(), null, false);
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
        Project project = new Project();

        project.setDescription(
            projectDefinition.getDescription()
                .orElse(null));
        project.setName(projectDefinition.getName());
        project.setWorkspaceId(workspaceId);

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
                uri.toURL(), language, javaLoader, uri.toString() + UUID.randomUUID(), cacheManager);

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
}

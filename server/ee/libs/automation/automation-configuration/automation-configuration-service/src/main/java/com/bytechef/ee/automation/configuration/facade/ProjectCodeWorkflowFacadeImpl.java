/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.project.ProjectHandler;
import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Workflow.CodeWorkflow;
import com.bytechef.ee.automation.configuration.exception.CodeWorkflowErrorType;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.codeworkflow.loader.automation.ProjectHandlerLoader;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
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

    private final CacheManager cacheManager;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final CodeWorkflowContainerFacade codeWorkflowContainerFacade;
    private final ProjectCodeWorkflowService projectCodeWorkflowService;
    private final boolean javaEnabled;
    private final ProjectHandlerLoader.JavaLoader javaLoader;

    @SuppressFBWarnings("EI")
    public ProjectCodeWorkflowFacadeImpl(
        ApplicationProperties applicationProperties, CacheManager cacheManager, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, CodeWorkflowContainerFacade codeWorkflowContainerFacade,
        ProjectCodeWorkflowService projectCodeWorkflowService) {

        this.cacheManager = cacheManager;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.codeWorkflowContainerFacade = codeWorkflowContainerFacade;
        this.projectCodeWorkflowService = projectCodeWorkflowService;
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
}

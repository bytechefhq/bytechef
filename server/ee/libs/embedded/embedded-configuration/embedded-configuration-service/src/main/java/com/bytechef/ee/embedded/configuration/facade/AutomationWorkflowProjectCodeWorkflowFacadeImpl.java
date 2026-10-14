/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.SystemProjects;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.project.ProjectHandler;
import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.configuration.domain.ProjectCodeWorkflow;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowDraftService;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectCodeWorkflowSourceDTO;
import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectCodeWorkflowStateDTO;
import com.bytechef.ee.embedded.configuration.exception.CodeWorkflowErrorType;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.codeworkflow.loader.automation.ProjectHandlerLoader;
import com.bytechef.platform.component.definition.AppEventComponentDefinition;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.workflow.definition.TriggerDefinition;
import com.bytechef.workflow.definition.WorkflowDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class AutomationWorkflowProjectCodeWorkflowFacadeImpl implements AutomationWorkflowProjectCodeWorkflowFacade {

    private static final Logger log = LoggerFactory.getLogger(AutomationWorkflowProjectCodeWorkflowFacadeImpl.class);

    private static final String MARKER = SystemProjects.EMBEDDED_AUTOMATION_NAME_PREFIX;

    private final AutomationWorkflowProjectFacade automationWorkflowProjectFacade;
    private final CacheManager cacheManager;
    private final CodeWorkflowContainerService codeWorkflowContainerService;
    private final CodeWorkflowFileStorage codeWorkflowFileStorage;
    private final boolean javaEnabled;
    private final ProjectCodeWorkflowDraftService projectCodeWorkflowDraftService;
    private final ProjectCodeWorkflowService projectCodeWorkflowService;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectCodeWorkflowFacadeImpl(
        ApplicationProperties applicationProperties, AutomationWorkflowProjectFacade automationWorkflowProjectFacade,
        CacheManager cacheManager, CodeWorkflowContainerService codeWorkflowContainerService,
        CodeWorkflowFileStorage codeWorkflowFileStorage,
        ProjectCodeWorkflowDraftService projectCodeWorkflowDraftService,
        ProjectCodeWorkflowService projectCodeWorkflowService, ProjectService projectService) {

        this.automationWorkflowProjectFacade = automationWorkflowProjectFacade;
        this.cacheManager = cacheManager;
        this.codeWorkflowContainerService = codeWorkflowContainerService;
        this.codeWorkflowFileStorage = codeWorkflowFileStorage;
        this.javaEnabled = applicationProperties.getWorkflow()
            .getCodeWorkflow()
            .isJavaEnabled();
        this.projectCodeWorkflowDraftService = projectCodeWorkflowDraftService;
        this.projectCodeWorkflowService = projectCodeWorkflowService;
        this.projectService = projectService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<String> save(byte[] bytes, Language language) {
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

        ProjectDefinition projectDefinition = loadProjectDefinition(language, bytes);

        long projectId = automationWorkflowProjectFacade.fetchProjectIdByName(projectDefinition.getName())
            .orElseGet(() -> automationWorkflowProjectFacade.createProject(
                projectDefinition.getName(), projectDefinition.getDescription()
                    .orElse(null),
                null, List.of(), null));

        Project project = projectService.getProject(projectId);

        projectCodeWorkflowDraftService.saveDraft(project, projectDefinition, bytes, language);

        automationWorkflowProjectFacade.publishProject(projectId);

        List<String> warnings = new ArrayList<>();

        for (WorkflowDefinition workflowDefinition : projectDefinition.getWorkflows()) {
            warnIfNotPubliclyInvocable(projectDefinition.getName(), workflowDefinition, warnings);
        }

        return warnings;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public long createEmptyCodeWorkflow(
        String name, Language language, @Nullable String description, @Nullable String category,
        @Nullable List<String> tags, @Nullable String permissionExpression) {

        validateName(name);
        validateEditableLanguage(language);

        if (automationWorkflowProjectFacade.fetchProjectIdByName(name)
            .isPresent()) {

            throw new ConfigurationException(
                "A catalog project named '" + name + "' already exists",
                CodeWorkflowErrorType.CODE_WORKFLOW_ALREADY_EXISTS);
        }

        byte[] bytes = readTemplate(language).replace("__NAME__", name)
            .getBytes(StandardCharsets.UTF_8);

        ProjectDefinition projectDefinition = loadProjectDefinition(language, bytes);

        long projectId = automationWorkflowProjectFacade.createProject(
            name,
            description == null || description.isBlank() ? projectDefinition.getDescription()
                .orElse(null) : description,
            category, tags == null ? List.of() : tags, permissionExpression);

        projectCodeWorkflowDraftService.saveDraft(
            projectService.getProject(projectId), projectDefinition, bytes, language);

        return projectId;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public AutomationWorkflowProjectCodeWorkflowSourceDTO getCodeWorkflowSource(long automationWorkflowProjectId) {
        CodeWorkflowContainer codeWorkflowContainer = getEditableCodeWorkflowContainer(automationWorkflowProjectId);

        return new AutomationWorkflowProjectCodeWorkflowSourceDTO(
            codeWorkflowFileStorage.readCodeWorkflowFileContent(codeWorkflowContainer.getWorkflows()),
            codeWorkflowContainer.getLanguage());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public Optional<AutomationWorkflowProjectCodeWorkflowStateDTO> getCodeWorkflowState(
        long automationWorkflowProjectId) {

        Project project = projectService.getProject(automationWorkflowProjectId);

        return projectCodeWorkflowService.fetchProjectCodeWorkflow(automationWorkflowProjectId)
            .map(projectCodeWorkflow -> {
                CodeWorkflowContainer codeWorkflowContainer = codeWorkflowContainerService.getCodeWorkflowContainer(
                    projectCodeWorkflow.getCodeWorkflowContainerId());

                return new AutomationWorkflowProjectCodeWorkflowStateDTO(
                    codeWorkflowContainer.getLanguage(),
                    projectCodeWorkflow.getProjectVersion() == project.getLastProjectVersion());
            });
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void updateCodeWorkflowSource(long automationWorkflowProjectId, String content) {
        CodeWorkflowContainer codeWorkflowContainer = getEditableCodeWorkflowContainer(automationWorkflowProjectId);

        Language language = codeWorkflowContainer.getLanguage();
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

        ProjectDefinition projectDefinition;

        try {
            projectDefinition = loadProjectDefinition(language, bytes);
        } catch (RuntimeException exception) {
            throw new ConfigurationException(
                "Failed to load code workflow source: " + exception.getMessage(),
                CodeWorkflowErrorType.SOURCE_LOAD_FAILED);
        }

        Project project = projectService.getProject(automationWorkflowProjectId);

        String expectedName = project.getName()
            .substring(MARKER.length());

        if (!Objects.equals(projectDefinition.getName(), expectedName)) {
            throw new ConfigurationException(
                "Renaming a code project by editing its source is not supported (expected name '" + expectedName +
                    "')",
                CodeWorkflowErrorType.CODE_WORKFLOW_NAME_MISMATCH);
        }

        projectCodeWorkflowDraftService.saveDraft(project, projectDefinition, bytes, language);
    }

    private CodeWorkflowContainer getEditableCodeWorkflowContainer(long automationWorkflowProjectId) {
        automationWorkflowProjectFacade.getProject(automationWorkflowProjectId);

        ProjectCodeWorkflow projectCodeWorkflow = projectCodeWorkflowService
            .fetchProjectCodeWorkflow(automationWorkflowProjectId)
            .orElseThrow(() -> new ConfigurationException(
                "Catalog project " + automationWorkflowProjectId + " is not a code project",
                CodeWorkflowErrorType.LANGUAGE_NOT_SUPPORTED));

        CodeWorkflowContainer codeWorkflowContainer = codeWorkflowContainerService.getCodeWorkflowContainer(
            projectCodeWorkflow.getCodeWorkflowContainerId());

        validateEditableLanguage(codeWorkflowContainer.getLanguage());

        return codeWorkflowContainer;
    }

    private static void validateEditableLanguage(Language language) {
        if (language != Language.JAVASCRIPT && language != Language.PYTHON) {
            throw new ConfigurationException(
                "Only JavaScript and Python code projects can be created and edited in the app",
                CodeWorkflowErrorType.LANGUAGE_NOT_SUPPORTED);
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.startsWith("__EMBEDDED") || name.indexOf('"') >= 0 ||
            name.indexOf('\\') >= 0 || name.indexOf('\n') >= 0 || name.indexOf('\r') >= 0) {

            throw new ConfigurationException(
                "Invalid code project name: must not be blank, start with '__EMBEDDED', or contain quotes, " +
                    "backslashes, or newlines",
                CodeWorkflowErrorType.INVALID_CODE_WORKFLOW_NAME);
        }
    }

    private static String readTemplate(Language language) {
        String resource = "automation-workflow-project-code-workflow-templates/starter." + language.getExtension();

        try (InputStream inputStream = AutomationWorkflowProjectCodeWorkflowFacadeImpl.class.getClassLoader()
            .getResourceAsStream(resource)) {

            if (inputStream == null) {
                throw new IllegalStateException("Missing starter template: " + resource);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void warnIfNotPubliclyInvocable(
        String projectName, WorkflowDefinition workflowDefinition, List<String> warnings) {

        List<? extends TriggerDefinition> triggerDefinitions = workflowDefinition.getTriggers()
            .orElseGet(List::of);

        boolean publiclyInvocable = triggerDefinitions.stream()
            .anyMatch(AutomationWorkflowProjectCodeWorkflowFacadeImpl::isPubliclyInvocableTrigger);

        if (!publiclyInvocable) {
            String warning = ("Workflow '%s' in deployed automation code workflow project '%s' declares neither a "
                + "request trigger nor an app-event trigger; it will not be invocable through the embedded public "
                + "endpoints").formatted(workflowDefinition.getName(), projectName);

            log.warn(warning);

            warnings.add(warning);
        }
    }

    private static boolean isPubliclyInvocableTrigger(TriggerDefinition triggerDefinition) {
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(triggerDefinition.getType());

        if (Objects.equals(workflowNodeType.name(), "request")) {
            return true;
        }

        return Objects.equals(workflowNodeType.name(), AppEventComponentDefinition.APP_EVENT) &&
            Objects.equals(workflowNodeType.operation(), AppEventComponentDefinition.NEW_EVENT);
    }

    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    private ProjectDefinition loadProjectDefinition(Language language, byte[] bytes) {
        try {
            Path path = Files.createTempFile("embedded_automation_code_workflow", language.getExtension());

            Files.write(path, bytes);

            URI uri = path.toUri();

            try {
                ProjectHandler projectHandler = ProjectHandlerLoader.loadProjectHandler(
                    uri.toURL(), language, uri + UUID.randomUUID()
                        .toString(),
                    cacheManager);

                return projectHandler.getDefinition();
            } finally {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

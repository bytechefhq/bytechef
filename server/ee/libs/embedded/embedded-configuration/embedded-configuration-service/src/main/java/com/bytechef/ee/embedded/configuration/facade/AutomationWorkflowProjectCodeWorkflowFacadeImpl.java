/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.project.ProjectHandler;
import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Workflow.CodeWorkflow;
import com.bytechef.ee.automation.configuration.domain.ProjectCodeWorkflow;
import com.bytechef.ee.automation.configuration.service.ProjectCodeWorkflowService;
import com.bytechef.ee.embedded.configuration.exception.CodeWorkflowErrorType;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.codeworkflow.loader.automation.ProjectHandlerLoader;
import com.bytechef.platform.component.definition.AppEventComponentDefinition;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.workflow.definition.TriggerDefinition;
import com.bytechef.workflow.definition.WorkflowDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deploys a plain automation code-workflow artifact into the embedded catalog. Structured identically to
 * {@code ProjectCodeWorkflowFacadeImpl} (same java-enabled/loader gate, same {@link ProjectHandlerLoader} call), with
 * one structural difference: the target catalog project is resolved/created through
 * {@link AutomationWorkflowProjectFacade}'s marker convention instead of a bare {@code ProjectService#fetchProject}, so
 * it stays hidden behind the embedded automation-workflow-project entity. Publish goes straight through
 * {@link ProjectService#publishProject}, mirroring how {@code ProjectCodeWorkflowFacadeImpl} itself publishes -- NOT
 * through {@link AutomationWorkflowProjectFacade#publishProject}, which additionally duplicates workflow rows for the
 * visual-editor versioning story that code workflows don't need.
 *
 * <p>
 * Redeploying the same artifact must keep each {@link ProjectWorkflow#getUuid()} stable across versions, since per-user
 * references pin on {@code catalog_workflow_uuid}. {@link ProjectWorkflowService#addWorkflow} always mints a fresh uuid
 * for a new row, so before the new version's workflows are added, {@link #fetchPreviousWorkflowUuidsByName} reads the
 * previous deploy's {@link CodeWorkflowContainer} (via its {@link ProjectCodeWorkflow} row) to map each still-present
 * workflow name to its previously-assigned uuid, mirroring the uuid carry-forward principle in
 * {@code ProjectFacadeImpl#publishProject} -- a same-named workflow keeps its uuid across redeploys, and only a
 * genuinely new name gets a fresh one.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class AutomationWorkflowProjectCodeWorkflowFacadeImpl implements AutomationWorkflowProjectCodeWorkflowFacade {

    private static final Logger log = LoggerFactory.getLogger(AutomationWorkflowProjectCodeWorkflowFacadeImpl.class);

    private final CacheManager cacheManager;
    private final AutomationWorkflowProjectFacade automationWorkflowProjectFacade;
    private final CodeWorkflowContainerFacade codeWorkflowContainerFacade;
    private final CodeWorkflowContainerService codeWorkflowContainerService;
    private final ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade;
    private final ProjectCodeWorkflowService projectCodeWorkflowService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final boolean javaEnabled;
    private final ProjectHandlerLoader.JavaLoader javaLoader;

    @SuppressFBWarnings("EI")
    public AutomationWorkflowProjectCodeWorkflowFacadeImpl(
        ApplicationProperties applicationProperties, CacheManager cacheManager,
        AutomationWorkflowProjectFacade automationWorkflowProjectFacade,
        CodeWorkflowContainerFacade codeWorkflowContainerFacade,
        CodeWorkflowContainerService codeWorkflowContainerService,
        ConnectedUserCodeWorkflowReferenceFacade connectedUserCodeWorkflowReferenceFacade,
        ProjectCodeWorkflowService projectCodeWorkflowService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService) {

        this.cacheManager = cacheManager;
        this.automationWorkflowProjectFacade = automationWorkflowProjectFacade;
        this.codeWorkflowContainerFacade = codeWorkflowContainerFacade;
        this.codeWorkflowContainerService = codeWorkflowContainerService;
        this.connectedUserCodeWorkflowReferenceFacade = connectedUserCodeWorkflowReferenceFacade;
        this.projectCodeWorkflowService = projectCodeWorkflowService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.javaEnabled = applicationProperties.getWorkflow()
            .getCodeWorkflow()
            .isJavaEnabled();
        this.javaLoader = applicationProperties.getWorkflow()
            .getCodeWorkflow()
            .getJavaLoader() == CodeWorkflow.JavaLoader.ESPRESSO
                ? ProjectHandlerLoader.JavaLoader.ESPRESSO
                : ProjectHandlerLoader.JavaLoader.CLASS_LOADER;
    }

    /**
     * Deploying a code workflow loads and executes the uploaded artifact on the server, so it is restricted to
     * administrators. The guard lives here, mirroring {@code IntegrationCodeWorkflowFacadeImpl#save} and
     * {@code ProjectCodeWorkflowFacadeImpl#save}, so it protects every caller, not only the REST entry point.
     */
    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<String> save(byte[] bytes, Language language) {
        if (!javaEnabled && language == Language.JAVA) {
            throw new ConfigurationException(
                "Uploading of Java code workflows is disabled",
                CodeWorkflowErrorType.JAVA_CODE_WORKFLOW_UPLOAD_DISABLED);
        }

        ProjectDefinition projectDefinition = loadProjectDefinition(language, bytes);

        long projectId = automationWorkflowProjectFacade.fetchProjectIdByName(projectDefinition.getName())
            .orElseGet(() -> automationWorkflowProjectFacade.createProject(
                projectDefinition.getName(),
                projectDefinition.getDescription()
                    .orElse(null),
                null, List.of(), null));

        Project project = projectService.getProject(projectId);

        // Must be captured before projectCodeWorkflowService.create(...) below, since
        // ProjectCodeWorkflowService#getProjectCodeWorkflow always resolves the most recently created row for the
        // project -- fetching after would return this deploy's own row instead of the previous one.
        Map<String, UUID> previousWorkflowUuidsByName = fetchPreviousWorkflowUuidsByName(project.getId());

        CodeWorkflowContainer codeWorkflowContainer = codeWorkflowContainerFacade.create(
            projectDefinition.getName(), projectDefinition.getVersion(), projectDefinition.getWorkflows(), language,
            bytes, PlatformType.AUTOMATION);

        projectCodeWorkflowService.create(codeWorkflowContainer, project);

        for (Map.Entry<String, String> entry : codeWorkflowContainer.getWorkflowNameIds()
            .entrySet()) {

            ProjectWorkflow projectWorkflow = projectWorkflowService.addWorkflow(
                project.getId(), project.getLastProjectVersion(), entry.getValue());

            UUID previousUuid = previousWorkflowUuidsByName.get(entry.getKey());

            if (previousUuid != null) {
                projectWorkflow.setUuid(previousUuid);

                projectWorkflowService.update(projectWorkflow);
            }
        }

        projectService.publishProject(project.getId(), null, false);

        Set<String> currentUuids = projectWorkflowService
            .getProjectWorkflows(project.getId(), project.getLastProjectVersion())
            .stream()
            .map(ProjectWorkflow::getUuidAsString)
            .collect(Collectors.toSet());

        Set<String> previousUuids = previousWorkflowUuidsByName.values()
            .stream()
            .map(UUID::toString)
            .collect(Collectors.toSet());

        connectedUserCodeWorkflowReferenceFacade.markDanglingReferences(project.getId(), previousUuids, currentUuids);

        List<String> warnings = new ArrayList<>();

        for (WorkflowDefinition workflowDefinition : projectDefinition.getWorkflows()) {
            warnIfNotPubliclyInvocable(projectDefinition.getName(), workflowDefinition, warnings);
        }

        return warnings;
    }

    /**
     * Maps each workflow name still present in the previous deploy to its previously-assigned
     * {@link ProjectWorkflow#getUuid()}, so the new version's same-named rows can carry it forward instead of getting a
     * fresh one from {@link ProjectWorkflowService#addWorkflow}. Returns an empty map on the first deploy of a project
     * (no previous {@link ProjectCodeWorkflow} row) or when a previously-named workflow no longer has a matching
     * {@link ProjectWorkflow} row -- both cases simply leave the corresponding new row with a fresh uuid.
     */
    private Map<String, UUID> fetchPreviousWorkflowUuidsByName(long projectId) {
        ProjectCodeWorkflow previousProjectCodeWorkflow;

        try {
            previousProjectCodeWorkflow = projectCodeWorkflowService.getProjectCodeWorkflow(projectId);
        } catch (IllegalArgumentException e) {
            return Map.of();
        }

        if (previousProjectCodeWorkflow == null) {
            return Map.of();
        }

        CodeWorkflowContainer previousCodeWorkflowContainer = codeWorkflowContainerService.getCodeWorkflowContainer(
            previousProjectCodeWorkflow.getCodeWorkflowContainerId());

        Map<String, UUID> previousWorkflowUuidsByName = new HashMap<>();

        for (Map.Entry<String, String> entry : previousCodeWorkflowContainer.getWorkflowNameIds()
            .entrySet()) {

            String workflowName = entry.getKey();
            String previousWorkflowId = entry.getValue();

            try {
                ProjectWorkflow previousProjectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(
                    previousWorkflowId);

                previousWorkflowUuidsByName.put(workflowName, previousProjectWorkflow.getUuid());
            } catch (IllegalArgumentException illegalArgumentException) {
                // No project-workflow row for this previously-deployed workflow id; the new row keeps its fresh uuid.
                log.trace(
                    "No previous project workflow for workflow id {}; keeping fresh uuid", previousWorkflowId,
                    illegalArgumentException);
            }
        }

        return previousWorkflowUuidsByName;
    }

    /**
     * Deploy-time trigger validation is advisory, not a rejection (consistent with {@code WorkflowValidator} being
     * advisory elsewhere): a deployed workflow with neither a {@code request} trigger nor an app-event trigger is still
     * deployed, but it will not be invocable through the embedded public endpoints, so a WARN is logged to surface the
     * gap to operators.
     */
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
                    uri.toURL(), language, javaLoader, uri + UUID.randomUUID()
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

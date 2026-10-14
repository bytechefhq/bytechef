/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.project.definition.ProjectDefinition;
import com.bytechef.ee.automation.configuration.domain.ProjectCodeWorkflow;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade;
import com.bytechef.ee.platform.codeworkflow.configuration.facade.CodeWorkflowContainerFacade.CodeWorkflowReconciliation;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.workflow.WorkflowPreDeleteListener;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
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
public class ProjectCodeWorkflowDraftServiceImpl implements ProjectCodeWorkflowDraftService {

    private final CodeWorkflowContainerFacade codeWorkflowContainerFacade;
    private final CodeWorkflowContainerService codeWorkflowContainerService;
    private final ProjectCodeWorkflowService projectCodeWorkflowService;
    private final ProjectWorkflowService projectWorkflowService;
    private final List<WorkflowPreDeleteListener> workflowPreDeleteListeners;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ProjectCodeWorkflowDraftServiceImpl(
        CodeWorkflowContainerFacade codeWorkflowContainerFacade,
        CodeWorkflowContainerService codeWorkflowContainerService,
        ProjectCodeWorkflowService projectCodeWorkflowService, ProjectWorkflowService projectWorkflowService,
        WorkflowService workflowService, List<WorkflowPreDeleteListener> workflowPreDeleteListeners) {

        this.codeWorkflowContainerFacade = codeWorkflowContainerFacade;
        this.codeWorkflowContainerService = codeWorkflowContainerService;
        this.projectCodeWorkflowService = projectCodeWorkflowService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
        this.workflowPreDeleteListeners = workflowPreDeleteListeners;
    }

    @Override
    public void saveDraft(Project project, ProjectDefinition projectDefinition, byte[] bytes, Language language) {
        long projectId = Objects.requireNonNull(project.getId());
        int draftProjectVersion = project.getLastProjectVersion();

        ProjectCodeWorkflow latestProjectCodeWorkflow = projectCodeWorkflowService.fetchProjectCodeWorkflow(projectId)
            .orElse(null);

        CodeWorkflowReconciliation reconciliation;

        if (latestProjectCodeWorkflow != null && latestProjectCodeWorkflow.getProjectVersion() == draftProjectVersion) {
            CodeWorkflowContainer draftCodeWorkflowContainer = codeWorkflowContainerService.getCodeWorkflowContainer(
                latestProjectCodeWorkflow.getCodeWorkflowContainerId());

            if (draftCodeWorkflowContainer.getLanguage() != language) {
                draftCodeWorkflowContainer.setLanguage(language);
            }

            reconciliation = codeWorkflowContainerFacade.update(
                draftCodeWorkflowContainer, projectDefinition.getVersion(), projectDefinition.getWorkflows(), bytes,
                PlatformType.AUTOMATION);
        } else {
            Map<String, String> reusableWorkflowNameIds = latestProjectCodeWorkflow == null
                ? Map.of()
                : resolveDraftWorkflowNameIds(latestProjectCodeWorkflow, projectId, draftProjectVersion);

            reconciliation = createDraftContainer(
                project, projectDefinition, bytes, language, reusableWorkflowNameIds);
        }

        Map<String, UUID> publishedWorkflowUuidsByName = getPublishedWorkflowUuidsByName(
            projectId, draftProjectVersion);

        for (Map.Entry<String, String> entry : reconciliation.addedWorkflowNameIds()
            .entrySet()) {

            ProjectWorkflow projectWorkflow = projectWorkflowService.addWorkflow(
                projectId, draftProjectVersion, entry.getValue());

            UUID publishedUuid = publishedWorkflowUuidsByName.get(entry.getKey());

            if (publishedUuid != null) {
                projectWorkflow.setUuid(publishedUuid);

                projectWorkflowService.update(projectWorkflow);
            }
        }

        for (String workflowId : reconciliation.removedWorkflowNameIds()
            .values()) {

            projectWorkflowService.delete(projectId, draftProjectVersion, workflowId);

            for (WorkflowPreDeleteListener workflowPreDeleteListener : workflowPreDeleteListeners) {
                workflowPreDeleteListener.onWorkflowPreDelete(workflowId);
            }

            workflowService.delete(workflowId);
        }
    }

    private CodeWorkflowReconciliation createDraftContainer(
        Project project, ProjectDefinition projectDefinition, byte[] bytes, Language language,
        Map<String, String> reusableWorkflowNameIds) {

        CodeWorkflowReconciliation reconciliation = codeWorkflowContainerFacade.create(
            projectDefinition.getName(), projectDefinition.getVersion(), projectDefinition.getWorkflows(), language,
            bytes, PlatformType.AUTOMATION, reusableWorkflowNameIds);

        projectCodeWorkflowService.create(reconciliation.codeWorkflowContainer(), project);

        return reconciliation;
    }

    private Map<String, UUID> getPublishedWorkflowUuidsByName(long projectId, int draftProjectVersion) {
        Optional<ProjectCodeWorkflow> publishedProjectCodeWorkflow =
            projectCodeWorkflowService.fetchPublishedProjectCodeWorkflow(projectId, draftProjectVersion);

        if (publishedProjectCodeWorkflow.isEmpty()) {
            return Map.of();
        }

        ProjectCodeWorkflow projectCodeWorkflow = publishedProjectCodeWorkflow.get();

        Map<String, String> publishedWorkflowNameIds = codeWorkflowContainerService
            .getCodeWorkflowContainer(projectCodeWorkflow.getCodeWorkflowContainerId())
            .getWorkflowNameIds();

        Map<String, UUID> workflowIdUuids = projectWorkflowService.getProjectWorkflows(
            projectId, projectCodeWorkflow.getProjectVersion())
            .stream()
            .collect(Collectors.toMap(ProjectWorkflow::getWorkflowId, ProjectWorkflow::getUuid));

        Map<String, UUID> publishedWorkflowUuidsByName = new HashMap<>();

        for (Map.Entry<String, String> entry : publishedWorkflowNameIds.entrySet()) {
            UUID uuid = workflowIdUuids.get(entry.getValue());

            if (uuid != null) {
                publishedWorkflowUuidsByName.put(entry.getKey(), uuid);
            }
        }

        return publishedWorkflowUuidsByName;
    }

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
}

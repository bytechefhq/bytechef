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

package com.bytechef.automation.knowledgebase.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.ownership.ProjectOwner;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.knowledgebase.dto.CreateKnowledgeBaseSourceInput;
import com.bytechef.automation.knowledgebase.dto.UpdateKnowledgeBaseSourceInput;
import com.bytechef.automation.knowledgebase.ownership.KnowledgeBaseOwnerContributorConfiguration;
import com.bytechef.automation.knowledgebase.service.WorkspaceKnowledgeBaseSourceService;
import com.bytechef.automation.knowledgebase.util.KnowledgeBaseSourceWorkflowGenerator;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.datastream.ItemReader;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.workflow.WorkflowPreDeleteListener;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSource;
import com.bytechef.platform.knowledgebase.domain.KnowledgeBaseSourceStatus;
import com.bytechef.platform.knowledgebase.domain.TombstoneStrategy;
import com.bytechef.platform.knowledgebase.service.KnowledgeBaseSourceService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
public class WorkspaceKnowledgeBaseSourceFacadeImpl implements WorkspaceKnowledgeBaseSourceFacade {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceKnowledgeBaseSourceFacadeImpl.class);

    static final String KNOWLEDGE_BASE_PROJECT_NAME_PREFIX =
        KnowledgeBaseOwnerContributorConfiguration.KNOWLEDGE_BASE_PROJECT_NAME_PREFIX;

    private final ComponentDefinitionService componentDefinitionService;
    private final KnowledgeBaseSourceService knowledgeBaseSourceService;
    private final PrincipalJobFacade principalJobFacade;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final TaskExecutor taskExecutor;
    private final List<WorkflowPreDeleteListener> workflowPreDeleteListeners;
    private final WorkflowService workflowService;
    private final WorkspaceKnowledgeBaseSourceService workspaceKnowledgeBaseSourceService;

    @SuppressFBWarnings("EI2")
    public WorkspaceKnowledgeBaseSourceFacadeImpl(
        ComponentDefinitionService componentDefinitionService, KnowledgeBaseSourceService knowledgeBaseSourceService,
        PrincipalJobFacade principalJobFacade, ProjectDeploymentFacade projectDeploymentFacade,
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService,
        TaskExecutor taskExecutor, WorkflowService workflowService,
        WorkspaceKnowledgeBaseSourceService workspaceKnowledgeBaseSourceService,
        List<WorkflowPreDeleteListener> workflowPreDeleteListeners) {

        this.componentDefinitionService = componentDefinitionService;
        this.knowledgeBaseSourceService = knowledgeBaseSourceService;
        this.principalJobFacade = principalJobFacade;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.taskExecutor = taskExecutor;
        this.workflowService = workflowService;
        this.workflowPreDeleteListeners = workflowPreDeleteListeners;
        this.workspaceKnowledgeBaseSourceService = workspaceKnowledgeBaseSourceService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public KnowledgeBaseSource create(Long workspaceId, CreateKnowledgeBaseSourceInput input) {
        Objects.requireNonNull(workspaceId, "workspaceId");
        Objects.requireNonNull(input, "input");

        String resolvedClusterElementName = resolveSourceClusterElementName(
            input.sourceComponentName(), input.sourceComponentVersion(), input.sourceClusterElementName());

        KnowledgeBaseSource sourceToInsert = new KnowledgeBaseSource();

        sourceToInsert.setName(input.name());
        sourceToInsert.setSourceComponentName(input.sourceComponentName());
        sourceToInsert.setSourceComponentVersion(input.sourceComponentVersion());
        sourceToInsert.setSourceClusterElementName(resolvedClusterElementName);
        sourceToInsert.setConnectionId(input.connectionId());
        sourceToInsert.setKnowledgeBaseId(input.knowledgeBaseId());
        sourceToInsert.setCadence(input.cadence());
        sourceToInsert.setStatus(KnowledgeBaseSourceStatus.BUILDING_PREVIEW);
        sourceToInsert.setEnabled(true);
        sourceToInsert.setParameters(input.parameters());
        sourceToInsert.setMetadataFields(input.metadataFields());
        sourceToInsert.setFullReplaceCadence(input.fullReplaceCadence());
        sourceToInsert.setTombstoneStrategy(
            input.tombstoneStrategy() == null ? TombstoneStrategy.PERIODIC_FULL_REPLACE : input.tombstoneStrategy());

        sourceToInsert.setWorkspaceId(workspaceId);

        KnowledgeBaseSource source = knowledgeBaseSourceService.create(sourceToInsert);

        String workflowDefinition = KnowledgeBaseSourceWorkflowGenerator.generate(source);

        Workflow workflow = workflowService.create(
            workflowDefinition, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        long projectId = findOrCreateKnowledgeBaseSourceProject(workspaceId);
        int projectVersion = projectService.getProject(projectId)
            .getLastProjectVersion();

        projectWorkflowService.addWorkflow(projectId, projectVersion, workflow.getId());

        long projectDeploymentId = findOrCreateProjectDeployment(projectId, projectVersion);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setProjectDeploymentId(projectDeploymentId);
        projectDeploymentWorkflow.setWorkflowId(workflow.getId());
        projectDeploymentWorkflow.setEnabled(true);
        projectDeploymentWorkflow.setInputs(Map.of());

        Long connectionId = input.connectionId();

        if (connectionId != null) {
            String sourceNodeName = input.sourceComponentName() + "_1";

            projectDeploymentWorkflow.setConnections(List.of(
                new ProjectDeploymentWorkflowConnection(connectionId, sourceNodeName, "sync")));
        }

        projectDeploymentWorkflowService.create(projectDeploymentWorkflow);

        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentId, workflow.getId(), true);

        source.setWorkflowId(workflow.getId());

        KnowledgeBaseSource updated = knowledgeBaseSourceService.update(source);

        registerAfterCommitSyncTrigger(updated, projectDeploymentId, projectVersion);

        return updated;
    }

    private void registerAfterCommitSyncTrigger(
        KnowledgeBaseSource source, long projectDeploymentId, int projectVersion) {

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            try {
                triggerSyncJob(source, projectDeploymentId, projectVersion);
            } catch (RuntimeException exception) {
                log.warn(
                    "Initial sync trigger failed for KnowledgeBaseSource {}: {}", source.getId(),
                    exception.getMessage(), exception);
            }

            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCommit() {
                taskExecutor.execute(() -> {
                    try {
                        triggerSyncJob(source, projectDeploymentId, projectVersion);
                    } catch (RuntimeException exception) {
                        log.warn(
                            "Initial sync trigger failed for KnowledgeBaseSource {} after commit: {}",
                            source.getId(), exception.getMessage(), exception);
                    }
                });
            }
        });
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public KnowledgeBaseSource update(Long workspaceId, Long sourceId, UpdateKnowledgeBaseSourceInput input) {
        Objects.requireNonNull(workspaceId, "workspaceId");
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(input, "input");

        verifyMembership(workspaceId, sourceId);

        KnowledgeBaseSource source = knowledgeBaseSourceService.get(sourceId);

        String name = input.name();

        if (name != null) {
            source.setName(name);
        }

        String cadence = input.cadence();

        if (cadence != null && !cadence.equals(source.getCadence())) {
            source.setCadence(cadence);

            String workflowId = source.getWorkflowId();

            if (workflowId != null) {
                Workflow workflow = workflowService.getWorkflow(workflowId);

                String mutatedDefinition = KnowledgeBaseSourceWorkflowGenerator.updateCadence(
                    workflow.getDefinition(), cadence);

                workflowService.update(workflowId, mutatedDefinition, workflow.getVersion());
            }
        }

        Boolean enabled = input.enabled();

        if (enabled != null && enabled != source.isEnabled()) {
            source.setEnabled(enabled);

            toggleProjectDeploymentWorkflow(workspaceId, source, enabled);
        }

        Map<String, ?> metadataFields = input.metadataFields();

        if (metadataFields != null) {
            source.setMetadataFields(metadataFields.isEmpty() ? null : metadataFields);
        }

        String fullReplaceCadence = input.fullReplaceCadence();

        if (fullReplaceCadence != null) {
            String resolvedFullReplaceCadence = fullReplaceCadence.isEmpty() ? null : fullReplaceCadence;

            source.setFullReplaceCadence(resolvedFullReplaceCadence);

            String workflowId = source.getWorkflowId();

            if (workflowId != null) {
                Workflow workflow = workflowService.getWorkflow(workflowId);

                String mutatedDefinition = KnowledgeBaseSourceWorkflowGenerator.updateFullReplaceCadence(
                    workflow.getDefinition(), source.getCadence(), resolvedFullReplaceCadence);

                workflowService.update(workflowId, mutatedDefinition, workflow.getVersion());
            }
        }

        TombstoneStrategy tombstoneStrategy = input.tombstoneStrategy();

        if (tombstoneStrategy != null) {
            source.setTombstoneStrategy(tombstoneStrategy);
        }

        return knowledgeBaseSourceService.update(source);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void delete(Long workspaceId, Long sourceId) {
        Objects.requireNonNull(workspaceId, "workspaceId");
        Objects.requireNonNull(sourceId, "sourceId");

        verifyMembership(workspaceId, sourceId);

        KnowledgeBaseSource source = knowledgeBaseSourceService.get(sourceId);

        String workflowId = source.getWorkflowId();

        if (workflowId != null) {
            long projectId = findOrCreateKnowledgeBaseSourceProject(workspaceId);
            int projectVersion = projectService.getProject(projectId)
                .getLastProjectVersion();
            long projectDeploymentId = findOrCreateProjectDeployment(projectId, projectVersion);

            Optional<ProjectDeploymentWorkflow> projectDeploymentWorkflowOptional = projectDeploymentWorkflowService
                .fetchProjectDeploymentWorkflow(projectDeploymentId, workflowId);

            projectDeploymentWorkflowOptional.ifPresent(
                projectDeploymentWorkflow -> projectDeploymentWorkflowService
                    .delete(projectDeploymentWorkflow.getId()));

            projectWorkflowService.delete(projectId, projectVersion, workflowId);

            for (WorkflowPreDeleteListener workflowPreDeleteListener : workflowPreDeleteListeners) {
                workflowPreDeleteListener.onWorkflowPreDelete(workflowId);
            }

            workflowService.delete(workflowId);
        }

        knowledgeBaseSourceService.delete(sourceId);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    @Transactional(propagation = Propagation.NEVER)
    public long refreshNow(Long workspaceId, Long sourceId) {
        Objects.requireNonNull(workspaceId, "workspaceId");
        Objects.requireNonNull(sourceId, "sourceId");

        verifyMembership(workspaceId, sourceId);

        KnowledgeBaseSource source = knowledgeBaseSourceService.get(sourceId);

        if (source.getStatus() == KnowledgeBaseSourceStatus.DISABLED) {
            throw new IllegalStateException(
                "KnowledgeBaseSource " + sourceId + " is DISABLED; cannot refresh");
        }

        if (source.getWorkflowId() == null) {
            throw new IllegalStateException(
                "KnowledgeBaseSource " + sourceId + " has no workflow; cannot refresh");
        }

        long projectId = findOrCreateKnowledgeBaseSourceProject(workspaceId);
        int projectVersion = projectService.getProject(projectId)
            .getLastProjectVersion();
        long projectDeploymentId = findOrCreateProjectDeployment(projectId, projectVersion);

        return triggerSyncJob(source, projectDeploymentId, projectVersion);
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public void setEnabled(Long workspaceId, Long sourceId, boolean enabled) {
        Objects.requireNonNull(workspaceId, "workspaceId");
        Objects.requireNonNull(sourceId, "sourceId");

        verifyMembership(workspaceId, sourceId);

        KnowledgeBaseSource source = knowledgeBaseSourceService.get(sourceId);

        if (source.isEnabled() == enabled) {
            return;
        }

        source.setEnabled(enabled);

        toggleProjectDeploymentWorkflow(workspaceId, source, enabled);

        knowledgeBaseSourceService.update(source);
    }

    private void verifyMembership(Long workspaceId, Long sourceId) {
        Long actualWorkspaceId = workspaceKnowledgeBaseSourceService
            .fetchWorkspaceIdByKnowledgeBaseSourceId(sourceId)
            .orElseThrow(() -> new IllegalArgumentException(
                "KnowledgeBaseSource " + sourceId + " is not registered to any workspace"));

        if (!Objects.equals(actualWorkspaceId, workspaceId)) {
            throw new IllegalArgumentException(
                "KnowledgeBaseSource " + sourceId + " does not belong to workspace " + workspaceId);
        }
    }

    private long findOrCreateKnowledgeBaseSourceProject(Long workspaceId) {
        String projectName = KNOWLEDGE_BASE_PROJECT_NAME_PREFIX + workspaceId;

        Set<ProjectOwner> excludedProjectOwners = EnumSet.complementOf(EnumSet.of(ProjectOwner.KNOWLEDGE_BASE));

        List<Long> projectIds = projectService.getWorkspaceProjectIds(workspaceId, excludedProjectOwners);

        List<Project> projects = projectService.getProjects(projectIds);

        return projects.stream()
            .filter(project -> projectName.equals(project.getName()))
            .findFirst()
            .map(Project::getId)
            .orElseGet(() -> {
                Project project = new Project();

                project.setName(projectName);
                project.setDescription(
                    "System project that owns auto-generated Knowledge Base sync workflows. Do not edit.");
                project.setWorkspaceId(workspaceId);

                Project saved = projectService.create(project);

                return saved.getId();
            });
    }

    private long findOrCreateProjectDeployment(long projectId, int projectVersion) {
        return projectDeploymentService.fetchProjectDeployment(projectId, Environment.DEVELOPMENT)
            .map(projectDeployment -> {
                if (!projectDeployment.isEnabled()) {
                    projectDeploymentService.updateEnabled(projectDeployment.getId(), true);
                }

                return projectDeployment.getId();
            })
            .orElseGet(() -> {
                ProjectDeployment projectDeployment = new ProjectDeployment();

                Project project = projectService.getProject(projectId);

                projectDeployment.setName(project.getName());
                projectDeployment.setProjectId(projectId);
                projectDeployment.setProjectVersion(projectVersion);
                projectDeployment.setEnvironment(Environment.DEVELOPMENT);
                projectDeployment.setEnabled(true);

                ProjectDeployment saved = projectDeploymentService.create(projectDeployment);

                return saved.getId();
            });
    }

    private void toggleProjectDeploymentWorkflow(Long workspaceId, KnowledgeBaseSource source, boolean enabled) {
        String workflowId = source.getWorkflowId();

        if (workflowId == null) {
            return;
        }

        long projectId = findOrCreateKnowledgeBaseSourceProject(workspaceId);
        int projectVersion = projectService.getProject(projectId)
            .getLastProjectVersion();
        long projectDeploymentId = findOrCreateProjectDeployment(projectId, projectVersion);

        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentId, workflowId, enabled);
    }

    private String resolveSourceClusterElementName(
        String sourceComponentName, int sourceComponentVersion, String explicitName) {

        if (explicitName != null) {
            return explicitName;
        }

        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
            sourceComponentName, sourceComponentVersion);

        for (ClusterElementDefinition clusterElement : componentDefinition.getClusterElements()) {
            ClusterElementType type = clusterElement.getType();

            if (ItemReader.SOURCE.equals(type)) {
                return clusterElement.getName();
            }
        }

        throw new IllegalArgumentException(
            "Component " + sourceComponentName + " v" + sourceComponentVersion
                + " has no ItemReader cluster element");
    }

    private long triggerSyncJob(KnowledgeBaseSource source, long projectDeploymentId, int projectVersion) {
        String workflowId = Objects.requireNonNull(source.getWorkflowId(), "workflowId");

        return principalJobFacade.createJob(
            new JobParametersDTO(
                workflowId, Map.of(),
                Map.of("projectVersion", projectVersion)),
            projectDeploymentId, PlatformType.AUTOMATION);
    }
}

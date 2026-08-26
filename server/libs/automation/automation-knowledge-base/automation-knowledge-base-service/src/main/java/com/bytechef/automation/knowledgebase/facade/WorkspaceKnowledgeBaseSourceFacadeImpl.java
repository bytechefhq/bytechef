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
import com.bytechef.automation.configuration.domain.SystemProjects;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.knowledgebase.dto.CreateKnowledgeBaseSourceInput;
import com.bytechef.automation.knowledgebase.dto.UpdateKnowledgeBaseSourceInput;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
 * Implementation of {@link WorkspaceKnowledgeBaseSourceFacade}. Coordinates the Knowledge Base control plane (sources)
 * with the Atlas configuration plane (workflows, project-deployment-workflows) and the workspace ↔ source relation
 * table so that every source is backed by an auto-generated
 * {@code [schedule.cron] -> [data-stream.stream(SOURCE=..., DESTINATION=knowledgeBase.writeAsDocument)]} workflow that
 * the existing scheduler + worker dispatch infrastructure runs without further glue.
 *
 * <p>
 * Per-workspace projects are auto-provisioned on first use using a fixed name
 * ({@value #KNOWLEDGE_BASE_PROJECT_NAME_PREFIX}{@code <workspaceId>}) and reused by subsequent sources in the same
 * workspace. The same project owns one {@link ProjectDeployment} per environment (DEVELOPMENT for now); each Knowledge
 * Base Source contributes one {@link ProjectDeploymentWorkflow} so its trigger is independently enable-toggleable.
 *
 * <p>
 * Mutating methods verify membership through
 * {@link WorkspaceKnowledgeBaseSourceService#fetchWorkspaceIdByKnowledgeBaseSourceId} and throw
 * {@link IllegalArgumentException} when the source does not belong to the supplied workspace — this prevents
 * cross-workspace mutation through forged {@code workspaceId} arguments.
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnProperty(prefix = "bytechef.ai.knowledge-base", name = "enabled", havingValue = "true")
public class WorkspaceKnowledgeBaseSourceFacadeImpl implements WorkspaceKnowledgeBaseSourceFacade {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceKnowledgeBaseSourceFacadeImpl.class);

    static final String KNOWLEDGE_BASE_PROJECT_NAME_PREFIX = SystemProjects.KNOWLEDGE_BASE_NAME_PREFIX;

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
        // Default at the boundary: null on the create input means "use default" rather than "leave unset". DB
        // default is 0/PERIODIC_FULL_REPLACE; setting explicitly so the in-memory entity matches the persisted row.
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

        // Wire the user-supplied source connection into the deployment workflow so the runtime task dispatcher can
        // bind it to the cluster element at perform time. workflowConnectionKey must equal the source cluster
        // element's name (e.g. "hubspot_1"); the surrounding task is named "sync" — see
        // KnowledgeBaseSourceWorkflowGenerator. Without this row the source reader sees `componentConnection = null`.
        Long connectionId = input.connectionId();

        if (connectionId != null) {
            String sourceNodeName = input.sourceComponentName() + "_1";

            projectDeploymentWorkflow.setConnections(List.of(
                new ProjectDeploymentWorkflowConnection(connectionId, sourceNodeName, "sync")));
        }

        projectDeploymentWorkflowService.create(projectDeploymentWorkflow);

        // Creating the row enabled is not enough: only ProjectDeploymentFacade registers the cron trigger with the
        // scheduler (ProjectDeploymentWorkflowService.create is a plain repository save). Without this the source's
        // initial sync — fired directly below — would be the only run it ever had.
        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentId, workflow.getId(), true);

        source.setWorkflowId(workflow.getId());

        KnowledgeBaseSource updated = knowledgeBaseSourceService.update(source);

        // Trigger the initial sync AFTER the surrounding @Transactional commits. principalJobFacade.createJob is
        // @Transactional(propagation = NEVER) for monolith-mode queue safety (see JobFacadeImpl), so calling it
        // inline here would throw IllegalTransactionStateException. The previous code wrapped the call in a silent
        // try/catch that swallowed exactly that exception — which meant the initial sync never actually fired and
        // users waited until the cron tick. Use TransactionSynchronization.afterCommit to fire the job outside the
        // tx; failures get logged loudly instead of being silently dropped, and the source row is already durable
        // by then.
        registerAfterCommitSyncTrigger(updated, projectDeploymentId, projectVersion);

        return updated;
    }

    private void registerAfterCommitSyncTrigger(
        KnowledgeBaseSource source, long projectDeploymentId, int projectVersion) {

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // Caller already runs outside a transaction — just fire directly. Defensive; class-level @Transactional
            // means this branch shouldn't normally hit in production but keeps the helper safe to reuse.
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
                // Hand off to applicationTaskExecutor so the job creation runs on a thread with no
                // TransactionSynchronizationManager binding. afterCommit fires before cleanupAfterCompletion clears
                // the bound JDBC connection on this thread, so principalJobFacade.createJob's
                // @Transactional(propagation = NEVER) would otherwise see a still-active synchronization and throw
                // IllegalTransactionStateException.
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
            // Empty map => "clear the whitelist (revert to full-flatten)"; non-empty => replace.
            // Distinguished from "leave unchanged" by null at the DTO level.
            source.setMetadataFields(metadataFields.isEmpty() ? null : metadataFields);
        }

        String fullReplaceCadence = input.fullReplaceCadence();

        if (fullReplaceCadence != null) {
            // Empty string sentinel means "clear" (drop back to single-trigger); any non-empty value replaces.
            // Null at the DTO level stays "leave unchanged" — already filtered out by the outer condition.
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
    public void delete(Long workspaceId, Long sourceId) {
        Objects.requireNonNull(workspaceId, "workspaceId");
        Objects.requireNonNull(sourceId, "sourceId");

        verifyMembership(workspaceId, sourceId);

        KnowledgeBaseSource source = knowledgeBaseSourceService.get(sourceId);

        String workflowId = source.getWorkflowId();

        if (workflowId != null) {
            // Drop ProjectDeploymentWorkflow rows pointing at this workflow. Iterate per-deployment because the
            // (deployment, workflow) pair is unique but workflows are looked up by id only.
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
        // Propagation.NEVER required: principalJobFacade.createJob is @Transactional(propagation = NEVER) for
        // monolith-mode queue safety (see JobFacadeImpl), and the class-level @Transactional on this facade would
        // otherwise force this method into a transaction that conflicts. Each underlying service call manages its
        // own transaction independently — the reads are safe without an outer wrapper.
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

        return projectService.fetchProject(projectName)
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
                // The Knowledge Base deployment is a system-managed singleton that must stay enabled; individual
                // sources toggle at the ProjectDeploymentWorkflow level. Reconcile a pre-existing deployment that
                // ended up disabled so the per-workflow trigger-registration guard in ProjectDeploymentFacadeImpl
                // ("if (projectDeployment.isEnabled())") does not silently swallow the cron trigger.
                if (!projectDeployment.isEnabled()) {
                    projectDeploymentService.updateEnabled(projectDeployment.getId(), true);
                }

                return projectDeployment.getId();
            })
            .orElseGet(() -> {
                ProjectDeployment projectDeployment = new ProjectDeployment();

                // Named after the system project it deploys, so both rows carry the same marker and a reader of
                // either table can tell at a glance that neither is user-created. See SystemProjects.
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

        // Delegate to ProjectDeploymentFacade (not the raw ProjectDeploymentWorkflowService) so the scheduler cron
        // trigger is actually registered/unregistered via enableWorkflowTriggers/disableWorkflowTriggers. Flipping
        // the enabled column alone never arms the schedule, which is why scheduled syncs previously never fired.
        // findOrCreateProjectDeployment above guarantees the parent deployment is enabled so the facade's
        // "if (projectDeployment.isEnabled())" trigger-registration guard passes.
        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentId, workflowId, enabled);
    }

    /**
     * Resolve the source-side {@code ItemReader} cluster element name. When the caller supplied an explicit name, trust
     * it as-is. Otherwise auto-pick the first {@link ItemReader#SOURCE} cluster element on the source component — saves
     * the wizard from forcing a pick when there's only one ItemReader (the common case). Throws when the component has
     * no {@code ItemReader} cluster element at all so the failure is loud at create-time rather than at the first sync
     * run.
     */
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

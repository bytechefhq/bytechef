/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.contextstore.facade;

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
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.component.definition.datastream.ItemReader;
import com.bytechef.ee.automation.contextstore.audit.ContextStoreSourceAuditEvent;
import com.bytechef.ee.automation.contextstore.audit.ContextStoreSourceAuditPublisher;
import com.bytechef.ee.automation.contextstore.dto.CreateContextStoreSourceInput;
import com.bytechef.ee.automation.contextstore.dto.UpdateContextStoreSourceInput;
import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreService;
import com.bytechef.ee.automation.contextstore.util.ContextStoreWorkflowGenerator;
import com.bytechef.ee.platform.contextstore.clickhouse.ClickHouseTableProvisioner;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSourceStatus;
import com.bytechef.ee.platform.contextstore.domain.TombstoneStrategy;
import com.bytechef.ee.platform.contextstore.exception.ContextStoreSourceErrorType;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.workflow.WorkflowPreDeleteListener;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Implementation of {@link WorkspaceContextStoreSourceFacade}. Coordinates the Context Store control plane (sources,
 * entities) with the Atlas configuration plane (workflows, project-deployment-workflows) and the workspace ↔ source
 * relation table so that every source is backed by an auto-generated
 * {@code [schedule.cron] -> [data-stream.stream(SOURCE=..., DESTINATION=contextStore.writeToReplica)]} workflow that
 * the existing scheduler + worker dispatch infrastructure runs without further glue.
 *
 * <p>
 * Per-workspace projects are auto-provisioned on first use using a fixed name
 * ({@value #CONTEXT_STORE_PROJECT_NAME_PREFIX}{@code <workspaceId>}) and reused by subsequent sources in the same
 * workspace. The same project owns one {@link ProjectDeployment} per environment (DEVELOPMENT for now); each Context
 * Source contributes one {@link ProjectDeploymentWorkflow} so its trigger is independently enable-toggleable.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
public class WorkspaceContextStoreSourceFacadeImpl implements WorkspaceContextStoreSourceFacade {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceContextStoreSourceFacadeImpl.class);

    static final String CONTEXT_STORE_PROJECT_NAME_PREFIX = SystemProjects.CONTEXT_STORE_NAME_PREFIX;

    private final ObjectProvider<ClickHouseTableProvisioner> clickHouseTableProvisionerProvider;
    private final ComponentDefinitionService componentDefinitionService;
    private final ContextStoreSourceAuditPublisher contextStoreSourceAuditPublisher;
    private final ContextStoreSourceService contextStoreSourceService;
    private final PrincipalJobFacade principalJobFacade;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final TaskExecutor taskExecutor;
    private final List<WorkflowPreDeleteListener> workflowPreDeleteListeners;
    private final WorkflowService workflowService;
    private final WorkspaceContextStoreService workspaceContextStoreService;

    @SuppressFBWarnings("EI2")
    public WorkspaceContextStoreSourceFacadeImpl(
        ObjectProvider<ClickHouseTableProvisioner> clickHouseTableProvisionerProvider,
        ComponentDefinitionService componentDefinitionService,
        ContextStoreSourceAuditPublisher contextStoreSourceAuditPublisher,
        ContextStoreSourceService contextStoreSourceService, PrincipalJobFacade principalJobFacade,
        ProjectDeploymentFacade projectDeploymentFacade, ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService,
        TaskExecutor taskExecutor, WorkflowService workflowService,
        WorkspaceContextStoreService workspaceContextStoreService,
        List<WorkflowPreDeleteListener> workflowPreDeleteListeners) {

        this.clickHouseTableProvisionerProvider = clickHouseTableProvisionerProvider;
        this.componentDefinitionService = componentDefinitionService;
        this.contextStoreSourceAuditPublisher = contextStoreSourceAuditPublisher;
        this.contextStoreSourceService = contextStoreSourceService;
        this.principalJobFacade = principalJobFacade;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.taskExecutor = taskExecutor;
        this.workflowService = workflowService;
        this.workflowPreDeleteListeners = workflowPreDeleteListeners;
        this.workspaceContextStoreService = workspaceContextStoreService;
    }

    @Override
    public ContextStoreSource create(Long workspaceId, CreateContextStoreSourceInput input) {
        Objects.requireNonNull(workspaceId, "workspaceId");
        Objects.requireNonNull(input, "input");

        // Defense in depth: the parent ContextStore must belong to this workspace. WorkspaceContextStoreService
        // owns the workspace scoping rules over context_store.workspace_id; this is the same ownership check pattern
        // used by the KB clone flow.
        if (!workspaceContextStoreService.isStoreInWorkspace(workspaceId, input.contextStoreId())) {
            throw new IllegalArgumentException(
                "Context Store " + input.contextStoreId() + " is not in workspace " + workspaceId);
        }

        ClusterElementDefinition sourceClusterElement = resolveSourceClusterElement(
            input.sourceComponentName(), input.sourceComponentVersion(), input.sourceClusterElementName());

        String resolvedClusterElementName = sourceClusterElement.getName();

        // Refuse to create a source missing required source-cluster-element parameters. The sync workflow would
        // otherwise blow up on first run (or on Refresh Now) with an opaque "missing input X" message — catching it
        // here gives the user a precise field-list instead of a runtime surprise.
        validateRequiredParameters(sourceClusterElement, input);

        ContextStoreSource sourceToInsert = new ContextStoreSource();

        sourceToInsert.setContextStoreId(input.contextStoreId());
        sourceToInsert.setName(input.name());
        sourceToInsert.setEntityName(input.entityName());
        sourceToInsert.setDescription(input.description());
        sourceToInsert.setIdField(input.idField());
        sourceToInsert.setStoredFields(input.storedFields());
        sourceToInsert.setIndexedFields(input.indexedFields());
        sourceToInsert.setSemanticIndexFields(input.semanticIndexFields());
        sourceToInsert.setParameters(input.parameters());
        sourceToInsert.setSourceComponentName(input.sourceComponentName());
        sourceToInsert.setSourceComponentVersion(input.sourceComponentVersion());
        sourceToInsert.setSourceClusterElementName(resolvedClusterElementName);
        sourceToInsert.setConnectionId(input.connectionId());
        sourceToInsert.setCadence(input.cadence());
        sourceToInsert.setStatus(ContextStoreSourceStatus.BUILDING_PREVIEW);
        sourceToInsert.setFullReplaceCadence(input.fullReplaceCadence());
        // Default at the boundary: a null strategy on the create input means "use the default" rather than "leave
        // unset". The DB default is 0/PERIODIC_FULL_REPLACE, but we set it explicitly here so the in-memory record
        // matches the row Spring Data JDBC will persist.
        sourceToInsert.setTombstoneStrategy(
            input.tombstoneStrategy() == null ? TombstoneStrategy.PERIODIC_FULL_REPLACE : input.tombstoneStrategy());

        // ClickHouse is a deployment-wide opt-in (set bytechef.context-store.clickhouse.url). When opted in, every
        // source gets a typed-projection table provisioned alongside the Postgres rows; when not, the provisioner
        // bean is absent and records live in Postgres only.
        ClickHouseTableProvisioner provisioner = clickHouseTableProvisionerProvider.getIfAvailable();

        sourceToInsert.setWorkspaceId(workspaceId);

        ContextStoreSource source = contextStoreSourceService.create(sourceToInsert);

        if (provisioner != null) {
            // Provision the ClickHouse table BEFORE the workflow setup so a DDL failure rolls back the source insert
            // via the surrounding @Transactional. Worst-case orphan is the reverse — source row persists but DDL
            // fails on retry — and is mopped up by the IF NOT EXISTS retry semantics.
            String tableName = provisioner.provisionTable(workspaceId, source.getContextStoreId(), source);

            source.setClickhouseTableName(tableName);
            source = contextStoreSourceService.update(source);
        }

        String workflowDefinition = ContextStoreWorkflowGenerator.generate(source, input);

        Workflow workflow = workflowService.create(
            workflowDefinition, Workflow.Format.JSON, Workflow.SourceType.JDBC);

        long projectId = findOrCreateContextStoreProject(workspaceId);
        int projectVersion = projectService.getProject(projectId)
            .getLastProjectVersion();

        projectWorkflowService.addWorkflow(projectId, projectVersion, workflow.getId());

        long projectDeploymentId = findOrCreateProjectDeployment(projectId, projectVersion);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setProjectDeploymentId(projectDeploymentId);
        projectDeploymentWorkflow.setWorkflowId(workflow.getId());
        projectDeploymentWorkflow.setEnabled(true);
        projectDeploymentWorkflow.setInputs(Map.of());

        // workflowConnectionKey must equal the source cluster element's name (that's what
        // DataStreamStreamActionDefinition.getValue resolves via clusterElement.getWorkflowNodeName); workflowNodeName
        // is the surrounding task name ("sync") emitted by ContextStoreWorkflowGenerator. Without this row the
        // dispatcher's CONNECTION_IDS metadata is empty and the reader hits "URI with undefined scheme".
        Long connectionId = input.connectionId();

        if (connectionId != null) {
            String sourceNodeName = input.sourceComponentName() + "_1";

            projectDeploymentWorkflow.setConnections(List.of(
                new ProjectDeploymentWorkflowConnection(connectionId, sourceNodeName, "sync")));
        }

        projectDeploymentWorkflowService.create(projectDeploymentWorkflow);

        // Creating the row enabled is not enough: only ProjectDeploymentFacade registers the cron trigger with the
        // scheduler (ProjectDeploymentWorkflowService.create is a plain repository save). Without this the source's
        // initial sync — fired directly below — would be the only run it ever had. Same reasoning as the toggle path
        // in toggleProjectDeploymentWorkflow.
        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentId, workflow.getId(), true);

        source.setWorkflowId(workflow.getId());

        ContextStoreSource updated = contextStoreSourceService.update(source);

        Map<String, Object> createdData = new HashMap<>();

        createdData.put("workspaceId", String.valueOf(workspaceId));

        if (updated.getName() != null) {
            createdData.put("name", updated.getName());
        }

        contextStoreSourceAuditPublisher.publish(
            ContextStoreSourceAuditEvent.CONTEXT_STORE_SOURCE_CREATED, updated.getId(), createdData);

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
        ContextStoreSource source, long projectDeploymentId, int projectVersion) {

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // Caller already runs outside a transaction — just fire directly. Defensive; class-level @Transactional
            // means this branch shouldn't normally hit in production but keeps the helper safe to reuse.
            try {
                triggerSyncJob(source, projectDeploymentId, projectVersion);
            } catch (RuntimeException exception) {
                log.warn(
                    "Initial sync trigger failed for ContextStoreSource {}: {}", source.getId(),
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
                            "Initial sync trigger failed for ContextStoreSource {} after commit: {}",
                            source.getId(), exception.getMessage(), exception);
                    }
                });
            }
        });
    }

    @Override
    public ContextStoreSource update(Long workspaceId, Long sourceId, UpdateContextStoreSourceInput input) {
        Objects.requireNonNull(workspaceId, "workspaceId");
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(input, "input");

        ContextStoreSource source = contextStoreSourceService.get(sourceId);

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

                String mutatedDefinition = ContextStoreWorkflowGenerator.updateCadence(
                    workflow.getDefinition(), cadence);

                workflowService.update(workflowId, mutatedDefinition, workflow.getVersion());
            }
        }

        Boolean enabled = input.enabled();

        boolean enabledChanged = enabled != null && enabled != source.isEnabled();

        if (enabledChanged) {
            source.setEnabled(enabled);

            toggleProjectDeploymentWorkflow(workspaceId, source, enabled);
        }

        String fullReplaceCadence = input.fullReplaceCadence();

        if (fullReplaceCadence != null) {
            // Empty string sentinel means "clear" (drop back to single-trigger); any non-empty value replaces.
            // Null at the DTO level stays "leave unchanged" — already filtered out by the outer condition.
            source.setFullReplaceCadence(fullReplaceCadence.isEmpty() ? null : fullReplaceCadence);
        }

        TombstoneStrategy tombstoneStrategy = input.tombstoneStrategy();

        if (tombstoneStrategy != null) {
            source.setTombstoneStrategy(tombstoneStrategy);
        }

        ContextStoreSource updated = contextStoreSourceService.update(source);

        if (enabledChanged) {
            ContextStoreSourceAuditEvent eventType = enabled
                ? ContextStoreSourceAuditEvent.CONTEXT_STORE_SOURCE_ENABLED
                : ContextStoreSourceAuditEvent.CONTEXT_STORE_SOURCE_DISABLED;

            contextStoreSourceAuditPublisher.publish(
                eventType, sourceId, Map.of("workspaceId", String.valueOf(workspaceId)));
        }

        return updated;
    }

    @Override
    public void delete(Long workspaceId, Long sourceId) {
        Objects.requireNonNull(workspaceId, "workspaceId");
        Objects.requireNonNull(sourceId, "sourceId");

        ContextStoreSource source = contextStoreSourceService.get(sourceId);

        String workflowId = source.getWorkflowId();

        if (workflowId != null) {
            // Drop ProjectDeploymentWorkflow rows pointing at this workflow. Iterate per-deployment because the
            // (deployment, workflow) pair is unique but workflows are looked up by id only.
            long projectId = findOrCreateContextStoreProject(workspaceId);
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

        contextStoreSourceService.delete(sourceId);

        contextStoreSourceAuditPublisher.publish(
            ContextStoreSourceAuditEvent.CONTEXT_STORE_SOURCE_DELETED, sourceId,
            Map.of("workspaceId", String.valueOf(workspaceId)));
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public long refreshNow(Long workspaceId, Long sourceId) {
        // Propagation.NEVER required: principalJobFacade.createJob is @Transactional(propagation = NEVER) for
        // monolith-mode queue safety (see JobFacadeImpl), and the class-level @Transactional on this facade would
        // otherwise force this method into a transaction that conflicts. Each underlying service call
        // (contextStoreSourceService.get, projectService.getProject, findOrCreateProjectDeployment) manages its own
        // transaction independently — the reads are safe without an outer wrapper.
        Objects.requireNonNull(workspaceId, "workspaceId");
        Objects.requireNonNull(sourceId, "sourceId");

        ContextStoreSource source = contextStoreSourceService.get(sourceId);

        if (source.getWorkflowId() == null) {
            throw new ConfigurationException(
                "Context source '" + source.getName() + "' has no workflow; cannot refresh",
                ContextStoreSourceErrorType.MISSING_WORKFLOW);
        }

        long projectId = findOrCreateContextStoreProject(workspaceId);
        int projectVersion = projectService.getProject(projectId)
            .getLastProjectVersion();
        long projectDeploymentId = findOrCreateProjectDeployment(projectId, projectVersion);

        // Job creation runs workflow input validation synchronously (JobService.create resolves the workflow and
        // verifies required parameters), so a misconfigured entity (e.g. Airtable with empty baseId/tableId) raises
        // here. Re-throw as ConfigurationException so GlobalDataFetcherExceptionResolver surfaces the real message
        // to the client instead of the default sanitized INTERNAL_ERROR; ConfigurationException re-throws unchanged
        // so we don't double-wrap the structured MISSING_WORKFLOW above.
        try {
            return triggerSyncJob(source, projectDeploymentId, projectVersion);
        } catch (ConfigurationException configurationException) {
            throw configurationException;
        } catch (RuntimeException runtimeException) {
            throw new ConfigurationException(
                "Failed to refresh context source '" + source.getName() + "': " + runtimeException.getMessage(),
                runtimeException, ContextStoreSourceErrorType.REFRESH_FAILED);
        }
    }

    @Override
    public void setEnabled(Long workspaceId, Long sourceId, boolean enabled) {
        Objects.requireNonNull(workspaceId, "workspaceId");
        Objects.requireNonNull(sourceId, "sourceId");

        ContextStoreSource source = contextStoreSourceService.get(sourceId);

        if (source.isEnabled() == enabled) {
            return;
        }

        source.setEnabled(enabled);

        toggleProjectDeploymentWorkflow(workspaceId, source, enabled);

        contextStoreSourceService.update(source);

        ContextStoreSourceAuditEvent eventType = enabled
            ? ContextStoreSourceAuditEvent.CONTEXT_STORE_SOURCE_ENABLED
            : ContextStoreSourceAuditEvent.CONTEXT_STORE_SOURCE_DISABLED;

        contextStoreSourceAuditPublisher.publish(
            eventType, sourceId, Map.of("workspaceId", String.valueOf(workspaceId)));
    }

    private long findOrCreateContextStoreProject(Long workspaceId) {
        String projectName = CONTEXT_STORE_PROJECT_NAME_PREFIX + workspaceId;

        return projectService.fetchProject(projectName)
            .map(Project::getId)
            .orElseGet(() -> {
                Project project = new Project();

                project.setName(projectName);
                project.setDescription(
                    "System project that owns auto-generated Context Store sync workflows. Do not edit.");
                project.setWorkspaceId(workspaceId);

                Project saved = projectService.create(project);

                return saved.getId();
            });
    }

    private long findOrCreateProjectDeployment(long projectId, int projectVersion) {
        // Environment.DEVELOPMENT here is a cron-container marker, NOT the source's effective environment. The real
        // env is stamped on the parent ContextStore and propagates to records via the contextStoreId column on every
        // synced row. We reuse a single DEVELOPMENT deployment per workspace as the host for the auto-generated
        // schedule.cron → data-stream.stream workflow because the deployment env is irrelevant once the workflow
        // resolves contextStoreId; a per-env deployment would just duplicate the trigger plumbing.
        return projectDeploymentService.fetchProjectDeployment(projectId, Environment.DEVELOPMENT)
            .map(projectDeployment -> {
                // The Context Store deployment is a system-managed singleton that must stay enabled; individual
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

    private void toggleProjectDeploymentWorkflow(Long workspaceId, ContextStoreSource source, boolean enabled) {
        String workflowId = source.getWorkflowId();

        if (workflowId == null) {
            return;
        }

        long projectId = findOrCreateContextStoreProject(workspaceId);
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
     * Resolve the source-side {@code ItemReader} cluster element. When the caller supplied an explicit name, look up
     * that element by name. Otherwise auto-pick the first {@link ItemReader#SOURCE} cluster element on the source
     * component — saves the wizard from forcing a pick when there's only one ItemReader (the common case). Throws when
     * the component has no matching cluster element so the failure is loud at create-time rather than at the first sync
     * run.
     *
     * <p>
     * Returns the definition itself rather than just the name so callers can inspect its property metadata (used by
     * {@link #validateEntityRequiredParameters}) without re-fetching the component definition.
     */
    private ClusterElementDefinition resolveSourceClusterElement(
        String sourceComponentName, int sourceComponentVersion, String explicitName) {

        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
            sourceComponentName, sourceComponentVersion);

        for (ClusterElementDefinition clusterElement : componentDefinition.getClusterElements()) {
            ClusterElementType type = clusterElement.getType();

            if (!ItemReader.SOURCE.equals(type)) {
                continue;
            }

            if (explicitName == null || explicitName.equals(clusterElement.getName())) {
                return clusterElement;
            }
        }

        throw new IllegalArgumentException(
            "Component " + sourceComponentName + " v" + sourceComponentVersion
                + (explicitName == null
                    ? " has no ItemReader cluster element"
                    : " has no ItemReader cluster element named '" + explicitName + "'"));
    }

    /**
     * Refuse to create a source missing a required source-cluster-element parameter. The sync workflow would otherwise
     * blow up later — at first cron tick or on a manual Refresh Now — with an opaque "input X is required" surfaced
     * through the GraphQL pipeline. Catching it here lets the dialog show a precise field list before any DB rows or
     * workflows are created.
     *
     * <p>
     * Treats {@code null}, missing keys, and blank strings as "missing." Non-string values are accepted as-is — the
     * downstream component does its own typed validation; we only guard the empty-input case here.
     */
    private void validateRequiredParameters(
        ClusterElementDefinition sourceClusterElement, CreateContextStoreSourceInput input) {

        List<String> requiredParameterNames = new ArrayList<>();

        for (Property property : sourceClusterElement.getProperties()) {
            if (property.getRequired()) {
                requiredParameterNames.add(property.getName());
            }
        }

        if (requiredParameterNames.isEmpty()) {
            return;
        }

        Map<String, ?> parameters = input.parameters();
        List<String> missing = new ArrayList<>();

        for (String parameterName : requiredParameterNames) {
            Object value = parameters == null ? null : parameters.get(parameterName);

            if (value == null || (value instanceof String stringValue && stringValue.isBlank())) {
                missing.add(parameterName);
            }
        }

        if (!missing.isEmpty()) {
            throw new ConfigurationException(
                "Cannot create context source '" + input.entityName() + "': missing required parameters " + missing,
                ContextStoreSourceErrorType.MISSING_REQUIRED_PARAMETER);
        }
    }

    private long triggerSyncJob(ContextStoreSource source, long projectDeploymentId, int projectVersion) {
        String workflowId = Objects.requireNonNull(source.getWorkflowId(), "workflowId");

        return principalJobFacade.createJob(
            new JobParametersDTO(
                workflowId, Map.of(),
                Map.of("projectVersion", projectVersion)),
            projectDeploymentId, PlatformType.AUTOMATION);
    }
}

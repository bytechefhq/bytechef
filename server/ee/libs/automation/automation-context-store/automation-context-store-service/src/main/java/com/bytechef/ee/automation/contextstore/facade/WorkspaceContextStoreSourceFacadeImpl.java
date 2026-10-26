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
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.ownership.ProjectOwner;
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
import com.bytechef.ee.automation.contextstore.ownership.ContextStoreOwnerContributorConfiguration;
import com.bytechef.ee.automation.contextstore.service.WorkspaceContextStoreService;
import com.bytechef.ee.automation.contextstore.util.ContextStoreWorkflowGenerator;
import com.bytechef.ee.platform.contextstore.clickhouse.ClickHouseTableProvisioner;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSource;
import com.bytechef.ee.platform.contextstore.domain.ContextStoreSourceStatus;
import com.bytechef.ee.platform.contextstore.domain.TombstoneStrategy;
import com.bytechef.ee.platform.contextstore.exception.ContextStoreSourceErrorType;
import com.bytechef.ee.platform.contextstore.service.ContextStoreService;
import com.bytechef.ee.platform.contextstore.service.ContextStoreSourceService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.workflow.WorkflowPreDeleteListener;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
@ConditionalOnProperty(prefix = "bytechef.context-store", name = "enabled", havingValue = "true")
public class WorkspaceContextStoreSourceFacadeImpl implements WorkspaceContextStoreSourceFacade {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceContextStoreSourceFacadeImpl.class);

    static final String CONTEXT_STORE_PROJECT_NAME_PREFIX =
        ContextStoreOwnerContributorConfiguration.CONTEXT_STORE_PROJECT_NAME_PREFIX;

    private final ObjectProvider<ClickHouseTableProvisioner> clickHouseTableProvisionerProvider;
    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectionService connectionService;
    private final ContextStoreSourceAuditPublisher contextStoreSourceAuditPublisher;
    private final ContextStoreService contextStoreService;
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
        ComponentDefinitionService componentDefinitionService, ConnectionService connectionService,
        ContextStoreSourceAuditPublisher contextStoreSourceAuditPublisher, ContextStoreService contextStoreService,
        ContextStoreSourceService contextStoreSourceService, PrincipalJobFacade principalJobFacade,
        ProjectDeploymentFacade projectDeploymentFacade, ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService,
        TaskExecutor taskExecutor, WorkflowService workflowService,
        WorkspaceContextStoreService workspaceContextStoreService,
        List<WorkflowPreDeleteListener> workflowPreDeleteListeners) {

        this.clickHouseTableProvisionerProvider = clickHouseTableProvisionerProvider;
        this.componentDefinitionService = componentDefinitionService;
        this.connectionService = connectionService;
        this.contextStoreSourceAuditPublisher = contextStoreSourceAuditPublisher;
        this.contextStoreService = contextStoreService;
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

        if (!workspaceContextStoreService.isStoreInWorkspace(workspaceId, input.contextStoreId())) {
            throw new IllegalArgumentException(
                "Context Store " + input.contextStoreId() + " is not in workspace " + workspaceId);
        }

        Environment environment = contextStoreService.get(input.contextStoreId())
            .getEnvironment();

        validateConnectionEnvironment(input.connectionId(), environment);

        ClusterElementDefinition sourceClusterElement = resolveSourceClusterElement(
            input.sourceComponentName(), input.sourceComponentVersion(), input.sourceClusterElementName());

        String resolvedClusterElementName = sourceClusterElement.getName();

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
        sourceToInsert.setTombstoneStrategy(
            input.tombstoneStrategy() == null ? TombstoneStrategy.PERIODIC_FULL_REPLACE : input.tombstoneStrategy());

        ClickHouseTableProvisioner provisioner = clickHouseTableProvisionerProvider.getIfAvailable();

        sourceToInsert.setWorkspaceId(workspaceId);

        ContextStoreSource source = contextStoreSourceService.create(sourceToInsert);

        if (provisioner != null) {
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

        long projectDeploymentId = findOrCreateProjectDeployment(projectId, projectVersion, environment);

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

        ContextStoreSource updated = contextStoreSourceService.update(source);

        Map<String, Object> createdData = new HashMap<>();

        createdData.put("workspaceId", String.valueOf(workspaceId));

        if (updated.getName() != null) {
            createdData.put("name", updated.getName());
        }

        contextStoreSourceAuditPublisher.publish(
            ContextStoreSourceAuditEvent.CONTEXT_STORE_SOURCE_CREATED, updated.getId(), createdData);

        registerAfterCommitSyncTrigger(updated, projectDeploymentId, projectVersion);

        return updated;
    }

    private void registerAfterCommitSyncTrigger(
        ContextStoreSource source, long projectDeploymentId, int projectVersion) {

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
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
            long projectId = findOrCreateContextStoreProject(workspaceId);
            int projectVersion = projectService.getProject(projectId)
                .getLastProjectVersion();

            for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflowService
                .getWorkflowProjectDeploymentWorkflows(workflowId)) {

                projectDeploymentFacade.enableProjectDeploymentWorkflow(
                    projectDeploymentWorkflow.getProjectDeploymentId(), workflowId, false);

                projectDeploymentWorkflowService.delete(projectDeploymentWorkflow.getId());
            }

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
        long projectDeploymentId = resolveProjectDeploymentId(source, projectId, projectVersion);

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

        Set<ProjectOwner> excludedProjectOwners = EnumSet.complementOf(EnumSet.of(ProjectOwner.CONTEXT_STORE));

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
                    "System project that owns auto-generated Context Store sync workflows. Do not edit.");
                project.setWorkspaceId(workspaceId);

                Project saved = projectService.create(project);

                return saved.getId();
            });
    }

    private long findOrCreateProjectDeployment(long projectId, int projectVersion, Environment environment) {
        return projectDeploymentService.fetchProjectDeployment(projectId, environment)
            .map(projectDeployment -> {
                ensureProjectDeploymentEnabled(projectDeployment);

                return projectDeployment.getId();
            })
            .orElseGet(() -> {
                ProjectDeployment projectDeployment = new ProjectDeployment();

                Project project = projectService.getProject(projectId);

                projectDeployment.setName(project.getName());
                projectDeployment.setProjectId(projectId);
                projectDeployment.setProjectVersion(projectVersion);
                projectDeployment.setEnvironment(environment);

                ProjectDeployment saved = projectDeploymentService.create(projectDeployment);

                projectDeploymentService.updateEnabled(saved.getId(), true);

                return saved.getId();
            });
    }

    private void ensureProjectDeploymentEnabled(ProjectDeployment projectDeployment) {
        if (!projectDeployment.isEnabled()) {
            projectDeploymentService.updateEnabled(projectDeployment.getId(), true);
        }
    }

    private Environment getStoreEnvironment(ContextStoreSource source) {
        return contextStoreService.get(source.getContextStoreId())
            .getEnvironment();
    }

    private long resolveProjectDeploymentId(ContextStoreSource source, long projectId, int projectVersion) {
        List<ProjectDeploymentWorkflow> projectDeploymentWorkflows = projectDeploymentWorkflowService
            .getWorkflowProjectDeploymentWorkflows(source.getWorkflowId());

        if (projectDeploymentWorkflows.isEmpty()) {
            return findOrCreateProjectDeployment(projectId, projectVersion, getStoreEnvironment(source));
        }

        ProjectDeploymentWorkflow projectDeploymentWorkflow = projectDeploymentWorkflows.getFirst();

        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(
            projectDeploymentWorkflow.getProjectDeploymentId());

        ensureProjectDeploymentEnabled(projectDeployment);

        return projectDeployment.getId();
    }

    private void toggleProjectDeploymentWorkflow(Long workspaceId, ContextStoreSource source, boolean enabled) {
        String workflowId = source.getWorkflowId();

        if (workflowId == null) {
            return;
        }

        long projectId = findOrCreateContextStoreProject(workspaceId);
        int projectVersion = projectService.getProject(projectId)
            .getLastProjectVersion();
        long projectDeploymentId = resolveProjectDeploymentId(source, projectId, projectVersion);

        projectDeploymentFacade.enableProjectDeploymentWorkflow(projectDeploymentId, workflowId, enabled);
    }

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

    private void validateConnectionEnvironment(Long connectionId, Environment environment) {
        if (connectionId == null) {
            return;
        }

        Connection connection = connectionService.getConnection(connectionId);

        if (connection.getEnvironmentId() != environment.ordinal()) {
            throw new ConfigurationException(
                "Connection '%s' does not belong to the Context Store's environment %s".formatted(
                    connection.getName(), environment.name()),
                ContextStoreSourceErrorType.CONNECTION_ENVIRONMENT_MISMATCH);
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

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

package com.bytechef.automation.datasync.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectVersion;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.ProjectWorkflowType;
import com.bytechef.automation.configuration.domain.SystemProjects;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.datasync.domain.DataSync;
import com.bytechef.automation.datasync.domain.DataSync.TriggerType;
import com.bytechef.automation.datasync.domain.DataSyncElement;
import com.bytechef.automation.datasync.domain.DataSyncElement.Kind;
import com.bytechef.automation.datasync.dto.DataSyncDTO;
import com.bytechef.automation.datasync.dto.DataSyncDeploymentDTO;
import com.bytechef.automation.datasync.dto.DataSyncVersionDTO;
import com.bytechef.automation.datasync.exception.DataSyncErrorType;
import com.bytechef.automation.datasync.service.DataSyncElementService;
import com.bytechef.automation.datasync.service.DataSyncService;
import com.bytechef.automation.datasync.util.DataSyncWorkflowGenerator;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.configuration.workflow.WorkflowPreDeleteListener;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Modelled on {@code AiAgentFacadeImpl}: rows are the truth, and the Data Sync's generated workflow in its project's
 * draft version is regenerated on every mutation. A Data Sync is published with its project:
 * {@code DataSyncProjectPublishPreListener} calls {@link #prepareProjectPublish} before the project version is minted.
 *
 * @author Ivica Cardic
 */
@Service
public class DataSyncFacadeImpl implements DataSyncFacade {

    private static final int MAX_NAME_LENGTH = 64;

    private static final String EXPORT_VERSION = "exportVersion";
    private static final int EXPORT_VERSION_VALUE = 1;
    private static final String EXPORT_NAME = "name";
    private static final String EXPORT_TITLE = "title";
    private static final String EXPORT_DESCRIPTION = "description";
    private static final String EXPORT_TRIGGER_TYPE = "triggerType";
    private static final String EXPORT_TRIGGER_PARAMETERS = "triggerParameters";
    private static final String EXPORT_ELEMENTS = "elements";
    private static final String EXPORT_KIND = "kind";
    private static final String EXPORT_COMPONENT_NAME = "componentName";
    private static final String EXPORT_COMPONENT_VERSION = "componentVersion";
    private static final String EXPORT_OPERATION_NAME = "operationName";
    private static final String EXPORT_PARAMETERS = "parameters";

    private final ComponentDefinitionService componentDefinitionService;
    private final DataSyncElementService dataSyncElementService;
    private final DataSyncService dataSyncService;
    private final JobService jobService;
    private final PrincipalJobService principalJobService;
    private final ProjectDeploymentFacade projectDeploymentFacade;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final List<WorkflowPreDeleteListener> workflowPreDeleteListeners;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI2")
    public DataSyncFacadeImpl(
        ComponentDefinitionService componentDefinitionService, DataSyncElementService dataSyncElementService,
        DataSyncService dataSyncService, JobService jobService, PrincipalJobService principalJobService,
        ProjectDeploymentFacade projectDeploymentFacade, ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService,
        List<WorkflowPreDeleteListener> workflowPreDeleteListeners, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.componentDefinitionService = componentDefinitionService;
        this.dataSyncElementService = dataSyncElementService;
        this.dataSyncService = dataSyncService;
        this.jobService = jobService;
        this.principalJobService = principalJobService;
        this.projectDeploymentFacade = projectDeploymentFacade;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowPreDeleteListeners = workflowPreDeleteListeners;
        this.workflowService = workflowService;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'DATA_SYNC_CREATE') and " +
        "(#projectId == null or hasPermission(#projectId, 'Project', 'WORKFLOW_CREATE'))")
    @Transactional
    public DataSyncDTO createDataSync(
        String title, @Nullable String description, long workspaceId, @Nullable Long projectId) {

        Objects.requireNonNull(title, "title");

        ProjectDataSync projectDataSync = createDataSync(title, description, workspaceId, projectId, title);

        regenerateAndSaveWorkflow(projectDataSync.dataSync(), projectDataSync.projectId());

        return toDataSyncDTO(projectDataSync.dataSync(), projectDataSync.projectId());
    }

    /**
     * The one path every new Data Sync takes — created, imported or copied — so each gets the same project handling and
     * its own placeholder workflow in the project's draft version. The caller writes whatever else the Data Sync
     * carries and then regenerates the draft once from the finished rows.
     */
    private ProjectDataSync createDataSync(
        String title, @Nullable String description, long workspaceId, @Nullable Long projectId, String baseName) {

        String name = uniqueName(slugify(baseName), workspaceId);

        Project project = projectId == null
            ? createDataSyncProject(title, description, workspaceId)
            : getWorkspaceProject(projectId, workspaceId);

        DataSync dataSync = new DataSync();

        dataSync.setName(name);
        dataSync.setTitle(title);
        dataSync.setDescription(description);
        dataSync.setWorkspaceId(project.getWorkspaceId());
        dataSync.setUuid(UUID.randomUUID());
        dataSync.setTriggerType(TriggerType.MANUAL);

        Workflow workflow = workflowService.create(
            JsonUtils.write(Map.of("label", name, "tasks", List.of())), Workflow.Format.JSON,
            Workflow.SourceType.JDBC);

        ProjectWorkflow projectWorkflow = projectWorkflowService.addWorkflow(
            project.getId(), project.getLastProjectVersion(), workflow.getId(), ProjectWorkflowType.DATA_SYNC);

        dataSync.setProjectWorkflowUuid(projectWorkflow.getUuid());

        return new ProjectDataSync(dataSyncService.create(dataSync), project.getId());
    }

    private Project createDataSyncProject(String title, @Nullable String description, long workspaceId) {
        Project project = new Project();

        project.setName(uniqueProjectName(title, workspaceId));
        project.setDescription(description);
        project.setWorkspaceId(workspaceId);

        return projectService.create(project);
    }

    /**
     * The project a new Data Sync joins must belong to the Data Sync's own workspace, so {@code data_sync.workspace_id}
     * always equals its project's {@code workspace_id}.
     */
    private Project getWorkspaceProject(long projectId, long workspaceId) {
        Project project = projectService.getProject(projectId);

        if (!Objects.equals(project.getWorkspaceId(), workspaceId) || SystemProjects.isSystemProject(project)) {

            throw new IllegalArgumentException(
                "Project " + projectId + " is not available in workspace " + workspaceId);
        }

        return project;
    }

    private String uniqueProjectName(String title, long workspaceId) {
        Set<String> projectNames = projectService.getProjects(null, null, null, null, null, workspaceId)
            .stream()
            .map(Project::getName)
            .collect(Collectors.toSet());

        if (!projectNames.contains(title)) {
            return title;
        }

        int suffix = 2;

        while (projectNames.contains(title + " (" + suffix + ")")) {
            suffix++;
        }

        return title + " (" + suffix + ")";
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'DataSync', 'DATA_SYNC_EDIT')")
    @Transactional
    public DataSyncDTO updateDataSync(long id, @Nullable String title, @Nullable String description) {
        DataSync dataSync = dataSyncService.getDataSync(id);

        if (title != null) {
            dataSync.setTitle(title);
        }

        if (description != null) {
            dataSync.setDescription(description);
        }

        DataSync updatedDataSync = dataSyncService.update(dataSync);

        regenerateAndSaveWorkflow(updatedDataSync);

        return toDataSyncDTO(updatedDataSync);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'DataSync', 'DATA_SYNC_DELETE')")
    @Transactional
    public void deleteDataSync(long id) {
        DataSync dataSync = dataSyncService.getDataSync(id);

        long projectId = dataSyncService.getProjectId(dataSync);

        if (isEnabledInAnyDeployment(dataSync, projectId)) {
            throw new ConfigurationException(
                "Data Sync " + id + " cannot be deleted while it is enabled in a deployment",
                DataSyncErrorType.DATA_SYNC_HAS_DEPLOYMENTS);
        }

        List<ProjectWorkflow> dataSyncProjectWorkflows = projectWorkflowService.getProjectWorkflows(
            projectId, String.valueOf(dataSync.getProjectWorkflowUuid()));

        dataSyncService.delete(id);

        List<String> deletedWorkflowIds = new ArrayList<>();

        // One row per project version: publishing duplicates the workflow into every new version.
        for (ProjectWorkflow dataSyncProjectWorkflow : dataSyncProjectWorkflows) {
            String workflowId = dataSyncProjectWorkflow.getWorkflowId();

            projectWorkflowService.delete(
                dataSyncProjectWorkflow.getProjectId(), dataSyncProjectWorkflow.getProjectVersion(), workflowId);

            for (WorkflowPreDeleteListener workflowPreDeleteListener : workflowPreDeleteListeners) {
                workflowPreDeleteListener.onWorkflowPreDelete(workflowId);
            }

            // The Data Sync's workflow can be disabled in a deployment (deletion is refused only while enabled), so its
            // project_deployment_workflow rows outlive the project_workflow row above unless removed here too.
            for (ProjectDeploymentWorkflow projectDeploymentWorkflow : projectDeploymentWorkflowService
                .getWorkflowProjectDeploymentWorkflows(workflowId)) {

                projectDeploymentWorkflowService.delete(projectDeploymentWorkflow.getId());
            }

            workflowService.delete(workflowId);

            deletedWorkflowIds.add(workflowId);
        }

        workflowTestConfigurationService.delete(deletedWorkflowIds);
    }

    /**
     * Deletes only the rows (elements cascade): {@code ProjectFacadeImpl.deleteProject} calls this through
     * {@code DataSyncProjectDeleteEventListener} before it removes the project's deployments, workflows and test
     * configurations itself.
     */
    @Override
    @PreAuthorize("hasPermission(#projectId, 'Project', 'PROJECT_DELETE')")
    @Transactional
    public void deleteProjectDataSyncs(long projectId) {
        for (DataSync projectDataSync : dataSyncService.getProjectDataSyncs(projectId)) {
            dataSyncService.delete(projectDataSync.getId());
        }
    }

    /**
     * Whether the Data Sync's own generated workflow is enabled in a deployment of its project. A project deployment
     * covers every workflow, agent and Data Sync of the project, so the deployment existing is not enough: the row for
     * the Data Sync's workflow has to be picked out. An ordinary project can hold several deployments per environment,
     * so every deployment is read in one pass through {@link ProjectDeploymentService#getAllProjectDeployments(long)}
     * rather than a single-result lookup per environment, which assumes at most one.
     */
    private boolean isEnabledInAnyDeployment(DataSync dataSync, long projectId) {
        for (ProjectDeployment projectDeployment : projectDeploymentService.getAllProjectDeployments(projectId)) {
            Optional<String> dataSyncWorkflowIdOptional = fetchDataSyncWorkflowId(dataSync, projectDeployment.getId());

            if (dataSyncWorkflowIdOptional.isEmpty()) {
                continue;
            }

            boolean enabled = dataSyncDeploymentWorkflows(projectDeployment.getId(), dataSyncWorkflowIdOptional.get())
                .stream()
                .anyMatch(ProjectDeploymentWorkflow::isEnabled);

            if (enabled) {
                return true;
            }
        }

        return false;
    }

    /**
     * The id of the Data Sync's generated workflow in the project version the deployment deploys, resolved from
     * {@code data_sync.project_workflow_uuid} by {@link ProjectWorkflowService#fetchProjectWorkflowWorkflowId}. Empty
     * when that version does not contain the Data Sync — another project's deployment, or one pinned to a version from
     * before the Data Sync existed.
     */
    private Optional<String> fetchDataSyncWorkflowId(DataSync dataSync, long projectDeploymentId) {
        return projectWorkflowService.fetchProjectWorkflowWorkflowId(
            projectDeploymentId, String.valueOf(dataSync.getProjectWorkflowUuid()));
    }

    /**
     * The deployment's own {@link ProjectDeploymentWorkflow} row(s) for the Data Sync's generated workflow: a project
     * deployment carries one row per workflow of the project, so the Data Sync's row has to be picked out.
     */
    private List<ProjectDeploymentWorkflow> dataSyncDeploymentWorkflows(
        long projectDeploymentId, String dataSyncWorkflowId) {

        return projectDeploymentWorkflowService.getProjectDeploymentWorkflows(projectDeploymentId)
            .stream()
            .filter(projectDeploymentWorkflow -> dataSyncWorkflowId.equals(projectDeploymentWorkflow.getWorkflowId()))
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'DataSync', 'DATA_SYNC_VIEW')")
    @Transactional(readOnly = true)
    public DataSyncDTO getDataSync(long id) {
        return toDataSyncDTO(dataSyncService.getDataSync(id));
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'DATA_SYNC_VIEW')")
    @Transactional(readOnly = true)
    public List<DataSyncDTO> getDataSyncs(long workspaceId) {
        return workspaceDataSyncs(workspaceId)
            .stream()
            .map(projectDataSync -> toDataSyncDTO(projectDataSync.dataSync(), projectDataSync.projectId()))
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'DataSync', 'DATA_SYNC_EDIT')")
    @Transactional
    public void updateDataSyncTrigger(long id, TriggerType triggerType, @Nullable Map<String, ?> triggerParameters) {
        DataSync dataSync = dataSyncService.getDataSync(id);

        dataSync.setTriggerType(triggerType);
        dataSync.setTriggerParameters(triggerType == TriggerType.MANUAL ? null : triggerParameters);

        regenerateAndSaveWorkflow(dataSyncService.update(dataSync));
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'DataSync', 'DATA_SYNC_EDIT')")
    @Transactional
    public DataSyncElement setDataSyncElement(
        long id, Kind kind, String componentName, int componentVersion, String operationName,
        @Nullable Map<String, ?> parameters, @Nullable Long connectionId) {

        DataSync dataSync = dataSyncService.getDataSync(id);

        if (kind == Kind.PROCESSOR && !isFieldMapper(componentName, componentVersion, operationName)) {
            throw new ConfigurationException(
                "The processor of a Data Sync must be dataStreamProcessor/v1/fieldMapper",
                DataSyncErrorType.PROCESSOR_NOT_FIELD_MAPPER);
        }

        List<DataSyncElement> elements = dataSyncElementService.getByDataSyncId(id);

        Optional<DataSyncElement> existing = elements.stream()
            .filter(element -> element.getKind() == kind)
            .findFirst();

        boolean sideComponentChanged = kind != Kind.PROCESSOR && existing
            .map(element -> !Objects.equals(element.getComponentName(), componentName)
                || element.getComponentVersion() != componentVersion
                || !Objects.equals(element.getOperationName(), operationName))
            .orElse(true);

        DataSyncElement element = existing.orElseGet(() -> new DataSyncElement(id, kind));

        element.setComponentName(componentName);
        element.setComponentVersion(componentVersion);
        element.setOperationName(operationName);
        element.setParameters(parameters);
        element.setConnectionId(connectionId);

        DataSyncElement savedElement = existing.isPresent()
            ? dataSyncElementService.update(element)
            : dataSyncElementService.create(element);

        if (sideComponentChanged) {
            elements.stream()
                .filter(candidate -> candidate.getKind() == Kind.PROCESSOR)
                .findFirst()
                .ifPresent(processor -> dataSyncElementService.delete(processor.getId()));
        }

        regenerateAndSaveWorkflow(dataSync);

        return savedElement;
    }

    @Override
    @PreAuthorize("hasPermission(#elementId, 'DataSyncElement', 'DATA_SYNC_EDIT')")
    @Transactional
    public void updateDataSyncElement(
        long elementId, @Nullable Map<String, ?> parameters, @Nullable Long connectionId) {

        DataSyncElement element = dataSyncElementService.getDataSyncElement(elementId);

        element.setParameters(parameters);
        element.setConnectionId(connectionId);

        dataSyncElementService.update(element);

        regenerateAndSaveWorkflow(dataSyncService.getDataSync(element.getDataSyncId()));
    }

    /**
     * Regenerates each Data Sync's draft workflow in place and never adds or removes a {@code project_workflow} row:
     * {@code ProjectFacadeImpl.publishProject} snapshots the draft's workflow list before the pre-listeners run and
     * duplicates exactly that list into the new version.
     */
    @Override
    @PreAuthorize("hasPermission(#projectId, 'Project', 'DATA_SYNC_PUBLISH')")
    @Transactional
    public void prepareProjectPublish(long projectId) {
        for (DataSync dataSync : dataSyncService.getProjectDataSyncs(projectId)) {
            validateForPublish(dataSync);

            regenerateAndSaveWorkflow(dataSync, projectId);
        }
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'DataSync', 'DATA_SYNC_VIEW')")
    @Transactional(readOnly = true)
    public List<DataSyncVersionDTO> getDataSyncVersions(long id) {
        DataSync dataSync = dataSyncService.getDataSync(id);
        Project project = projectService.getProject(dataSyncService.getProjectId(dataSync));

        return project.getProjectVersions()
            .stream()
            .sorted(Comparator.comparingInt(ProjectVersion::getVersion)
                .reversed())
            .map(projectVersion -> new DataSyncVersionDTO(
                projectVersion.getVersion(), projectVersion.getDescription(), projectVersion.getPublishedDate(),
                String.valueOf(projectVersion.getStatus())))
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'DATA_SYNC_VIEW')")
    @Transactional(readOnly = true)
    public List<DataSyncDeploymentDTO> getDataSyncDeployments(long workspaceId) {
        List<DataSyncDeploymentDTO> dataSyncDeploymentDTOs = new ArrayList<>();

        for (ProjectDataSync projectDataSync : workspaceDataSyncs(workspaceId)) {
            DataSync dataSync = projectDataSync.dataSync();
            long projectId = projectDataSync.projectId();

            for (ProjectDeployment projectDeployment : projectDeploymentService.getAllProjectDeployments(projectId)) {
                Optional<String> dataSyncWorkflowIdOptional = fetchDataSyncWorkflowId(
                    dataSync, projectDeployment.getId());

                if (dataSyncWorkflowIdOptional.isEmpty()) {
                    continue;
                }

                String dataSyncWorkflowId = dataSyncWorkflowIdOptional.get();

                List<ProjectDeploymentWorkflow> dataSyncDeploymentWorkflows = dataSyncDeploymentWorkflows(
                    projectDeployment.getId(), dataSyncWorkflowId);

                if (dataSyncDeploymentWorkflows.isEmpty()) {
                    continue;
                }

                dataSyncDeploymentDTOs.add(
                    toDeploymentDTO(
                        dataSync, projectId, projectDeployment, dataSyncWorkflowId, dataSyncDeploymentWorkflows));
            }
        }

        return dataSyncDeploymentDTOs;
    }

    /**
     * Not {@code @Transactional}: {@code ProjectDeploymentFacadeImpl.createProjectDeploymentWorkflowJob} is declared
     * {@code Propagation.NEVER}, so an enclosing transaction here would make it throw.
     * <p>
     * A deployment is this Data Sync's when its deployed project version contains the Data Sync's workflow, which also
     * rejects a deployment of the right project pinned to a version from before the Data Sync existed. In a project
     * holding several workflows the per-workflow switch is how a Data Sync is turned off, so a disabled row for the
     * Data Sync's workflow refuses Run now just as a disabled deployment does.
     */
    @Override
    @PreAuthorize("hasPermission(#id, 'DataSync', 'DATA_SYNC_EDIT')")
    public long runDataSyncDeployment(long id, long projectDeploymentId) {
        DataSync dataSync = dataSyncService.getDataSync(id);

        String dataSyncWorkflowId = fetchDataSyncWorkflowId(dataSync, projectDeploymentId)
            .orElseThrow(() -> new ConfigurationException(
                "Deployment " + projectDeploymentId + " does not deploy Data Sync '" + dataSync.getTitle() + "'",
                DataSyncErrorType.DEPLOYMENT_NOT_OWNED));

        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(projectDeploymentId);

        if (!projectDeployment.isEnabled()) {
            throw new ConfigurationException(
                "Deployment " + projectDeploymentId + " is disabled", DataSyncErrorType.DEPLOYMENT_DISABLED);
        }

        boolean dataSyncEnabled = dataSyncDeploymentWorkflows(projectDeploymentId, dataSyncWorkflowId)
            .stream()
            .anyMatch(ProjectDeploymentWorkflow::isEnabled);

        if (!dataSyncEnabled) {
            throw new ConfigurationException(
                "Data Sync '" + dataSync.getTitle() + "' is disabled in deployment " + projectDeploymentId,
                DataSyncErrorType.DEPLOYMENT_DISABLED);
        }

        return projectDeploymentFacade.createProjectDeploymentWorkflowJob(projectDeploymentId, dataSyncWorkflowId);
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'DATA_SYNC_CREATE') and " +
        "hasPermission(#targetProjectId, 'Project', 'WORKFLOW_CREATE')")
    @Transactional
    public List<DataSyncDTO> copyProjectDataSyncs(long sourceProjectId, long targetProjectId, long workspaceId) {
        getWorkspaceProject(sourceProjectId, workspaceId);

        List<DataSyncDTO> copiedDataSyncDTOs = new ArrayList<>();

        for (DataSync sourceDataSync : dataSyncService.getProjectDataSyncs(sourceProjectId)) {
            ProjectDataSync projectDataSync = createDataSync(
                sourceDataSync.getTitle(), sourceDataSync.getDescription(), workspaceId, targetProjectId,
                sourceDataSync.getName());

            DataSync copiedDataSync = projectDataSync.dataSync();

            copiedDataSync.setTriggerType(sourceDataSync.getTriggerType());
            copiedDataSync.setTriggerParameters(sourceDataSync.getTriggerParameters());

            DataSync updatedDataSync = dataSyncService.update(copiedDataSync);

            for (DataSyncElement sourceElement : dataSyncElementService.getByDataSyncId(sourceDataSync.getId())) {
                DataSyncElement element = new DataSyncElement(updatedDataSync.getId(), sourceElement.getKind());

                element.setComponentName(sourceElement.getComponentName());
                element.setComponentVersion(sourceElement.getComponentVersion());
                element.setOperationName(sourceElement.getOperationName());
                element.setParameters(sourceElement.getParameters());
                element.setConnectionId(sourceElement.getConnectionId());

                dataSyncElementService.create(element);
            }

            regenerateAndSaveWorkflow(updatedDataSync, projectDataSync.projectId());

            copiedDataSyncDTOs.add(toDataSyncDTO(updatedDataSync, projectDataSync.projectId()));
        }

        return copiedDataSyncDTOs;
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'DataSync', 'DATA_SYNC_VIEW')")
    @Transactional(readOnly = true)
    public String exportDataSync(long id) {
        DataSync dataSync = dataSyncService.getDataSync(id);

        List<Map<String, Object>> exportedElements = dataSyncElementService.getByDataSyncId(id)
            .stream()
            .sorted(Comparator.comparing(DataSyncElement::getKind))
            .map(DataSyncFacadeImpl::toExportedElement)
            .toList();

        Map<String, Object> exportedDataSync = new LinkedHashMap<>();

        exportedDataSync.put(EXPORT_VERSION, EXPORT_VERSION_VALUE);
        exportedDataSync.put(EXPORT_NAME, dataSync.getName());
        exportedDataSync.put(EXPORT_TITLE, dataSync.getTitle());
        exportedDataSync.put(EXPORT_DESCRIPTION, dataSync.getDescription());
        exportedDataSync.put(EXPORT_TRIGGER_TYPE, dataSync.getTriggerType()
            .name());
        exportedDataSync.put(EXPORT_TRIGGER_PARAMETERS, dataSync.getTriggerParameters());
        exportedDataSync.put(EXPORT_ELEMENTS, exportedElements);

        return JsonUtils.writeWithDefaultPrettyPrinter(exportedDataSync);
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'DATA_SYNC_CREATE') and " +
        "(#projectId == null or hasPermission(#projectId, 'Project', 'WORKFLOW_CREATE'))")
    @Transactional
    public DataSyncDTO importDataSync(long workspaceId, String json, @Nullable Long projectId) {
        ExportedDataSync exportedDataSync = readExportedDataSync(json);

        String exportedName = exportedDataSync.name();

        ProjectDataSync projectDataSync = createDataSync(
            exportedDataSync.title(), exportedDataSync.description(), workspaceId, projectId,
            exportedName == null || exportedName.isBlank() ? exportedDataSync.title() : exportedName);

        DataSync dataSync = projectDataSync.dataSync();

        applyExportedTrigger(dataSync, exportedDataSync);

        DataSync updatedDataSync = dataSyncService.update(dataSync);

        for (ExportedElement exportedElement : exportedDataSync.elements()) {
            DataSyncElement element = new DataSyncElement(updatedDataSync.getId(), exportedElement.kind());

            applyExportedElement(element, exportedElement);

            dataSyncElementService.create(element);
        }

        regenerateAndSaveWorkflow(updatedDataSync, projectDataSync.projectId());

        return toDataSyncDTO(updatedDataSync, projectDataSync.projectId());
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'DataSync', 'DATA_SYNC_EDIT')")
    @Transactional
    public DataSyncDTO updateDataSyncFromExport(long id, String json) {
        ExportedDataSync exportedDataSync = readExportedDataSync(json);

        DataSync dataSync = dataSyncService.getDataSync(id);

        dataSync.setTitle(exportedDataSync.title());
        dataSync.setDescription(exportedDataSync.description());

        applyExportedTrigger(dataSync, exportedDataSync);

        DataSync updatedDataSync = dataSyncService.update(dataSync);

        Map<Kind, DataSyncElement> existingElements = dataSyncElementService.getByDataSyncId(id)
            .stream()
            .collect(Collectors.toMap(DataSyncElement::getKind, element -> element));

        for (ExportedElement exportedElement : exportedDataSync.elements()) {
            DataSyncElement existingElement = existingElements.remove(exportedElement.kind());

            if (existingElement == null) {
                DataSyncElement element = new DataSyncElement(id, exportedElement.kind());

                applyExportedElement(element, exportedElement);

                dataSyncElementService.create(element);

                continue;
            }

            // A connection belongs to a component: it survives only while the element still runs the same operation
            // of the same component version.
            Long connectionId = isSameOperation(existingElement, exportedElement)
                ? existingElement.getConnectionId()
                : null;

            applyExportedElement(existingElement, exportedElement);

            existingElement.setConnectionId(connectionId);

            dataSyncElementService.update(existingElement);
        }

        for (DataSyncElement unmatchedElement : existingElements.values()) {
            dataSyncElementService.delete(unmatchedElement.getId());
        }

        regenerateAndSaveWorkflow(updatedDataSync);

        return toDataSyncDTO(dataSyncService.getDataSync(id));
    }

    // --- export documents -----------------------------------------------------------------------------------------

    /**
     * Reads and validates the whole document before anything is written, so a rejected document leaves no project, Data
     * Sync or element behind.
     */
    private static ExportedDataSync readExportedDataSync(String json) {
        Map<String, ?> exportedDataSync;

        try {
            exportedDataSync = JsonUtils.readMap(json);
        } catch (RuntimeException runtimeException) {
            throw new IllegalArgumentException("Data Sync import is not valid JSON", runtimeException);
        }

        Object exportVersion = exportedDataSync.get(EXPORT_VERSION);

        if (!(exportVersion instanceof Number number) || number.intValue() != EXPORT_VERSION_VALUE) {
            throw new IllegalArgumentException(
                "Data Sync import has unsupported " + EXPORT_VERSION + " " + exportVersion + "; expected " +
                    EXPORT_VERSION_VALUE);
        }

        String title = exportedString(exportedDataSync, EXPORT_TITLE);

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Data Sync import carries no title");
        }

        String triggerTypeName = exportedString(exportedDataSync, EXPORT_TRIGGER_TYPE);

        if (triggerTypeName == null) {
            throw new IllegalArgumentException("Data Sync import carries no " + EXPORT_TRIGGER_TYPE);
        }

        TriggerType triggerType = exportedEnum(TriggerType.class, triggerTypeName, EXPORT_TRIGGER_TYPE);

        // A missing elements list must not read as "no elements": update-from-export would then delete every element.
        if (!(exportedDataSync.get(EXPORT_ELEMENTS) instanceof List<?> exportedElements)) {
            throw new IllegalArgumentException("Data Sync import carries no " + EXPORT_ELEMENTS + " list");
        }

        List<ExportedElement> elements = new ArrayList<>();
        Set<Kind> kinds = EnumSet.noneOf(Kind.class);

        for (Object exportedElement : exportedElements) {
            // Dropping a malformed entry must not read as "fewer elements": update-from-export would then delete the
            // elements it stands for, connections included.
            if (!(exportedElement instanceof Map<?, ?> exportedElementMap)) {
                throw new IllegalArgumentException(
                    "Data Sync import carries an " + EXPORT_ELEMENTS + " entry that is not an object");
            }

            ExportedElement element = readExportedElement(exportedElementMap);

            if (!kinds.add(element.kind())) {
                throw new IllegalArgumentException("Data Sync import carries more than one " + element.kind());
            }

            elements.add(element);
        }

        return new ExportedDataSync(
            exportedString(exportedDataSync, EXPORT_NAME), title,
            exportedString(exportedDataSync, EXPORT_DESCRIPTION), triggerType,
            exportedMap(exportedDataSync.get(EXPORT_TRIGGER_PARAMETERS)), elements);
    }

    private static ExportedElement readExportedElement(Map<?, ?> exportedElement) {
        String kindName = exportedString(exportedElement, EXPORT_KIND);
        String componentName = exportedString(exportedElement, EXPORT_COMPONENT_NAME);
        String operationName = exportedString(exportedElement, EXPORT_OPERATION_NAME);

        if (kindName == null || componentName == null || componentName.isBlank() || operationName == null ||
            operationName.isBlank() || !(exportedElement.get(EXPORT_COMPONENT_VERSION) instanceof Number number)) {

            throw new IllegalArgumentException(
                "Data Sync import carries an element without kind, componentName, componentVersion or operationName");
        }

        Kind kind = exportedEnum(Kind.class, kindName, EXPORT_KIND);
        int componentVersion = number.intValue();

        if (kind == Kind.PROCESSOR && !isFieldMapper(componentName, componentVersion, operationName)) {
            throw new ConfigurationException(
                "The processor of a Data Sync must be dataStreamProcessor/v1/fieldMapper",
                DataSyncErrorType.PROCESSOR_NOT_FIELD_MAPPER);
        }

        return new ExportedElement(
            kind, componentName, componentVersion, operationName,
            exportedMap(exportedElement.get(EXPORT_PARAMETERS)));
    }

    private static void applyExportedTrigger(DataSync dataSync, ExportedDataSync exportedDataSync) {
        TriggerType triggerType = exportedDataSync.triggerType();

        dataSync.setTriggerType(triggerType);
        dataSync.setTriggerParameters(triggerType == TriggerType.MANUAL ? null : exportedDataSync.triggerParameters());
    }

    private static void applyExportedElement(DataSyncElement element, ExportedElement exportedElement) {
        element.setComponentName(exportedElement.componentName());
        element.setComponentVersion(exportedElement.componentVersion());
        element.setOperationName(exportedElement.operationName());
        element.setParameters(exportedElement.parameters());
    }

    private static boolean isSameOperation(DataSyncElement element, ExportedElement exportedElement) {
        return Objects.equals(element.getComponentName(), exportedElement.componentName())
            && element.getComponentVersion() == exportedElement.componentVersion()
            && Objects.equals(element.getOperationName(), exportedElement.operationName());
    }

    /** Deliberately carries no connection: see {@link DataSyncFacade#exportDataSync(long)}. */
    private static Map<String, Object> toExportedElement(DataSyncElement element) {
        Map<String, Object> exportedElement = new LinkedHashMap<>();

        exportedElement.put(EXPORT_KIND, element.getKind()
            .name());
        exportedElement.put(EXPORT_COMPONENT_NAME, element.getComponentName());
        exportedElement.put(EXPORT_COMPONENT_VERSION, element.getComponentVersion());
        exportedElement.put(EXPORT_OPERATION_NAME, element.getOperationName());
        exportedElement.put(EXPORT_PARAMETERS, element.getParameters());

        return exportedElement;
    }

    private static <E extends Enum<E>> E exportedEnum(Class<E> enumClass, String name, String key) {
        try {
            return Enum.valueOf(enumClass, name);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new IllegalArgumentException(
                "Data Sync import carries an unknown " + key + " '" + name + "'", illegalArgumentException);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> exportedMap(@Nullable Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }

        return (Map<String, Object>) map;
    }

    private static @Nullable String exportedString(Map<?, ?> exported, String key) {
        Object value = exported.get(key);

        return value instanceof String string ? string : null;
    }

    // --- publish validation -------------------------------------------------------------------------------------

    private void validateForPublish(DataSync dataSync) {
        String dataSyncLabel = "Data Sync '" + dataSync.getTitle() + "'";

        List<DataSyncElement> elements = dataSyncElementService.getByDataSyncId(dataSync.getId());

        DataSyncElement source = elements.stream()
            .filter(element -> element.getKind() == Kind.SOURCE)
            .findFirst()
            .orElseThrow(() -> new ConfigurationException(
                dataSyncLabel + " has no source.", DataSyncErrorType.SOURCE_MISSING));

        DataSyncElement destination = elements.stream()
            .filter(element -> element.getKind() == Kind.DESTINATION)
            .findFirst()
            .orElseThrow(() -> new ConfigurationException(
                dataSyncLabel + " has no destination.", DataSyncErrorType.DESTINATION_MISSING));

        for (DataSyncElement element : List.of(source, destination)) {
            if (element.getConnectionId() != null) {
                continue;
            }

            ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
                element.getComponentName(), element.getComponentVersion());

            if (componentDefinition != null && componentDefinition.isConnectionRequired()) {
                throw new ConfigurationException(
                    "Element " + element.getKind() + " of " + dataSyncLabel + " needs a connection.",
                    DataSyncErrorType.ELEMENT_CONNECTION_MISSING);
            }
        }

        if (dataSync.getTriggerType() == TriggerType.SCHEDULE) {
            String expression = MapUtils.getString(
                dataSync.getTriggerParameters(), DataSyncWorkflowGenerator.TRIGGER_PARAMETER_EXPRESSION, "");

            if (expression.isBlank()) {
                throw new ConfigurationException(
                    dataSyncLabel + " is scheduled but has no cron expression.",
                    DataSyncErrorType.SCHEDULE_EXPRESSION_MISSING);
            }
        }
    }

    // --- deployments ----------------------------------------------------------------------------------------------

    private DataSyncDeploymentDTO toDeploymentDTO(
        DataSync dataSync, long projectId, ProjectDeployment projectDeployment, String dataSyncWorkflowId,
        List<ProjectDeploymentWorkflow> dataSyncDeploymentWorkflows) {

        return new DataSyncDeploymentDTO(
            projectDeployment.getId(), projectDeployment.getName(), dataSync.getId(), dataSync.getTitle(), projectId,
            (int) projectDeployment.getEnvironmentId(), projectDeployment.isEnabled(),
            projectDeployment.getProjectVersion(), dataSyncWorkflowId, getDeployedTriggerType(dataSyncWorkflowId),
            getLastExecutionDate(projectDeployment.getId(), dataSyncDeploymentWorkflows));
    }

    /**
     * The trigger of the deployed workflow, not of the Data Sync's current row: the row may have been switched to
     * another trigger since the deployed version was published, and the listing reports what actually runs.
     */
    private TriggerType getDeployedTriggerType(String dataSyncWorkflowId) {
        Workflow workflow = workflowService.getWorkflow(dataSyncWorkflowId);

        boolean scheduled = WorkflowTrigger.of(workflow)
            .stream()
            .map(WorkflowTrigger::getType)
            .anyMatch(DataSyncWorkflowGenerator.SCHEDULE_CRON_TRIGGER_TYPE::equals);

        return scheduled ? TriggerType.SCHEDULE : TriggerType.MANUAL;
    }

    /**
     * The Data Sync's most recent finished run in the deployment, derived as {@code AiAgentFacadeImpl} derives its own
     * but from the Data Sync's own row only: a project deployment also runs the project's other workflows, whose jobs
     * must not count as the Data Sync's.
     */
    private @Nullable Instant getLastExecutionDate(
        long projectDeploymentId, List<ProjectDeploymentWorkflow> dataSyncDeploymentWorkflows) {

        List<String> workflowIds = dataSyncDeploymentWorkflows.stream()
            .map(ProjectDeploymentWorkflow::getWorkflowId)
            .toList();

        if (workflowIds.isEmpty()) {
            return null;
        }

        return principalJobService.fetchLastWorkflowJobId(projectDeploymentId, workflowIds, PlatformType.AUTOMATION)
            .map(jobId -> {
                Job job = jobService.getJob(jobId);

                return job.getEndDate();
            })
            .orElse(null);
    }

    // --- draft regeneration -----------------------------------------------------------------------------------

    private void regenerateAndSaveWorkflow(DataSync dataSync) {
        regenerateAndSaveWorkflow(dataSync, dataSyncService.getProjectId(dataSync));
    }

    private void regenerateAndSaveWorkflow(DataSync dataSync, long projectId) {
        ProjectWorkflow draftProjectWorkflow = draftProjectWorkflow(dataSync, projectService.getProject(projectId));

        String workflowId = draftProjectWorkflow.getWorkflowId();
        Workflow workflow = workflowService.getWorkflow(workflowId);

        List<DataSyncElement> elements = dataSyncElementService.getByDataSyncId(dataSync.getId());

        String definition = DataSyncWorkflowGenerator.generate(dataSync, elements);

        workflowService.update(workflowId, definition, workflow.getVersion());

        syncTestConnections(workflowId, elements);
    }

    /**
     * Test runs source connections ONLY from the draft's {@code WorkflowTestConfiguration}, so every regeneration
     * re-syncs each row's {@code connectionId} into it, keyed the way the generator emits the element's
     * {@code connections} block: root node name + element node name. Synced into every {@link Environment} because the
     * row's connection is not environment-scoped and the test configuration is.
     */
    private void syncTestConnections(String workflowId, List<DataSyncElement> elements) {
        Map<Kind, Long> connectionIds = new EnumMap<>(Kind.class);

        for (DataSyncElement element : elements) {
            if (element.getConnectionId() != null) {
                connectionIds.put(element.getKind(), element.getConnectionId());
            }
        }

        for (Environment environment : Environment.values()) {
            for (Kind kind : Kind.values()) {
                String connectionKey = DataSyncWorkflowGenerator.nodeName(kind);
                Long connectionId = connectionIds.get(kind);

                if (connectionId == null) {
                    removeTestConnection(workflowId, connectionKey, environment);
                } else {
                    workflowTestConfigurationService.saveWorkflowTestConfigurationConnection(
                        workflowId, DataSyncWorkflowGenerator.TASK_NODE_NAME, connectionKey, connectionId, false,
                        environment.ordinal());
                }
            }
        }
    }

    /**
     * The test configuration is keyed by element kind, not by component, so a kind that lost its connection (or its
     * element) must lose its test connection too — otherwise the editor's Test step would run a replaced component with
     * the previous component's connection.
     */
    private void removeTestConnection(String workflowId, String connectionKey, Environment environment) {
        boolean present = workflowTestConfigurationService
            .fetchWorkflowTestConfiguration(workflowId, environment.ordinal())
            .map(WorkflowTestConfiguration::getConnections)
            .stream()
            .flatMap(List::stream)
            .anyMatch(connection -> DataSyncWorkflowGenerator.TASK_NODE_NAME.equals(connection.getWorkflowNodeName())
                && connectionKey.equals(connection.getWorkflowConnectionKey()));

        if (present) {
            workflowTestConfigurationService.deleteWorkflowTestConfigurationConnection(
                workflowId, DataSyncWorkflowGenerator.TASK_NODE_NAME, connectionKey, environment.ordinal());
        }
    }

    /**
     * The Data Sync's generated workflow in its project's current draft version. A project holds any number of
     * workflows, agents and Data Syncs, so the Data Sync's own row is found by {@code project_workflow.uuid}, which is
     * stable across versions.
     */
    private ProjectWorkflow draftProjectWorkflow(DataSync dataSync, Project project) {
        return projectWorkflowService
            .fetchProjectWorkflow(
                project.getId(), project.getLastProjectVersion(), String.valueOf(dataSync.getProjectWorkflowUuid()))
            .orElseThrow(() -> new IllegalStateException(
                "Data Sync " + dataSync.getId() + " has no workflow in project " + project.getId() + " version "
                    + project.getLastProjectVersion()));
    }

    private static boolean isFieldMapper(String componentName, int componentVersion, String operationName) {
        return DataSyncElement.PROCESSOR_COMPONENT_NAME.equals(componentName)
            && DataSyncElement.PROCESSOR_COMPONENT_VERSION == componentVersion
            && DataSyncElement.PROCESSOR_OPERATION_NAME.equals(operationName);
    }

    // --- read model ---------------------------------------------------------------------------------------------

    private DataSyncDTO toDataSyncDTO(DataSync dataSync) {
        return toDataSyncDTO(dataSync, dataSyncService.getProjectId(dataSync));
    }

    private DataSyncDTO toDataSyncDTO(DataSync dataSync, long projectId) {
        List<DataSyncElement> elements = dataSyncElementService.getByDataSyncId(dataSync.getId());

        Project project = projectService.getProject(projectId);
        ProjectVersion lastPublishedProjectVersion = project.getLastPublishedProjectVersion();

        ProjectWorkflow draftProjectWorkflow = draftProjectWorkflow(dataSync, project);

        Workflow draftWorkflow = workflowService.getWorkflow(draftProjectWorkflow.getWorkflowId());

        boolean unpublishedChanges;
        int lastPublishedVersion;
        Instant publishedDate;

        if (lastPublishedProjectVersion == null) {
            unpublishedChanges = true;
            lastPublishedVersion = 0;
            publishedDate = null;
        } else {
            // A Data Sync added to the project after its last publish has no row in that published version, so it has
            // never been published even though the project has.
            Optional<ProjectWorkflow> publishedProjectWorkflow = projectWorkflowService.fetchProjectWorkflow(
                projectId, lastPublishedProjectVersion.getVersion(), String.valueOf(dataSync.getProjectWorkflowUuid()));

            if (publishedProjectWorkflow.isEmpty()) {
                unpublishedChanges = true;
                lastPublishedVersion = 0;
                publishedDate = null;
            } else {
                lastPublishedVersion = lastPublishedProjectVersion.getVersion();
                publishedDate = lastPublishedProjectVersion.getPublishedDate();

                ProjectWorkflow publishedWorkflow = publishedProjectWorkflow.get();

                Workflow workflow = workflowService.getWorkflow(publishedWorkflow.getWorkflowId());

                unpublishedChanges = !Objects.equals(draftWorkflow.getDefinition(), workflow.getDefinition());
            }
        }

        Instant lastModifiedDate = draftWorkflow.getLastModifiedDate();

        if (lastModifiedDate == null) {
            lastModifiedDate = dataSync.getLastModifiedDate();
        }

        return new DataSyncDTO(
            dataSync, projectId, elements, unpublishedChanges, lastPublishedVersion, publishedDate, lastModifiedDate,
            draftWorkflow.getId());
    }

    private List<ProjectDataSync> workspaceDataSyncs(long workspaceId) {
        List<DataSync> dataSyncs = dataSyncService.getDataSyncs(workspaceId);

        if (dataSyncs.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> projectIds = dataSyncService.getProjectIds(dataSyncs);

        return dataSyncs.stream()
            .map(dataSync -> new ProjectDataSync(dataSync, projectIds.get(dataSync.getId())))
            .toList();
    }

    private static String slugify(String title) {
        if (title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }

        String lower = title.toLowerCase(Locale.ROOT);
        String collapsed = lower.replaceAll("[^a-z0-9_-]+", "-");
        String trimmed = collapsed.replaceAll("^-+|-+$", "");

        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(
                "title '" + title + "' produces an empty slug — use letters, digits, hyphens, or underscores");
        }

        return trimmed.length() > MAX_NAME_LENGTH ? trimmed.substring(0, MAX_NAME_LENGTH) : trimmed;
    }

    private String uniqueName(String baseName, Long workspaceId) {
        Set<String> existingNames = dataSyncService.getDataSyncs(workspaceId)
            .stream()
            .map(DataSync::getName)
            .collect(Collectors.toSet());

        if (!existingNames.contains(baseName)) {
            return baseName;
        }

        for (int suffix = 2;; suffix++) {
            String suffixText = "-" + suffix;
            String truncatedBase = baseName.length() > MAX_NAME_LENGTH - suffixText.length()
                ? baseName.substring(0, MAX_NAME_LENGTH - suffixText.length())
                : baseName;
            String candidate = truncatedBase + suffixText;

            if (!existingNames.contains(candidate)) {
                return candidate;
            }
        }
    }

    private record ProjectDataSync(DataSync dataSync, long projectId) {
    }

    private record ExportedDataSync(
        @Nullable String name, String title, @Nullable String description, TriggerType triggerType,
        Map<String, Object> triggerParameters, List<ExportedElement> elements) {
    }

    private record ExportedElement(
        Kind kind, String componentName, int componentVersion, String operationName, Map<String, Object> parameters) {
    }
}

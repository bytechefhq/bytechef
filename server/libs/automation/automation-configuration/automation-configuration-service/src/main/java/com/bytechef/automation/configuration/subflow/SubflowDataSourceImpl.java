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

package com.bytechef.automation.configuration.subflow;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.SystemProjects;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.definition.BaseProperty;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.util.SchemaUtils;
import com.bytechef.platform.workflow.task.dispatcher.definition.PropertyFactory;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowDataSource;
import com.bytechef.platform.workflow.task.dispatcher.subflow.domain.SubflowEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
class SubflowDataSourceImpl implements SubflowDataSource {

    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final UserService userService;
    private final WorkflowService workflowService;
    private final WorkspaceFacade workspaceFacade;

    SubflowDataSourceImpl(
        ProjectService projectService, ProjectWorkflowService projectWorkflowService, UserService userService,
        WorkflowService workflowService, WorkspaceFacade workspaceFacade) {

        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.userService = userService;
        this.workflowService = workflowService;
        this.workspaceFacade = workspaceFacade;
    }

    @Override
    public BaseProperty.@Nullable BaseValueProperty<?> getSubWorkflowInputSchema(String workflowUuid) {
        String workflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);

        if (!isReferencedWorkflowAccessible(workflowId)) {
            return null;
        }

        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowTrigger callableTrigger = getCallableTrigger(workflow, WorkflowConstants.NEW_WORKFLOW_CALL);

        if (callableTrigger == null) {
            return null;
        }

        String inputSchema = MapUtils.getString(callableTrigger.getParameters(), WorkflowConstants.INPUT_SCHEMA);

        if (inputSchema == null || inputSchema.isEmpty()) {
            return null;
        }

        return SchemaUtils.getJsonSchemaProperty(inputSchema, PropertyFactory.JSON_SCHEMA_PROPERTY_FACTORY);
    }

    @Override
    public BaseProperty.@Nullable BaseValueProperty<?> getSubWorkflowOutputSchema(String workflowUuid) {
        String workflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);

        if (!isReferencedWorkflowAccessible(workflowId)) {
            return null;
        }

        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowTask callableResponseTask = getCallableResponseTask(workflow);

        if (callableResponseTask == null) {
            return null;
        }

        String outputSchema = MapUtils.getString(callableResponseTask.getParameters(), WorkflowConstants.OUTPUT_SCHEMA);

        if (outputSchema == null || outputSchema.isEmpty()) {
            return null;
        }

        return SchemaUtils.getJsonSchemaProperty(outputSchema, PropertyFactory.JSON_SCHEMA_PROPERTY_FACTORY);
    }

    @Override
    public List<SubflowEntry> getSubWorkflows(PlatformType platformType, String triggerName, String search) {
        List<SubflowEntry> subWorkflowEntries = new ArrayList<>();

        String lowerCaseSearch = (search == null) ? null : search.toLowerCase();

        // Scope the picker to the caller's accessible workspaces so it cannot enumerate other workspaces' subflows
        // (cross-workspace IDOR). Resolved from the authenticated calling thread; when no user principal is available
        // (e.g. the EE remote option-load path before principal propagation lands) the listing is left unfiltered
        // rather than breaking the editor — it tightens automatically once the principal is present on that path.
        Set<Long> accessibleWorkspaceIds = fetchAccessibleWorkspaceIds();

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getLatestProjectWorkflows();

        for (ProjectWorkflow projectWorkflow : projectWorkflows) {
            Workflow workflow = workflowService.getWorkflow(projectWorkflow.getWorkflowId());

            if (getCallableTrigger(workflow, triggerName) != null) {
                Project project = projectService.getProject(projectWorkflow.getProjectId());

                if (accessibleWorkspaceIds != null &&
                    !accessibleWorkspaceIds.contains(project.getWorkspaceId())) {

                    continue;
                }

                String workflowLabel = workflow.getLabel() == null ? "Unnamed Workflow" : workflow.getLabel();
                String name;

                if (SystemProjects.isSystemProject(project)) {
                    if (project.getName()
                        .startsWith(SystemProjects.AI_AGENT_NAME_PREFIX)) {

                        // Published agents are backed by a hidden __AI_AGENT__ project whose generated workflow is
                        // deliberately callable from this picker — present it as an agent rather than leaking the
                        // internal project name.
                        name = "Agent > " + workflowLabel;
                    } else {

                        // Every other system project's workflows are internal to their owning feature; none happen
                        // to expose a newWorkflowCall trigger today, but skip them defensively so a future one
                        // cannot leak into this picker.
                        continue;
                    }
                } else {
                    name = project.getName() + " > " + workflowLabel;
                }

                if (lowerCaseSearch == null || lowerCaseSearch.isEmpty() ||
                    name.toLowerCase()
                        .contains(lowerCaseSearch)) {

                    subWorkflowEntries.add(new SubflowEntry(projectWorkflow.getUuidAsString(), name));
                }
            }
        }

        return subWorkflowEntries;
    }

    /**
     * Authorizes a schema read of the referenced sub-workflow. Returns {@code true} when no user principal is available
     * (e.g. workflow execution resolving the sub-workflow's I/O schema — not an authorization context); otherwise the
     * referenced workflow's owning workspace must be accessible to the caller, so the editor cannot read another
     * workspace's sub-workflow input/output schema by uuid.
     */
    private boolean isReferencedWorkflowAccessible(String workflowId) {
        Set<Long> accessibleWorkspaceIds = fetchAccessibleWorkspaceIds();

        if (accessibleWorkspaceIds == null) {
            return true;
        }

        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        Project project = projectService.getProject(projectWorkflow.getProjectId());

        return accessibleWorkspaceIds.contains(project.getWorkspaceId());
    }

    /**
     * Resolves the current user's accessible workspace ids, or {@code null} when no user principal is available (the
     * caller is then left unfiltered — see {@link #getSubWorkflows}).
     */
    private @Nullable Set<Long> fetchAccessibleWorkspaceIds() {
        return userService.fetchCurrentUser()
            .map(user -> workspaceFacade.getUserWorkspaces(user.getId())
                .stream()
                .map(Workspace::getId)
                .collect(Collectors.toSet()))
            .orElse(null);
    }

    private @Nullable WorkflowTask getCallableResponseTask(Workflow workflow) {
        List<WorkflowTask> workflowTasks = workflow.getTasks(true);

        for (WorkflowTask workflowTask : workflowTasks) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

            if (Objects.equals(workflowNodeType.name(), WorkflowConstants.WORKFLOW) &&
                Objects.equals(workflowNodeType.operation(), WorkflowConstants.RESPONSE_TO_WORKFLOW_CALL)) {

                return workflowTask;
            }
        }

        return null;
    }

    private @Nullable WorkflowTrigger getCallableTrigger(Workflow workflow, String triggerName) {
        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            if (Objects.equals(workflowNodeType.name(), WorkflowConstants.WORKFLOW) &&
                Objects.equals(workflowNodeType.operation(), triggerName)) {

                return workflowTrigger;
            }
        }

        return null;
    }
}

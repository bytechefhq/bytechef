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

package com.bytechef.automation.configuration.registry;

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.object;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.workflow.task.dispatcher.definition.JsonSchemaUtils;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.ModifiableValueProperty;
import com.bytechef.platform.workflow.task.dispatcher.registry.SubWorkflowDataSource;
import com.bytechef.platform.workflow.task.dispatcher.registry.domain.SubWorkflowEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
class SubWorkflowDataSourceImpl implements SubWorkflowDataSource {

    private static final String CALLABLE_RESPONSE_OPERATION_NAME = "callableResponse";
    private static final String CALLABLE_TRIGGER_COMPONENT_NAME = "workflow";
    private static final String CALLABLE_TRIGGER_OPERATION_NAME = "callable";
    private static final String OUTPUT_SCHEMA = "outputSchema";

    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    SubWorkflowDataSourceImpl(
        ProjectService projectService, ProjectWorkflowService projectWorkflowService,
        WorkflowService workflowService) {

        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
    }

    @Override
    public @Nullable OutputResponse getSubWorkflowInputSchema(String workflowUuid) {
        String workflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);

        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowTrigger callableTrigger = getCallableTrigger(workflow);

        if (callableTrigger == null) {
            return null;
        }

        String inputSchema = MapUtils.getString(callableTrigger.getParameters(), "inputSchema");

        if (inputSchema == null || inputSchema.isEmpty()) {
            return null;
        }

        ModifiableValueProperty<?, ?> inputProperty = JsonSchemaUtils.getProperty(inputSchema);

        if (inputProperty == null) {
            return null;
        }

        return OutputResponse.of(object().properties(inputProperty));
    }

    @Override
    public @Nullable OutputResponse getSubWorkflowOutputSchema(String workflowUuid) {
        String workflowId = projectWorkflowService.getLastWorkflowId(workflowUuid);

        Workflow workflow = workflowService.getWorkflow(workflowId);

        WorkflowTask callableResponseTask = getCallableResponseTask(workflow);

        if (callableResponseTask == null) {
            return null;
        }

        String outputSchema = MapUtils.getString(callableResponseTask.getParameters(), OUTPUT_SCHEMA);

        if (outputSchema == null || outputSchema.isEmpty()) {
            return null;
        }

        BaseProperty.BaseValueProperty<?> outputProperty = SchemaUtils.getJsonSchemaProperty(
            outputSchema, PropertyFactory.JSON_SCHEMA_PROPERTY_FACTORY);

        if (outputProperty == null) {
            return null;
        }

        return OutputResponse.of(outputProperty);
    }

    @Override
    public List<SubWorkflowEntry> getSubWorkflows(PlatformType platformType, String search) {
        List<SubWorkflowEntry> subWorkflowEntries = new ArrayList<>();

        String lowerCaseSearch = (search == null) ? null : search.toLowerCase();

        List<ProjectWorkflow> projectWorkflows = projectWorkflowService.getLatestProjectWorkflows();

        for (ProjectWorkflow projectWorkflow : projectWorkflows) {
            Workflow workflow = workflowService.getWorkflow(projectWorkflow.getWorkflowId());

            if (getCallableTrigger(workflow) != null) {
                Project project = projectService.getProject(projectWorkflow.getProjectId());

                String workflowLabel = workflow.getLabel() == null ? "Unnamed Workflow" : workflow.getLabel();
                String name = project.getName() + " > " + workflowLabel;

                if (lowerCaseSearch == null || lowerCaseSearch.isEmpty() ||
                    name.toLowerCase()
                        .contains(lowerCaseSearch)) {

                    subWorkflowEntries.add(new SubWorkflowEntry(projectWorkflow.getUuidAsString(), name));
                }
            }
        }

        return subWorkflowEntries;
    }

    private @Nullable WorkflowTask getCallableResponseTask(Workflow workflow) {
        List<WorkflowTask> workflowTasks = workflow.getTasks(true);

        for (WorkflowTask workflowTask : workflowTasks) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTask.getType());

            if (Objects.equals(workflowNodeType.name(), CALLABLE_TRIGGER_COMPONENT_NAME) &&
                Objects.equals(workflowNodeType.operation(), CALLABLE_RESPONSE_OPERATION_NAME)) {

                return workflowTask;
            }
        }

        return null;
    }

    private @Nullable WorkflowTrigger getCallableTrigger(Workflow workflow) {
        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        for (WorkflowTrigger workflowTrigger : workflowTriggers) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            if (Objects.equals(workflowNodeType.name(), CALLABLE_TRIGGER_COMPONENT_NAME) &&
                Objects.equals(workflowNodeType.operation(), CALLABLE_TRIGGER_OPERATION_NAME)) {

                return workflowTrigger;
            }
        }

        return null;
    }
}

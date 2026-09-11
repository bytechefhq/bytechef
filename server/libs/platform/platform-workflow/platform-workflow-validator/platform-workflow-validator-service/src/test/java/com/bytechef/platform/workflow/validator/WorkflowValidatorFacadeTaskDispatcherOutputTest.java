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

package com.bytechef.platform.workflow.validator;

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.integer;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.object;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.string;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.domain.ObjectProperty;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.map.MapDataSource;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.WorkflowValidationResult;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowValidatorFacadeTaskDispatcherOutputTest {

    private static final String WORKFLOW = """
        {
            "label": "workflow1",
            "description": "",
            "triggers": [],
            "tasks": [
                {
                    "label": "Map",
                    "name": "map_1",
                    "type": "map/v1",
                    "parameters": {
                        "items": ["ee"],
                        "iteratee": [
                            {
                                "label": "Var",
                                "name": "var_1",
                                "type": "var/v1/set",
                                "parameters": {"type": "STRING", "value": "${map_1.item}"}
                            }
                        ]
                    }
                },
                {
                    "label": "Subflow",
                    "name": "subflow_1",
                    "type": "subflow/v1",
                    "parameters": {"workflowUuid": "25327c41"}
                },
                {
                    "label": "Logger",
                    "name": "logger_1",
                    "type": "logger/v1/info",
                    "parameters": {"text": "${subflow_1.message}"}
                }
            ]
        }
        """;

    private static final OutputResponse SUBFLOW_OUTPUT = new OutputResponse(
        new ObjectProperty(object("subflow_1").properties(string("message"))), null);

    private final ActionDefinitionService actionDefinitionService = mock(ActionDefinitionService.class);
    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService =
        mock(TaskDispatcherDefinitionService.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    private final WorkflowValidatorFacadeImpl workflowValidatorFacade = new WorkflowValidatorFacadeImpl(
        mock(ActionDefinitionFacade.class), actionDefinitionService, mock(ClusterElementDefinitionService.class),
        mock(ComponentDefinitionService.class), taskDispatcherDefinitionService, mock(TriggerDefinitionFacade.class),
        mock(TriggerDefinitionService.class), workflowService, List.of());

    @BeforeEach
    void beforeEach() {
        ActionDefinition varActionDefinition = actionDefinition(
            ComponentDsl.string("type"), ComponentDsl.string("value"));
        ActionDefinition loggerActionDefinition = actionDefinition(ComponentDsl.string("text"));

        when(actionDefinitionService.getActionDefinition("var", 1, "set")).thenReturn(varActionDefinition);
        when(actionDefinitionService.getActionDefinition("logger", 1, "info")).thenReturn(loggerActionDefinition);
        when(taskDispatcherDefinitionService.getTaskDispatcherDefinition("map", 1))
            .thenReturn(new TaskDispatcherDefinition("map"));
        when(taskDispatcherDefinitionService.getTaskDispatcherDefinition("subflow", 1))
            .thenReturn(new TaskDispatcherDefinition("subflow"));
        when(taskDispatcherDefinitionService.executeVariableProperties(eq("map"), eq(1), anyMap()))
            .thenReturn(new OutputResponse(
                new ObjectProperty(object("map_1").properties(string("item"), integer("index"))), null));
    }

    @Test
    void taskDispatcherVariablePropertiesAndDynamicOutputsSatisfyTheReferences() {
        when(taskDispatcherDefinitionService.executeOutput(
            eq("subflow"), eq(1),
            argThat(inputParameters -> Objects.equals(inputParameters.get(MapDataSource.ENVIRONMENT_ID), 3L) &&
                !inputParameters.containsKey(MapDataSource.WORKFLOW_ID))))
                    .thenReturn(SUBFLOW_OUTPUT);

        WorkflowValidationResult result = workflowValidatorFacade.validateWorkflow(WORKFLOW, 3L);

        assertEquals(List.of(), result.errors());
        assertEquals(List.of(), result.warnings());
    }

    @Test
    void validatingByIdPassesTheWorkflowIdToTheTaskDispatcherOutput() {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getDefinition()).thenReturn(WORKFLOW);
        when(workflowService.getWorkflow("wf-1")).thenReturn(workflow);
        when(taskDispatcherDefinitionService.executeOutput(
            eq("subflow"), eq(1),
            argThat(inputParameters -> Objects.equals(inputParameters.get(MapDataSource.WORKFLOW_ID), "wf-1") &&
                Objects.equals(inputParameters.get(MapDataSource.ENVIRONMENT_ID), 3L))))
                    .thenReturn(SUBFLOW_OUTPUT);

        WorkflowValidationResult result = workflowValidatorFacade.validateWorkflowById("wf-1", 3L);

        assertEquals(List.of(), result.errors());
        assertEquals(List.of(), result.warnings());
    }

    @Test
    void anEmptyTaskDispatcherOutputLeavesTheReferenceUnresolved() {
        when(taskDispatcherDefinitionService.executeOutput(eq("subflow"), eq(1), anyMap()))
            .thenReturn(new OutputResponse(new ObjectProperty(object("subflow_1")), Map.of()));

        WorkflowValidationResult result = workflowValidatorFacade.validateWorkflow(WORKFLOW, 3L);

        assertEquals(List.of(), result.errors());
        assertEquals(
            List.of("[logger_1] Property 'subflow_1.message' might not exist in the output of 'subflow/v1'"),
            result.warnings());
    }

    private static ActionDefinition actionDefinition(ComponentDsl.ModifiableStringProperty... properties) {
        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        List<Property> propertyList = Arrays.stream(properties)
            .map(property -> (Property) Property.toProperty(property))
            .toList();

        doReturn(propertyList).when(actionDefinition)
            .getProperties();

        return actionDefinition;
    }
}

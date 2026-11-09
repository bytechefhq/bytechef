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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.evaluator.SpelEvaluator;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.WorkflowValidationResult;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowValidatorNodeTypeTest {

    private final ActionDefinitionService actionDefinitionService = mock(ActionDefinitionService.class);
    private final ComponentDefinitionService componentDefinitionService = mock(ComponentDefinitionService.class);
    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService =
        mock(TaskDispatcherDefinitionService.class);

    private final WorkflowValidatorFacadeImpl workflowValidatorFacade = new WorkflowValidatorFacadeImpl(
        mock(ActionDefinitionFacade.class), actionDefinitionService, mock(ClusterElementDefinitionService.class),
        componentDefinitionService, mock(ConnectionService.class), SpelEvaluator.create(),
        taskDispatcherDefinitionService,
        mock(TriggerDefinitionFacade.class), mock(TriggerDefinitionService.class),
        mock(WorkflowNodeTestOutputService.class), mock(WorkflowService.class),
        mock(WorkflowTestConfigurationService.class), List.of());

    @BeforeEach
    void beforeEach() {
        ActionDefinition placeholderActionDefinition = actionDefinition("missing");
        ActionDefinition searchEmailActionDefinition = actionDefinition("searchEmail");

        when(actionDefinitionService.getActionDefinition(anyString(), anyInt(), anyString()))
            .thenReturn(placeholderActionDefinition);
        when(actionDefinitionService.getActionDefinition("googleMail", 1, "searchEmail"))
            .thenReturn(searchEmailActionDefinition);
        when(actionDefinitionService.getActionDefinition("googleMail", 1, "unknownAction"))
            .thenThrow(new IllegalArgumentException("The component 'googleMail' does not contain the action."));
        when(componentDefinitionService.hasComponentDefinition(any(), any())).thenReturn(false);
        when(componentDefinitionService.hasComponentDefinition("googleMail", 1)).thenReturn(true);
        when(taskDispatcherDefinitionService.getTaskDispatcherDefinition("condition", 1))
            .thenReturn(new TaskDispatcherDefinition("condition"));
    }

    @Test
    void validateWorkflowAcceptsAnExistingAction() {
        WorkflowValidationResult result = workflowValidatorFacade.validateWorkflow(
            singleTaskWorkflow("googleMail/v1/searchEmail"));

        assertEquals(List.of(), result.errors());
    }

    @Test
    void validateWorkflowAcceptsAnExistingTaskDispatcher() {
        WorkflowValidationResult result = workflowValidatorFacade.validateWorkflow(singleTaskWorkflow("condition/v1"));

        assertEquals(List.of(), result.errors());

        verify(componentDefinitionService, never()).hasComponentDefinition(any(), any());
    }

    @Test
    void validateWorkflowReportsAnActionOfAnUnknownComponent() {
        WorkflowValidationResult result = workflowValidatorFacade.validateWorkflow(
            singleTaskWorkflow("nonexistent/v1/x"));

        assertEquals(
            List.of(
                "[task_1] Component 'nonexistent' (version 1) is not available"),
            result.errors());
    }

    @Test
    void validateWorkflowReportsAnActionOfAnUnknownComponentVersion() {
        WorkflowValidationResult result = workflowValidatorFacade.validateWorkflow(
            singleTaskWorkflow("googleMail/v9/searchEmail"));

        assertEquals(
            List.of(
                "[task_1] Component 'googleMail' (version 9) is not available"),
            result.errors());
    }

    @Test
    void validateWorkflowReportsAnUnknownActionOfAnExistingComponent() {
        WorkflowValidationResult result = workflowValidatorFacade.validateWorkflow(
            singleTaskWorkflow("googleMail/v1/unknownAction"));

        assertEquals(
            List.of(
                "[task_1] Action 'unknownAction' does not exist in component 'googleMail' (version 1)"),
            result.errors());
    }

    private static ActionDefinition actionDefinition(String name) {
        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        when(actionDefinition.getName()).thenReturn(name);

        return actionDefinition;
    }

    private static String singleTaskWorkflow(String taskType) {
        return """
            {
                "label": "Invoices",
                "description": "",
                "triggers": [],
                "tasks": [
                    {"label": "Task", "name": "task_1", "type": "{TASK_TYPE}", "parameters": {}}
                ]
            }
            """
            .replace("{TASK_TYPE}", taskType);
    }
}

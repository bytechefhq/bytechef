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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.exception.ConfigurationException;
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
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.NodeValidationIssue;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.WorkflowIssueKind;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.WorkflowIssueSeverity;
import com.bytechef.platform.workflow.validator.exception.WorkflowValidatorErrorType;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SpringBootTest(classes = WorkflowValidatorFacadeIntTest.WorkflowValidatorIntTestConfiguration.class)
class WorkflowValidatorFacadeIntTest {

    @MockitoBean
    private ActionDefinitionFacade actionDefinitionFacade;

    @MockitoBean
    private ActionDefinitionService actionDefinitionService;

    @MockitoBean
    private ClusterElementDefinitionService clusterElementDefinitionService;

    @MockitoBean
    private ComponentDefinitionService componentDefinitionService;

    @MockitoBean
    private ConnectionService connectionService;

    @MockitoBean
    private TaskDispatcherDefinitionService taskDispatcherDefinitionService;

    @MockitoBean
    private TriggerDefinitionFacade triggerDefinitionFacade;

    @MockitoBean
    private TriggerDefinitionService triggerDefinitionService;

    @Autowired
    private WorkflowValidatorFacade workflowValidatorFacade;

    @MockitoBean
    private WorkflowNodeTestOutputService workflowNodeTestOutputService;

    @MockitoBean
    private WorkflowService workflowService;

    @MockitoBean
    private WorkflowTestConfigurationService workflowTestConfigurationService;

    @Test
    void testFacadeIsRealImplementation() {
        assertThat(workflowValidatorFacade).isInstanceOf(WorkflowValidatorFacadeImpl.class);
    }

    @Test
    void testReservedInputNameRejected() {
        String definition = """
            {"label":"t","inputs":[{"name":"__triggerName","type":"string"}],"tasks":[]}
            """;

        assertThatThrownBy(() -> workflowValidatorFacade.validateNoReservedInputNames(definition))
            .isInstanceOf(ConfigurationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"label\":\"t\",\"inputs\":[{\"name\":\"message\",\"type\":\"string\"}],\"tasks\":[]}",
        "{\"label\":\"t\",\"tasks\":[]}",
        "not json",
        "{\"label\":\"t\",\"inputs\":[{\"name\":\"varsCount\",\"type\":\"string\"}],\"tasks\":[]}"
    })
    void testValidateNoReservedInputNamesAcceptsDefinitionsWithoutReservedNames(String definition) {
        assertThatCode(() -> workflowValidatorFacade.validateNoReservedInputNames(definition))
            .doesNotThrowAnyException();
    }

    @Test
    void testValidateNoReservedInputNamesThrowsWithErrorKeyOnReservedName() {
        String definition = """
            {"label":"t","inputs":[{"name":"__secret","type":"string"}],"tasks":[]}
            """;

        assertThatThrownBy(() -> workflowValidatorFacade.validateNoReservedInputNames(definition))
            .asInstanceOf(type(ConfigurationException.class))
            .extracting(ConfigurationException::getErrorKey)
            .isEqualTo(103)
            .isNotEqualTo(WorkflowValidatorErrorType.INVALID_INPUT_NAME.getErrorKey());
    }

    @Test
    void testErrorKeysAreUnique() throws IllegalAccessException {
        List<Integer> errorKeys = new ArrayList<>();

        for (Field field : WorkflowValidatorErrorType.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == WorkflowValidatorErrorType.class) {
                WorkflowValidatorErrorType workflowValidatorErrorType = (WorkflowValidatorErrorType) field.get(null);

                errorKeys.add(workflowValidatorErrorType.getErrorKey());
            }
        }

        assertThat(errorKeys).hasSizeGreaterThan(1)
            .doesNotHaveDuplicates();
    }

    @Test
    void testValidateNoReservedNodeNamesRejectsReservedTriggerName() {
        String definition = """
            {"label":"t","triggers":[{"name":"__triggerName","type":"manual/v1/manual"}],"tasks":[]}
            """;

        assertThatThrownBy(() -> workflowValidatorFacade.validateNoReservedNodeNames(definition))
            .asInstanceOf(type(ConfigurationException.class))
            .extracting(ConfigurationException::getErrorKey)
            .isEqualTo(WorkflowValidatorErrorType.RESERVED_NODE_NAME.getErrorKey());
    }

    @Test
    void testValidateNoReservedNodeNamesRejectsReservedTaskName() {
        String definition = """
            {"label":"t","triggers":[],"tasks":[{"name":"__internal","type":"logger/v1/info"}]}
            """;

        assertThatThrownBy(() -> workflowValidatorFacade.validateNoReservedNodeNames(definition))
            .isInstanceOf(ConfigurationException.class);
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            """
                {"label":"t","triggers":[],"tasks":[{"name":"condition_1","type":"condition/v1",
                 "parameters":{"caseTrue":[{"name":"__nested","type":"logger/v1/info"}],"caseFalse":[]}}]}
                """,
            """
                {"label":"t","triggers":[],"tasks":[{"name":"loop_1","type":"loop/v1",
                 "parameters":{"iteratee":[{"name":"vars","type":"logger/v1/info"}]}}]}
                """,
            """
                {"label":"t","triggers":[],"tasks":[{"name":"branch_1","type":"branch/v1",
                 "parameters":{"cases":[{"key":"a","tasks":[{"name":"__nested","type":"logger/v1/info"}]}],
                 "default":[]}}]}
                """,
            """
                {"label":"t","triggers":[],"tasks":[{"name":"parallel_1","type":"parallel/v1",
                 "parameters":{"tasks":[{"name":"__nested","type":"logger/v1/info"}]}}]}
                """,
            """
                {"label":"t","triggers":[],"tasks":[{"name":"forkJoin_1","type":"fork-join/v1",
                 "parameters":{"branches":[[{"name":"__nested","type":"logger/v1/info"}]]}}]}
                """,
            """
                {"label":"t","triggers":[],"tasks":[{"name":"loop_1","type":"loop/v1",
                 "parameters":{"iteratee":[{"name":"condition_1","type":"condition/v1",
                 "parameters":{"caseFalse":[{"name":"__deep","type":"logger/v1/info"}]}}]}}]}
                """
        })
    void testValidateNoReservedNodeNamesRejectsReservedNameNestedInTaskDispatcher(String definition) {
        assertThatThrownBy(() -> workflowValidatorFacade.validateNoReservedNodeNames(definition))
            .asInstanceOf(type(ConfigurationException.class))
            .extracting(ConfigurationException::getErrorKey)
            .isEqualTo(WorkflowValidatorErrorType.RESERVED_NODE_NAME.getErrorKey());
    }

    @Test
    void testValidateNoReservedNodeNamesAcceptsOrdinaryNamesNestedInTaskDispatcher() {
        String definition = """
            {"label":"t","triggers":[],"tasks":[{"name":"condition_1","type":"condition/v1",
             "parameters":{"caseTrue":[{"name":"task_1","type":"logger/v1/info"}],"caseFalse":[]}}]}
            """;

        assertThatCode(() -> workflowValidatorFacade.validateNoReservedNodeNames(definition))
            .doesNotThrowAnyException();
    }

    @Test
    void testValidateNoReservedNodeNamesAcceptsOrdinaryNames() {
        String definition = """
            {"label":"t","triggers":[{"name":"trigger_1","type":"manual/v1/manual"}],
             "tasks":[{"name":"task_1","type":"logger/v1/info"}]}
            """;

        assertThatCode(() -> workflowValidatorFacade.validateNoReservedNodeNames(definition))
            .doesNotThrowAnyException();
    }

    @Test
    void testValidateNoReservedNodeNamesFailsOpenOnMalformedJson() {
        String definition = "not json";

        assertThatCode(() -> workflowValidatorFacade.validateNoReservedNodeNames(definition))
            .doesNotThrowAnyException();
    }

    @Test
    void testValidateNoReservedInputNamesRejectsVars() {
        String definition = """
            {"label":"t","inputs":[{"name":"vars","type":"string"}],"tasks":[]}
            """;

        assertThatThrownBy(() -> workflowValidatorFacade.validateNoReservedInputNames(definition))
            .asInstanceOf(type(ConfigurationException.class))
            .extracting(ConfigurationException::getErrorKey)
            .isEqualTo(WorkflowValidatorErrorType.RESERVED_INPUT_NAME.getErrorKey());
    }

    @Test
    void testValidateNoReservedNodeNamesRejectsVars() {
        String definition = """
            {"label":"t","triggers":[],"tasks":[{"name":"vars","type":"var/v1/set"}]}
            """;

        assertThatThrownBy(() -> workflowValidatorFacade.validateNoReservedNodeNames(definition))
            .asInstanceOf(type(ConfigurationException.class))
            .extracting(ConfigurationException::getErrorKey)
            .isEqualTo(WorkflowValidatorErrorType.RESERVED_NODE_NAME.getErrorKey());
    }

    @Test
    void testValidateWorkflowReportsTaskWhoseComponentIsMissing() {
        when(actionDefinitionService.getActionDefinition("viber", 1, "sendTextMessage"))
            .thenReturn(mock(ActionDefinition.class));

        WorkflowValidatorFacade.WorkflowValidationResult workflowValidationResult =
            workflowValidatorFacade.validateWorkflow(
                """
                    {"tasks": [{"name": "viber_1", "type": "viber/v1/sendTextMessage", "parameters": {}}]}
                    """);

        assertThat(workflowValidationResult.nodeIssues()).contains(
            new NodeValidationIssue(
                "viber_1", null, WorkflowIssueKind.OTHER, WorkflowIssueSeverity.ERROR,
                "Component 'viber' (version 1) is not available"));
    }

    @Test
    void testValidateWorkflowReportsTaskWhoseActionIsMissing() {
        when(actionDefinitionService.getActionDefinition("infobip", 1, "sendViberBotTextMessage"))
            .thenThrow(
                new IllegalArgumentException(
                    "The component 'infobip' does not contain the 'sendViberBotTextMessage' action."));
        when(componentDefinitionService.hasComponentDefinition("infobip", 1)).thenReturn(true);

        WorkflowValidatorFacade.WorkflowValidationResult workflowValidationResult =
            workflowValidatorFacade.validateWorkflow(
                """
                    {
                        "tasks": [
                            {"name": "infobip_1", "type": "infobip/v1/sendViberBotTextMessage", "parameters": {}}
                        ]
                    }
                    """);

        assertThat(workflowValidationResult.nodeIssues()).contains(
            new NodeValidationIssue(
                "infobip_1", null, WorkflowIssueKind.OTHER, WorkflowIssueSeverity.ERROR,
                "Action 'sendViberBotTextMessage' does not exist in component 'infobip' (version 1)"));
    }

    @Test
    void testValidateWorkflowReportsTriggerWhoseComponentIsMissing() {
        when(triggerDefinitionService.getTriggerDefinition("viber", 1, "newMessage"))
            .thenThrow(
                new IllegalArgumentException("Component definition with name 'viber' and version '1' not found"));

        WorkflowValidatorFacade.WorkflowValidationResult workflowValidationResult =
            workflowValidatorFacade.validateWorkflow(
                """
                    {
                        "triggers": [{"name": "trigger_1", "type": "viber/v1/newMessage", "parameters": {}}],
                        "tasks": []
                    }
                    """);

        assertThat(workflowValidationResult.nodeIssues()).contains(
            new NodeValidationIssue(
                "trigger_1", null, WorkflowIssueKind.OTHER, WorkflowIssueSeverity.ERROR,
                "Component 'viber' (version 1) is not available"));
    }

    @Test
    void testValidateWorkflowReportsTriggerWhoseTriggerIsMissing() {
        when(triggerDefinitionService.getTriggerDefinition("infobip", 1, "newViberBotMessage"))
            .thenThrow(
                new IllegalArgumentException(
                    "The component 'infobip' does not contain the 'newViberBotMessage' trigger."));
        when(componentDefinitionService.hasComponentDefinition("infobip", 1)).thenReturn(true);

        WorkflowValidatorFacade.WorkflowValidationResult workflowValidationResult =
            workflowValidatorFacade.validateWorkflow(
                """
                    {
                        "triggers": [{"name": "trigger_2", "type": "infobip/v1/newViberBotMessage", "parameters": {}}],
                        "tasks": []
                    }
                    """);

        assertThat(workflowValidationResult.nodeIssues()).contains(
            new NodeValidationIssue(
                "trigger_2", null, WorkflowIssueKind.OTHER, WorkflowIssueSeverity.ERROR,
                "Trigger 'newViberBotMessage' does not exist in component 'infobip' (version 1)"));
    }

    @Test
    void testValidateWorkflowReportsTaskDispatcherThatIsMissing() {
        when(taskDispatcherDefinitionService.getTaskDispatcherDefinition("unknown", 1))
            .thenThrow(new IllegalArgumentException("Task dispatcher definition 'unknown' not found"));

        WorkflowValidatorFacade.WorkflowValidationResult workflowValidationResult =
            workflowValidatorFacade.validateWorkflow(
                """
                    {"tasks": [{"name": "unknown_1", "type": "unknown/v1", "parameters": {}}]}
                    """);

        assertThat(workflowValidationResult.nodeIssues()).contains(
            new NodeValidationIssue(
                "unknown_1", null, WorkflowIssueKind.OTHER, WorkflowIssueSeverity.ERROR,
                "Task dispatcher 'unknown' (version 1) is not available"));
    }

    @Test
    void testValidateWorkflowReportsNestedTaskWhoseComponentIsMissing() {
        when(taskDispatcherDefinitionService.getTaskDispatcherDefinition("loop", 1))
            .thenReturn(mock(TaskDispatcherDefinition.class));
        when(actionDefinitionService.getActionDefinition("viber", 1, "sendTextMessage"))
            .thenReturn(mock(ActionDefinition.class));

        WorkflowValidatorFacade.WorkflowValidationResult workflowValidationResult =
            workflowValidatorFacade.validateWorkflow(
                """
                    {
                        "tasks": [
                            {
                                "name": "loop_1",
                                "type": "loop/v1",
                                "parameters": {
                                    "items": [],
                                    "iteratee": [
                                        {"name": "viber_1", "type": "viber/v1/sendTextMessage", "parameters": {}}
                                    ]
                                }
                            }
                        ]
                    }
                    """);

        assertThat(workflowValidationResult.nodeIssues()).contains(
            new NodeValidationIssue(
                "viber_1", null, WorkflowIssueKind.OTHER, WorkflowIssueSeverity.ERROR,
                "Component 'viber' (version 1) is not available"));
        assertThat(workflowValidationResult.nodeIssues())
            .noneMatch(nodeValidationIssue -> "loop_1".equals(nodeValidationIssue.nodeName()));
    }

    @Test
    void testValidateWorkflowDoesNotReportNodesWhoseDefinitionsExist() {
        when(actionDefinitionService.getActionDefinition("var", 1, "set")).thenReturn(mock(ActionDefinition.class));
        when(componentDefinitionService.hasComponentDefinition("var", 1)).thenReturn(true);

        WorkflowValidatorFacade.WorkflowValidationResult workflowValidationResult =
            workflowValidatorFacade.validateWorkflow(
                """
                    {"tasks": [{"name": "var_1", "type": "var/v1/set", "parameters": {}}]}
                    """);

        assertThat(workflowValidationResult.nodeIssues())
            .noneMatch(nodeValidationIssue -> nodeValidationIssue.message()
                .contains("not available") ||
                nodeValidationIssue.message()
                    .contains("does not exist in component"));
    }

    @Configuration
    @ComponentScan("com.bytechef.platform.workflow.validator")
    static class WorkflowValidatorIntTestConfiguration {
    }
}

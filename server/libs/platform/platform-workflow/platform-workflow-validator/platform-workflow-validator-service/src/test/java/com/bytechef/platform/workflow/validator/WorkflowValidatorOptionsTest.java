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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableArrayProperty;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.Option;
import com.bytechef.platform.component.domain.OptionsDataSource;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.StringProperty;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade.WorkflowValidationResult;
import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WorkflowValidatorOptionsTest {

    private static final List<Object> DAY_OF_WEEK_VALUES = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L);

    private static final PropertyInfo DAY_OF_WEEK = new PropertyInfo(
        "dayOfWeek", "ARRAY", null, true, true, null, null,
        List.of(new PropertyInfo(null, "INTEGER", null, false, true, null, null)), null, DAY_OF_WEEK_VALUES);

    private static final PropertyInfo COUNT = new PropertyInfo(
        "count", "INTEGER", null, false, true, null, null, null, null, List.of(10L, 20L));

    private static final PropertyInfo FORMAT = new PropertyInfo(
        "format", "STRING", null, false, true, null, null, null, null, List.of("JSON", "XML"));

    private static final PropertyInfo DYNAMIC_SHEET = new PropertyInfo(
        "sheetName", "STRING", null, false, true, null, null, null, null, null);

    private final ActionDefinitionService actionDefinitionService = mock(ActionDefinitionService.class);
    private final TriggerDefinitionService triggerDefinitionService = mock(TriggerDefinitionService.class);

    private final WorkflowValidatorFacadeImpl workflowValidatorFacade = new WorkflowValidatorFacadeImpl(
        mock(ActionDefinitionFacade.class), actionDefinitionService, mock(ClusterElementDefinitionService.class),
        mock(ComponentDefinitionService.class), mock(ConnectionService.class),
        mock(TaskDispatcherDefinitionService.class), mock(TriggerDefinitionFacade.class), triggerDefinitionService,
        mock(WorkflowNodeTestOutputService.class), mock(WorkflowService.class),
        mock(WorkflowTestConfigurationService.class), List.of());

    @Test
    void testArrayValueOutsideTheOptionsIsAnError() {
        ValidationOutcome validationOutcome = validate("{\"dayOfWeek\": [0, 1, 2, 3, 4, 5, 6]}", DAY_OF_WEEK);

        assertThat(validationOutcome.errors()).contains("dayOfWeek[0]")
            .contains("0")
            .contains("1, 2, 3, 4, 5, 6, 7");
        assertThat(validationOutcome.errors()).doesNotContain("dayOfWeek[1]");
        assertThat(validationOutcome.warnings()).isEmpty();
    }

    @Test
    void testArrayValueInsideTheOptionsPasses() {
        ValidationOutcome validationOutcome = validate("{\"dayOfWeek\": [1, 2, 3, 4, 5, 6, 7]}", DAY_OF_WEEK);

        assertThat(validationOutcome.errors()).isEmpty();
        assertThat(validationOutcome.warnings()).isEmpty();
    }

    @Test
    void testDataPillAsTheOnlyArrayItemPasses() {
        ValidationOutcome validationOutcome = validate("{\"dayOfWeek\": [\"${trigger_1.days}\"]}", DAY_OF_WEEK);

        assertThat(validationOutcome.errors()).isEmpty();
        assertThat(validationOutcome.warnings()).isEmpty();
    }

    @Test
    void testDataPillAsTheWholeValuePasses() {
        ValidationOutcome validationOutcome = validate("{\"dayOfWeek\": \"${trigger_1.days}\"}", DAY_OF_WEEK);

        assertThat(validationOutcome.errors()).isEmpty();
        assertThat(validationOutcome.warnings()).isEmpty();
    }

    @Test
    void testDataPillNextToAValidItemPasses() {
        ValidationOutcome validationOutcome = validate("{\"dayOfWeek\": [1, \"${x.day}\"]}", DAY_OF_WEEK);

        assertThat(validationOutcome.errors()).isEmpty();
        assertThat(validationOutcome.warnings()).isEmpty();
    }

    @Test
    void testDataPillNextToAnInvalidItemReportsOnlyTheInvalidItem() {
        ValidationOutcome validationOutcome = validate("{\"dayOfWeek\": [0, \"${x.day}\"]}", DAY_OF_WEEK);

        assertThat(validationOutcome.errors()).contains("dayOfWeek[0]")
            .doesNotContain("dayOfWeek[1]")
            .doesNotContain("${x.day}");
        assertThat(validationOutcome.errors()
            .lines()
            .count()).isEqualTo(1);
    }

    @Test
    void testFormulaPasses() {
        assertThat(validate("{\"dayOfWeek\": \"=fromAi('dayOfWeek', 'ARRAY')\"}", DAY_OF_WEEK).errors()).isEmpty();
        assertThat(validate("{\"dayOfWeek\": [\"=fromAi('day', 'INTEGER')\", 2]}", DAY_OF_WEEK).errors()).isEmpty();
        assertThat(validate("{\"count\": \"=fromAi('count', 'INTEGER')\"}", COUNT).errors()).isEmpty();
    }

    @Test
    void testScalarValueOutsideTheOptionsIsAnError() {
        ValidationOutcome validationOutcome = validate("{\"count\": 15}", COUNT);

        assertThat(validationOutcome.errors()).contains("'count'")
            .contains("15");
        assertThat(validate("{\"count\": 20}", COUNT).errors()).isEmpty();
    }

    @Test
    void testStringValueOutsideTheOptionsIsOnlyAWarning() {
        ValidationOutcome validationOutcome = validate("{\"format\": \"CSV\"}", FORMAT);

        assertThat(validationOutcome.errors()).isEmpty();
        assertThat(validationOutcome.warnings()).contains("'format'")
            .contains("CSV");
        assertThat(validate("{\"format\": \"XML\"}", FORMAT).warnings()).isEmpty();
    }

    @Test
    void testDynamicOptionsAreNotEnforced() {
        ValidationOutcome validationOutcome = validate("{\"sheetName\": \"Unknown sheet\"}", DYNAMIC_SHEET);

        assertThat(validationOutcome.errors()).isEmpty();
        assertThat(validationOutcome.warnings()).isEmpty();
    }

    @Test
    void testScheduleDayOfWeekThroughValidateWorkflow() {
        TriggerDefinition triggerDefinition = mock(TriggerDefinition.class);

        doReturn(List.of(Property.toProperty(scheduleDayOfWeek()))).when(triggerDefinition)
            .getProperties();
        when(triggerDefinitionService.getTriggerDefinition("schedule", 1, "everyDay")).thenReturn(triggerDefinition);

        WorkflowValidationResult invalidResult = workflowValidatorFacade.validateWorkflow(
            workflow("triggers", "schedule/v1/everyDay", "{\"dayOfWeek\": [0, 1, 2, 3, 4, 5, 6]}"));

        assertThat(invalidResult.errors()).singleElement()
            .asString()
            .contains("dayOfWeek[0]");

        WorkflowValidationResult validResult = workflowValidatorFacade.validateWorkflow(
            workflow("triggers", "schedule/v1/everyDay", "{\"dayOfWeek\": [1, 2, 3, 4, 5, 6, 7]}"));

        assertThat(validResult.errors()).isEmpty();
        assertThat(validResult.warnings()).isEmpty();
    }

    @Test
    void testDynamicOptionsAreNotEnforcedThroughValidateWorkflow() {
        StringProperty sheetNameProperty = mock(StringProperty.class);

        when(sheetNameProperty.getName()).thenReturn("sheetName");
        when(sheetNameProperty.getType()).thenReturn(com.bytechef.component.definition.Property.Type.STRING);
        when(sheetNameProperty.getExpressionEnabled()).thenReturn(true);
        when(sheetNameProperty.getOptions()).thenReturn(List.of(new Option(ComponentDsl.option("Sheet1", "Sheet1"))));
        when(sheetNameProperty.getOptionsDataSource()).thenReturn(mock(OptionsDataSource.class));

        ActionDefinition actionDefinition = mock(ActionDefinition.class);

        doReturn(List.of(sheetNameProperty)).when(actionDefinition)
            .getProperties();
        when(actionDefinitionService.getActionDefinition("googleSheets", 1, "getRows")).thenReturn(actionDefinition);

        WorkflowValidationResult result = workflowValidatorFacade.validateWorkflow(
            workflow("tasks", "googleSheets/v1/getRows", "{\"sheetName\": \"Unknown sheet\"}"));

        assertThat(result.errors()).isEmpty();
        assertThat(result.warnings()).isEmpty();
    }

    private static ModifiableArrayProperty scheduleDayOfWeek() {
        return ComponentDsl.array("dayOfWeek")
            .items(ComponentDsl.integer())
            .options(
                List.of(
                    ComponentDsl.option("Sunday", 1), ComponentDsl.option("Monday", 2),
                    ComponentDsl.option("Tuesday", 3), ComponentDsl.option("Wednesday", 4),
                    ComponentDsl.option("Thursday", 5), ComponentDsl.option("Friday", 6),
                    ComponentDsl.option("Saturday", 7)))
            .required(true);
    }

    private static String workflow(String section, String type, String parameters) {
        String node = "[{\"label\": \"Node\", \"name\": \"node_1\", \"type\": \"" + type + "\", \"parameters\": " +
            parameters + "}]";

        return "{\"label\": \"Workflow\", \"description\": \"\", \"triggers\": " +
            ("triggers".equals(section) ? node : "[]") + ", \"tasks\": " + ("tasks".equals(section) ? node : "[]") +
            "}";
    }

    private static ValidationOutcome validate(String parameters, PropertyInfo propertyInfo) {
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        String task = "{\"label\": \"Task\", \"name\": \"task_1\", \"type\": \"component/v1/action\", " +
            "\"parameters\": " + parameters + "}";

        WorkflowValidator.validateSingleTask(task, (taskType, kind) -> List.of(propertyInfo), errors, warnings);

        return new ValidationOutcome(errors.toString(), warnings.toString());
    }

    private record ValidationOutcome(String errors, String warnings) {
    }
}

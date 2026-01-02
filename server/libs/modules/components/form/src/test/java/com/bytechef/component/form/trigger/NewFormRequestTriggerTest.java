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

package com.bytechef.component.form.trigger;

import static com.bytechef.component.form.constant.FormConstants.FIELD_NAME;
import static com.bytechef.component.form.constant.FormConstants.FIELD_TYPE;
import static com.bytechef.component.form.constant.FormConstants.IGNORE_BOTS;
import static com.bytechef.component.form.constant.FormConstants.INPUTS;
import static com.bytechef.component.form.constant.FormConstants.MULTIPLE_CHOICE;
import static com.bytechef.component.form.constant.FormConstants.USE_WORKFLOW_TIMEZONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.form.util.FieldType;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.definition.BaseProperty;
import com.bytechef.definition.BaseProperty.BaseObjectProperty;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class NewFormRequestTriggerTest {

    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final HttpHeaders mockedHttpHeaders = mock(HttpHeaders.class);
    private final HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final WebhookMethod mockedWebhookMethod = mock(WebhookMethod.class);
    private final Parameters mockedWebhookEnableOutput = mock(Parameters.class);

    @Test
    void testGetOutput() {
        Parameters inputParameters = MockParametersFactory.create(
            Map.of(
                INPUTS, List.of(
                    Map.of(FIELD_NAME, "field1", FIELD_TYPE, FieldType.CHECKBOX.getValue()),
                    Map.of(FIELD_NAME, "field2", FIELD_TYPE, FieldType.DATE_PICKER.getValue()),
                    Map.of(FIELD_NAME, "field3", FIELD_TYPE, FieldType.DATETIME_PICKER.getValue()),
                    Map.of(FIELD_NAME, "field4", FIELD_TYPE, FieldType.FILE_INPUT.getValue()),
                    Map.of(FIELD_NAME, "field5", FIELD_TYPE, FieldType.NUMBER_INPUT.getValue()),
                    Map.of(FIELD_NAME, "field6", FIELD_TYPE, FieldType.SELECT.getValue(), MULTIPLE_CHOICE, true),
                    Map.of(FIELD_NAME, "field7", FIELD_TYPE, FieldType.SELECT.getValue(), MULTIPLE_CHOICE, false),
                    Map.of(FIELD_NAME, "field8", FIELD_TYPE, FieldType.INPUT.getValue()),
                    Map.of(FIELD_TYPE, FieldType.INPUT.getValue()) // Missing field name
                )));

        OutputResponse outputResponse = NewFormRequestTrigger.getOutput(
            inputParameters, mock(Parameters.class), mockedTriggerContext);

        assertNotNull(outputResponse);

        BaseProperty.BaseValueProperty<?> outputSchema = outputResponse.getOutputSchema();
        assertInstanceOf(BaseObjectProperty.class, outputSchema);

        BaseObjectProperty<?> objectProperty = (BaseObjectProperty<?>) outputSchema;

        List<? extends BaseProperty> properties = objectProperty.getProperties()
            .orElseThrow();

        assertEquals(2, properties.size());

        BaseProperty property1 = properties.get(0);

        assertEquals("submittedAt", property1.getName());

        BaseProperty property2 = properties.get(1);

        assertEquals("body", property2.getName());

        BaseObjectProperty<?> bodyProperty = (BaseObjectProperty<?>) property2;

        List<? extends BaseProperty> bodyProperties = bodyProperty.getProperties()
            .orElseThrow();

        assertEquals(8, bodyProperties.size());

        BaseProperty bodyProperty1 = bodyProperties.getFirst();

        assertEquals("field1", bodyProperty1.getName());
        assertInstanceOf(BaseProperty.BaseBooleanProperty.class, bodyProperty1);

        BaseProperty bodyProperty2 = bodyProperties.get(1);

        assertEquals("field2", bodyProperty2.getName());
        assertInstanceOf(BaseProperty.BaseDateProperty.class, bodyProperty2);

        BaseProperty bodyProperty3 = bodyProperties.get(2);

        assertEquals("field3", bodyProperty3.getName());
        assertInstanceOf(BaseProperty.BaseDateTimeProperty.class, bodyProperty3);

        BaseProperty bodyProperty4 = bodyProperties.get(3);

        assertEquals("field4", bodyProperty4.getName());
        assertInstanceOf(BaseProperty.BaseFileEntryProperty.class, bodyProperty4);

        BaseProperty bodyProperty5 = bodyProperties.get(4);

        assertEquals("field5", bodyProperty5.getName());
        assertInstanceOf(BaseProperty.BaseNumberProperty.class, bodyProperty5);

        BaseProperty bodyProperty6 = bodyProperties.get(5);

        assertEquals("field6", bodyProperty6.getName());
        assertInstanceOf(BaseProperty.BaseArrayProperty.class, bodyProperty6);

        BaseProperty bodyProperty7 = bodyProperties.get(6);

        assertEquals("field7", bodyProperty7.getName());
        assertInstanceOf(BaseProperty.BaseStringProperty.class, bodyProperty7);

        BaseProperty bodyProperty8 = bodyProperties.get(7);

        assertEquals("field8", bodyProperty8.getName());
        assertInstanceOf(BaseProperty.BaseStringProperty.class, bodyProperty8);
    }

    @Test
    void testGetWebhookResult() {
        Map<String, Object> bodyContent = Map.of("key1", "value1");

        when(mockedWebhookBody.getContent()).thenReturn(bodyContent);

        Parameters inputParameters =
            MockParametersFactory.create(Map.of(IGNORE_BOTS, false, USE_WORKFLOW_TIMEZONE, false));

        Map<String, ?> result = NewFormRequestTrigger.getWebhookResult(
            inputParameters, mock(Parameters.class), mockedHttpHeaders, mockedHttpParameters,
            mockedWebhookBody, mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

        assertEquals("value1", result.get("key1"));
        assertTrue(result.containsKey("submittedAt"));
    }

    @Test
    void testGetWebhookResultIgnoreBots() {
        Map<String, Object> bodyContent = Map.of("key1", "value1");

        when(mockedWebhookBody.getContent()).thenReturn(bodyContent);

        Parameters inputParameters = MockParametersFactory.create(Map.of(IGNORE_BOTS, true));

        // Test with bot user agent
        when(mockedHttpHeaders.firstValue("User-Agent")).thenReturn(Optional.of("Googlebot"));

        Map<String, ?> result = NewFormRequestTrigger.getWebhookResult(
            inputParameters, mock(Parameters.class), mockedHttpHeaders, mockedHttpParameters,
            mockedWebhookBody, mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

        assertTrue(result.isEmpty());

        // Test with the regular user agent
        when(mockedHttpHeaders.firstValue("User-Agent")).thenReturn(Optional.of("Mozilla/5.0"));

        result = NewFormRequestTrigger.getWebhookResult(
            inputParameters, mock(Parameters.class), mockedHttpHeaders, mockedHttpParameters,
            mockedWebhookBody, mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

        assertEquals("value1", result.get("key1"));
    }

    @Test
    void testGetWebhookResultUseWorkflowTimezone() {
        Map<String, Object> bodyContent = Map.of("key1", "value1");

        when(mockedWebhookBody.getContent()).thenReturn(bodyContent);

        Parameters inputParameters = MockParametersFactory.create(Map.of(USE_WORKFLOW_TIMEZONE, true));

        Map<String, ?> result = NewFormRequestTrigger.getWebhookResult(
            inputParameters, mock(Parameters.class), mockedHttpHeaders, mockedHttpParameters,
            mockedWebhookBody, mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

        assertEquals("value1", result.get("key1"));
        assertFalse(result.containsKey("submittedAt"));
    }
}

/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.bamboohr.trigger;

import static com.bytechef.component.bamboohr.constant.BambooHrConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.bamboohr.util.BambooHrUtils;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TypeReference;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Marija Horvat
 */
class BambooHrUpdateEmployeeTriggerTest {

    protected TriggerDefinition.WebhookEnableOutput mockedWebhookEnableOutput =
        mock(TriggerDefinition.WebhookEnableOutput.class);
    protected TriggerDefinition.WebhookBody mockedWebhookBody = mock(TriggerDefinition.WebhookBody.class);
    protected TriggerDefinition.HttpHeaders mockedHttpHeaders = mock(TriggerDefinition.HttpHeaders.class);
    protected TriggerDefinition.HttpParameters mockedHttpParameters = mock(TriggerDefinition.HttpParameters.class);
    protected TriggerDefinition.WebhookMethod mockedWebhookMethod = mock(TriggerDefinition.WebhookMethod.class);
    protected Parameters mockedParameters = mock(Parameters.class);
    protected TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    protected MockedStatic<BambooHrUtils> bambooHrUtilsMockedStatic;
    protected String workflowExecutionId = "testWorkflowExecutionId";

    @BeforeEach
    public void beforeEach() {
        bambooHrUtilsMockedStatic = mockStatic(BambooHrUtils.class);
    }

    @AfterEach
    public void afterEach() {
        bambooHrUtilsMockedStatic.close();
    }

    @Test
    void testWebhookEnable() {
        String webhookUrl = "testWebhookUrl";

        when(mockedParameters.getRequiredString(ID))
            .thenReturn("id");

        bambooHrUtilsMockedStatic.when(
            () -> BambooHrUtils.addWebhook(webhookUrl, mockedTriggerContext))
            .thenReturn("123");

        TriggerDefinition.WebhookEnableOutput webhookEnableOutput = BambooHrUpdateEmployeeTrigger.webhookEnable(
            mockedParameters, mockedParameters, webhookUrl, workflowExecutionId, mockedTriggerContext);

        Map<String, ?> parameters = webhookEnableOutput.parameters();
        LocalDateTime webhookExpirationDate = webhookEnableOutput.webhookExpirationDate();

        Map<String, Object> expectedParameters = Map.of(ID, "123");

        assertEquals(expectedParameters, parameters);
        assertNull(webhookExpirationDate);
    }

    @Test
    void testWebhookRequest() {
        Map<String, Object> bodyContent = Map.of("employees", List.of(
            Map.of(
                "fields", Map.of(
                    "firstName", Map.of("value", "test"),
                    "lastName", Map.of("value", "test"),
                    "employeeNumber", Map.of("value", "1")))));

        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(bodyContent);

        Object result = BambooHrUpdateEmployeeTrigger.webhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

        List<Map<String, Object>> expectedOutput = List.of(
            Map.of("firstName", "test", "lastName", "test", "employeeNumber", "1"));

        assertEquals(expectedOutput, result);
    }
}

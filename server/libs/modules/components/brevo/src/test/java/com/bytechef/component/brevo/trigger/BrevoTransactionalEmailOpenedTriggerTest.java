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

package com.bytechef.component.brevo.trigger;

import static com.bytechef.component.brevo.constant.BrevoConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.brevo.util.BrevoUtils;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Marija Horvat
 */
class BrevoTransactionalEmailOpenedTriggerTest {

    protected TriggerDefinition.WebhookEnableOutput mockedWebhookEnableOutput =
        mock(TriggerDefinition.WebhookEnableOutput.class);
    protected TriggerDefinition.WebhookBody mockedWebhookBody = mock(TriggerDefinition.WebhookBody.class);
    protected TriggerDefinition.HttpHeaders mockedHttpHeaders = mock(TriggerDefinition.HttpHeaders.class);
    protected TriggerDefinition.HttpParameters mockedHttpParameters = mock(TriggerDefinition.HttpParameters.class);
    protected TriggerDefinition.WebhookMethod mockedWebhookMethod = mock(TriggerDefinition.WebhookMethod.class);
    protected Parameters mockedParameters = mock(Parameters.class);
    protected TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    protected MockedStatic<BrevoUtils> brevoUtilsMockedStatic;
    protected String workflowExecutionId = "testWorkflowExecutionId";
    private final Object mockedObject = mock(Object.class);

    @BeforeEach
    public void beforeEach() {
        brevoUtilsMockedStatic = mockStatic(BrevoUtils.class);
    }

    @AfterEach
    public void afterEach() {
        brevoUtilsMockedStatic.close();
    }

    @Test
    void testWebhookEnable() {
        String webhookUrl = "testWebhookUrl";

        when(mockedParameters.getRequiredString(ID))
            .thenReturn("id");

        brevoUtilsMockedStatic.when(
            () -> BrevoUtils.createWebhook(webhookUrl, mockedTriggerContext))
            .thenReturn("123");

        TriggerDefinition.WebhookEnableOutput webhookEnableOutput = BrevoTransactionalEmailOpenedTrigger.webhookEnable(
            mockedParameters, mockedParameters, webhookUrl, workflowExecutionId, mockedTriggerContext);

        Map<String, ?> parameters = webhookEnableOutput.parameters();
        LocalDateTime webhookExpirationDate = webhookEnableOutput.webhookExpirationDate();

        Map<String, Object> expectedParameters = Map.of(ID, "123");

        assertEquals(expectedParameters, parameters);
        assertNull(webhookExpirationDate);
    }

    @Test
    void testWebhookRequest() {

        when(mockedWebhookBody.getContent())
            .thenReturn(mockedObject);

        Object result = BrevoTransactionalEmailOpenedTrigger.webhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

        assertEquals(mockedObject, result);
    }
}

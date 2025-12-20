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

package com.bytechef.component.productboard.trigger;

import static com.bytechef.component.productboard.constant.ProductboardConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.productboard.util.ProductboardUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;

/**
 * @author Monika Kušter
 */
class ProductboardUpdatedFeatureTriggerTest {

    private final ArgumentCaptor<TriggerContext> contextArgumentCaptor = ArgumentCaptor.forClass(TriggerContext.class);
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(ID, "123"));
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final WebhookEnableOutput mockedWebhookEnableOutput = mock(WebhookEnableOutput.class);
    private final Parameters mockedWebhookEnableOutputParameters = mock(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<WebhookBody> webhookBodyArgumentCaptor = ArgumentCaptor.forClass(WebhookBody.class);

    @Test
    void testWebhookEnable() {
        String webhookUrl = "testWebhookUrl";
        String workflowExecutionId = "testWorkflowExecutionId";

        try (MockedStatic<ProductboardUtils> productboardUtilsMockedStatic = mockStatic(ProductboardUtils.class)) {
            productboardUtilsMockedStatic
                .when(() -> ProductboardUtils.createSubscription(
                    stringArgumentCaptor.capture(), stringArgumentCaptor.capture(), contextArgumentCaptor.capture(),
                    stringArgumentCaptor.capture()))
                .thenReturn(mockedWebhookEnableOutput);

            WebhookEnableOutput result = ProductboardUpdatedFeatureTrigger.webhookEnable(
                mockedParameters, mockedParameters, webhookUrl, workflowExecutionId, mockedTriggerContext);

            assertEquals(mockedWebhookEnableOutput, result);
            assertEquals(List.of(webhookUrl, workflowExecutionId, "feature.updated"),
                stringArgumentCaptor.getAllValues());
            assertEquals(mockedTriggerContext, contextArgumentCaptor.getValue());
        }
    }

    @Test
    void testWebhookDisable() {
        String workflowExecutionId = "testWorkflowExecutionId";

        try (MockedStatic<ProductboardUtils> productboardUtilsMockedStatic = mockStatic(ProductboardUtils.class)) {
            productboardUtilsMockedStatic
                .when(() -> ProductboardUtils.deleteSubscription(
                    contextArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenAnswer((Answer<Void>) invocation -> null);

            ProductboardUpdatedFeatureTrigger.webhookDisable(
                mockedParameters, mockedParameters, mockedParameters, workflowExecutionId, mockedTriggerContext);

            assertEquals("123", stringArgumentCaptor.getValue());
            assertEquals(mockedTriggerContext, contextArgumentCaptor.getValue());
        }
    }

    @Test
    void testWebhookRequest() {
        try (MockedStatic<ProductboardUtils> productboardUtilsMockedStatic = mockStatic(ProductboardUtils.class)) {
            productboardUtilsMockedStatic
                .when(() -> ProductboardUtils.getContent(webhookBodyArgumentCaptor.capture()))
                .thenReturn(mockedObject);

            Object result = ProductboardUpdatedFeatureTrigger.webhookRequest(
                mockedParameters, mockedParameters, mock(HttpHeaders.class), mock(HttpParameters.class),
                mockedWebhookBody, mock(WebhookMethod.class), mockedWebhookEnableOutputParameters,
                mockedTriggerContext);

            assertEquals(mockedObject, result);
            assertEquals(mockedWebhookBody, webhookBodyArgumentCaptor.getValue());
        }
    }
}

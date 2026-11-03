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

package com.bytechef.component.infobip.trigger;

import static com.bytechef.component.infobip.constant.InfobipConstants.CONFIGURATION_KEY;
import static com.bytechef.component.infobip.constant.InfobipConstants.SENDER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.infobip.util.InfobipUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

/**
 * @author Ivica Cardic
 */
class InfobipNewViberBusinessMessageTriggerTest extends AbstractInfobipTriggerTest {

    private final Parameters mockedViberParameters = MockParametersFactory.create(
        Map.of(SENDER, "441134960000", CONFIGURATION_KEY, "abc"));

    @Test
    void testWebhookEnable() {
        infobipUtilsMockedStatic
            .when(() -> InfobipUtils.getWebhookEnableOutput(
                stringArgumentCaptor.capture(), stringArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                any(), stringArgumentCaptor.capture(), triggerContextArgumentCaptor.capture()))
            .thenReturn(mockedWebhookEnableOutput);

        WebhookEnableOutput webhookEnableOutput = InfobipNewViberBusinessMessageTrigger.webhookEnable(
            mockedViberParameters, mockedViberParameters, "testWebhookUrl", "workflowExecutionId",
            mockedTriggerContext);

        assertEquals(mockedWebhookEnableOutput, webhookEnableOutput);
        assertEquals(
            List.of("resource", "441134960000", "VIBER_BM", "testWebhookUrl"),
            stringArgumentCaptor.getAllValues());
        assertEquals(mockedTriggerContext, triggerContextArgumentCaptor.getValue());
    }

    @Test
    void testWebhookDisable() {
        infobipUtilsMockedStatic
            .when(() -> InfobipUtils.unsubscribeWebhook(stringArgumentCaptor.capture(),
                triggerContextArgumentCaptor.capture()))
            .thenAnswer((Answer<Void>) invocation -> null);

        InfobipNewViberBusinessMessageTrigger.webhookDisable(
            mockedViberParameters, mockedViberParameters, mockedViberParameters, "workflowExecutionId",
            mockedTriggerContext);

        assertEquals(List.of("abc"), stringArgumentCaptor.getAllValues());
        assertEquals(mockedTriggerContext, triggerContextArgumentCaptor.getValue());
    }
}

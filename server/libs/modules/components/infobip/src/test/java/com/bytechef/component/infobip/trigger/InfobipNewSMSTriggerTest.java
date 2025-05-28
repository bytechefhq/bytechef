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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.infobip.util.InfobipUtils;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

/**
 * @author Monika Kušter
 */
class InfobipNewSMSTriggerTest extends AbstractInfobipTriggerTest {

    @Test
    void testWebhookEnable() {
        infobipUtilsMockedStatic
            .when(() -> InfobipUtils.getWebhookEnableOutput(
                stringArgumentCaptor.capture(), stringArgumentCaptor.capture(), any(),
                stringArgumentCaptor.capture(), triggerContextArgumentCaptor.capture()))
            .thenReturn(mockedWebhookEnableOutput);

        WebhookEnableOutput webhookEnableOutput = InfobipNewSMSTrigger.webhookEnable(
            mockedParameters, mockedParameters, "testWebhookUrl", "workflowExecutionId", mockedTriggerContext);

        assertEquals(mockedWebhookEnableOutput, webhookEnableOutput);

        assertEquals(List.of("123", "SMS", "testWebhookUrl"), stringArgumentCaptor.getAllValues());
        assertEquals(mockedTriggerContext, triggerContextArgumentCaptor.getValue());
    }

    @Test
    void testWebhookDisable() {
        infobipUtilsMockedStatic
            .when(() -> InfobipUtils.unsubscribeWebhook(
                stringArgumentCaptor.capture(), triggerContextArgumentCaptor.capture()))
            .thenAnswer((Answer<Void>) invocation -> null);

        InfobipNewSMSTrigger.webhookDisable(
            mockedParameters, mockedParameters, mockedParameters, "workflowExecutionId", mockedTriggerContext);

        assertEquals(List.of("abc"), stringArgumentCaptor.getAllValues());
        assertEquals(mockedTriggerContext, triggerContextArgumentCaptor.getValue());
    }
}

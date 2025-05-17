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

package com.bytechef.component.attio.trigger;

import static com.bytechef.component.attio.constant.AttioConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.attio.util.AttioUtils;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class AttioRecordCreatedTriggerTest extends AbstractAttioTriggerTest {

    @Test
    void webhookEnable() {
        String webhhookUrl = "testWebhookUrl";

        attioUtilsMockedStatic.when(
            () -> AttioUtils.subscribeWebhook(
                stringArgumentCaptor.capture(), triggerContextArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn("123");

        WebhookEnableOutput webhookEnableOutput = AttioRecordCreatedTrigger.webhookEnable(
            mockedParameters, mockedParameters, webhhookUrl, workflowExecutionId, mockedTriggerContext);

        WebhookEnableOutput expectedWebhookEnableOutput = new WebhookEnableOutput(Map.of(ID, "123"), null);

        assertEquals(expectedWebhookEnableOutput, webhookEnableOutput);
        assertEquals(List.of("record.created", webhhookUrl), stringArgumentCaptor.getAllValues());
        assertEquals(mockedTriggerContext, triggerContextArgumentCaptor.getValue());
    }

    @Test
    void webhookDisable() {
        mockedParameters = MockParametersFactory.create(Map.of(ID, "123"));

        AttioRecordCreatedTrigger.webhookDisable(
            mockedParameters, mockedParameters, mockedParameters, workflowExecutionId, mockedTriggerContext);

        attioUtilsMockedStatic.verify(() -> AttioUtils.unsubscribeWebhook(mockedTriggerContext, "123"));
    }

    @Test
    void webhookRequest() {
        Map<String, Object> content = Map.of("id", 123);

        attioUtilsMockedStatic.when(
            () -> AttioUtils.getContent(webhookBodyArgumentCaptor.capture()))
            .thenReturn(content);

        Object result = AttioTaskCreatedTrigger.webhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

        assertEquals(content, result);
        assertEquals(mockedWebhookBody, webhookBodyArgumentCaptor.getValue());
    }
}

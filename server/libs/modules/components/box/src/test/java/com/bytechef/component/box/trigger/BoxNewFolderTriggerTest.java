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

package com.bytechef.component.box.trigger;

import static com.bytechef.component.box.constant.BoxConstants.FOLDER;
import static com.bytechef.component.box.constant.BoxConstants.FOLDER_ID;
import static com.bytechef.component.box.constant.BoxConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.box.util.BoxUtils;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class BoxNewFolderTriggerTest extends AbstractBoxTriggerTest {

    @Test
    void testDynamicWebhookEnable() {
        mockedParameters = MockParametersFactory.create(Map.of(FOLDER_ID, "folderId"));

        String webhookUrl = "testWebhookUrl";

        boxUtilsMockedStatic.when(
            () -> BoxUtils.subscribeWebhook(
                stringArgumentCaptor.capture(), triggerContextArgumentCaptor.capture(),
                stringArgumentCaptor.capture(), stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn("123");

        WebhookEnableOutput webhookEnableOutput = BoxNewFolderTrigger.webhookEnable(
            mockedParameters, mockedParameters, webhookUrl, workflowExecutionId, mockedTriggerContext);

        WebhookEnableOutput expectedWebhookEnableOutput = new WebhookEnableOutput(Map.of(ID, "123"), null);

        assertEquals(expectedWebhookEnableOutput, webhookEnableOutput);
        assertEquals(List.of(webhookUrl, FOLDER, "FOLDER.CREATED", "folderId"), stringArgumentCaptor.getAllValues());
        assertEquals(mockedTriggerContext, triggerContextArgumentCaptor.getValue());
    }

    @Test
    void testWebhookRequest() {
        Map<String, ?> sourceMap = Map.of("source", mockedObject);

        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(sourceMap);

        Object result = BoxNewFolderTrigger.webhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedParameters, mockedTriggerContext);

        assertEquals(mockedObject, result);
    }
}

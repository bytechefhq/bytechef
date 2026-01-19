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

package com.bytechef.component.heygen.trigger;

import static com.bytechef.component.heygen.constant.HeyGenConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.heygen.util.HeyGenUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Marija Horvat
 */
class HeyGenVideoGenerationFailedTriggerTest extends AbstractHeyGenTriggerTest {

    @Test
    void webhookEnable() {
        String webhhookUrl = "testWebhookUrl";

        heyGenUtilsMockedStatic.when(
            () -> HeyGenUtils.registerWebhook(
                stringArgumentCaptor.capture(), triggerContextArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn("123");

        WebhookEnableOutput webhookEnableOutput = HeyGenVideoGenerationFailedTrigger.webhookEnable(
            null, null, webhhookUrl, "", mockedTriggerContext);

        WebhookEnableOutput expectedWebhookEnableOutput = new WebhookEnableOutput(Map.of(ID, "123"), null);

        assertEquals(expectedWebhookEnableOutput, webhookEnableOutput);
        assertEquals(List.of("avatar_video.fail", webhhookUrl), stringArgumentCaptor.getAllValues());
        assertEquals(mockedTriggerContext, triggerContextArgumentCaptor.getValue());
    }

    @Test
    void webhookDisable() {
        Parameters mockedParameters = MockParametersFactory.create(Map.of(ID, "123"));

        HeyGenVideoGenerationFailedTrigger.webhookDisable(
            null, null, mockedParameters, "", mockedTriggerContext);

        heyGenUtilsMockedStatic.verify(() -> HeyGenUtils.deleteWebhook(mockedTriggerContext, "123"));
    }

    @Test
    void webhookRequest() {
        Map<String, Object> content = Map.of("id", 123);

        heyGenUtilsMockedStatic.when(
            () -> HeyGenUtils.getWebhookEventData(webhookBodyArgumentCaptor.capture()))
            .thenReturn(content);

        Object result = HeyGenVideoGenerationFailedTrigger.webhookRequest(
            null, null, null, null, mockedWebhookBody,
            null, null, null);

        assertEquals(content, result);
        assertEquals(mockedWebhookBody, webhookBodyArgumentCaptor.getValue());
    }
}

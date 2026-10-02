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

package com.bytechef.platform.component.trigger;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WebhookRequestTest {

    @Test
    void testNewRequestIsNotValidated() {
        assertThat(createWebhookRequest().validated()).isFalse();
    }

    @Test
    void testAsValidatedKeepsRequestData() {
        WebhookRequest webhookRequest = createWebhookRequest().asValidated();

        assertThat(webhookRequest.validated()).isTrue();
        assertThat(webhookRequest.headers()).containsEntry("X-Csrf-Token", List.of("secret"));
        assertThat(webhookRequest.method()).isEqualTo(WebhookMethod.POST);
    }

    @Test
    void testValidatedFlagSurvivesJsonRoundTripThroughMetadata() {
        Map<String, ?> metadata = Map.of(
            WebhookRequest.WEBHOOK_REQUEST, JsonUtils.read(JsonUtils.write(createWebhookRequest().asValidated())));

        WebhookRequest webhookRequest = MapUtils.get(metadata, WebhookRequest.WEBHOOK_REQUEST, WebhookRequest.class);

        assertThat(webhookRequest.validated()).isTrue();
        assertThat(webhookRequest.headers()).containsEntry("X-Csrf-Token", List.of("secret"));
    }

    @Test
    void testRequestStoredWithoutValidatedFlagIsNotValidated() {
        Map<String, ?> metadata = Map.of(
            WebhookRequest.WEBHOOK_REQUEST,
            Map.of("headers", Map.of(), "parameters", Map.of(), "method", "POST"));

        WebhookRequest webhookRequest = MapUtils.get(metadata, WebhookRequest.WEBHOOK_REQUEST, WebhookRequest.class);

        assertThat(webhookRequest.validated()).isFalse();
    }

    private static WebhookRequest createWebhookRequest() {
        return new WebhookRequest(
            Map.of("X-Csrf-Token", List.of("secret")), Map.of("source", List.of("postman")), null,
            WebhookMethod.POST);
    }
}

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

package com.bytechef.platform.component.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.component.definition.ActionDefinition.WebhookResponse;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WebhookResponseSerializationTest {

    @Test
    void testRedirectWebhookResponseRoundTrip() {
        WebhookResponse webhookResponse = WebhookResponse.redirect("https://example.com");

        assertThat(roundTrip(webhookResponse)).isEqualTo(webhookResponse);
    }

    @Test
    void testRawWebhookResponseRoundTrip() {
        WebhookResponse webhookResponse = WebhookResponse.raw("<p>ok</p>", Map.of("Content-Type", "text/html"), 201);

        WebhookResponse deserializedWebhookResponse = roundTrip(webhookResponse);

        assertThat(deserializedWebhookResponse.getType()).isEqualTo(WebhookResponse.Type.RAW);
        assertThat(deserializedWebhookResponse.getBody()).isEqualTo("<p>ok</p>");
        assertThat(deserializedWebhookResponse.getHeaders()).containsEntry("Content-Type", "text/html");
        assertThat(deserializedWebhookResponse.getStatusCode()).isEqualTo(201);
    }

    private static WebhookResponse roundTrip(WebhookResponse webhookResponse) {
        return JsonUtils.read(JsonUtils.write(webhookResponse), WebhookResponse.class);
    }
}

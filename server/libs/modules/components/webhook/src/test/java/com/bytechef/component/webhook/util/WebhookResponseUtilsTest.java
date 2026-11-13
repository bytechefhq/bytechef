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

package com.bytechef.component.webhook.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionDefinition.WebhookResponse;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WebhookResponseUtilsTest {

    private final TriggerContext triggerContext = mock(TriggerContext.class);

    @Test
    void testDefaultReturnsNull() {
        assertThat(getWebhookResponse(Map.of("responseType", "DEFAULT"))).isNull();
    }

    @Test
    void testMissingResponseTypeReturnsNull() {
        assertThat(getWebhookResponse(Map.of())).isNull();
    }

    @Test
    void testRedirectUsesRedirectUrl() {
        assertThat(getWebhookResponse(Map.of("responseType", "REDIRECT", "redirectUrl", "https://example.com")))
            .isEqualTo(WebhookResponse.redirect("https://example.com"));
    }

    @Test
    void testJsonUsesBodyHeadersAndStatusCode() {
        WebhookResponse webhookResponse = getWebhookResponse(
            Map.of(
                "responseType", "JSON", "body", Map.of("ok", true), "headers", Map.of("X-A", "1"),
                "statusCode", 201));

        assertThat(webhookResponse).isEqualTo(WebhookResponse.json(Map.of("ok", true), Map.of("X-A", "1"), 201));
    }

    @Test
    void testRawDefaultsStatusCodeTo200() {
        assertThat(getWebhookResponse(Map.of("responseType", "RAW", "body", "Thanks")))
            .isEqualTo(WebhookResponse.raw("Thanks", Map.of(), 200));
    }

    @Test
    void testNoDataUsesStatusCode() {
        assertThat(getWebhookResponse(Map.of("responseType", "NO_DATA", "statusCode", 204)))
            .isEqualTo(WebhookResponse.noData(Map.of(), 204));
    }

    private WebhookResponse getWebhookResponse(Map<String, Object> inputParameters) {
        return WebhookResponseUtils.getWebhookResponse(MockParametersFactory.create(inputParameters), triggerContext);
    }
}

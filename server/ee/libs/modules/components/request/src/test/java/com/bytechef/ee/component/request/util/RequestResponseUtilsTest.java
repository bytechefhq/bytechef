/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.request.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionDefinition.WebhookResponse;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 * @version ee
 */
class RequestResponseUtilsTest {

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
        return RequestResponseUtils.getWebhookResponse(MockParametersFactory.create(inputParameters), triggerContext);
    }
}

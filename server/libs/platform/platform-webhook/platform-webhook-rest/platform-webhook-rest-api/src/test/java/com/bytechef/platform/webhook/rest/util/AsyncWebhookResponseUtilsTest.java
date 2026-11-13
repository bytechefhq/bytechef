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

package com.bytechef.platform.webhook.rest.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionDefinition.WebhookResponse;
import com.bytechef.component.definition.FileEntry;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class AsyncWebhookResponseUtilsTest {

    @Test
    void testAllowsJsonRawNoDataAndValidRedirect() {
        assertThat(AsyncWebhookResponseUtils.isAllowed(WebhookResponse.json(Map.of("a", 1)))).isTrue();
        assertThat(
            AsyncWebhookResponseUtils.isAllowed(
                WebhookResponse.raw("x", Map.of("Content-Type", "text/html"), 200)))
                    .isTrue();
        assertThat(AsyncWebhookResponseUtils.isAllowed(WebhookResponse.noData(Map.of(), 204))).isTrue();
        assertThat(AsyncWebhookResponseUtils.isAllowed(WebhookResponse.redirect("https://example.com"))).isTrue();
    }

    @Test
    void testRejectsRedirectStatusOnNonRedirectResponse() {
        assertThat(AsyncWebhookResponseUtils.isAllowed(WebhookResponse.json(Map.of("a", 1), 301))).isFalse();
    }

    @Test
    void testRejectsLocationHeaderOnNonRedirectResponse() {
        assertThat(
            AsyncWebhookResponseUtils.isAllowed(
                WebhookResponse.noData(Map.of("Location", "https://evil.com"), 200)))
                    .isFalse();
    }

    @Test
    void testRejectsSetCookieHeaderCaseInsensitively() {
        assertThat(AsyncWebhookResponseUtils.isAllowed(WebhookResponse.raw("x", Map.of("set-cookie", "a=b"), 200)))
            .isFalse();
    }

    @Test
    void testRejectsUnresolvedRedirect() {
        assertThat(AsyncWebhookResponseUtils.isAllowed(WebhookResponse.redirect("${redirectUrl}"))).isFalse();
    }

    @Test
    void testRejectsBinaryResponse() {
        assertThat(AsyncWebhookResponseUtils.isAllowed(WebhookResponse.binary(mock(FileEntry.class)))).isFalse();
    }

    @Test
    void testRejectsStatusCodeOutsideHttpRange() {
        assertThat(AsyncWebhookResponseUtils.isAllowed(WebhookResponse.noData(Map.of(), 0))).isFalse();
        assertThat(AsyncWebhookResponseUtils.isAllowed(WebhookResponse.noData(Map.of(), 99))).isFalse();
        assertThat(AsyncWebhookResponseUtils.isAllowed(WebhookResponse.noData(Map.of(), 600))).isFalse();
        assertThat(AsyncWebhookResponseUtils.isAllowed(WebhookResponse.json(Map.of(), 1000))).isFalse();
    }

    @Test
    void testRejectsUnparseableContentType() {
        assertThat(AsyncWebhookResponseUtils.isAllowed(WebhookResponse.raw("x", Map.of("Content-Type", "html"), 200)))
            .isFalse();
    }

    @Test
    void testRejectsRefreshHeader() {
        assertThat(
            AsyncWebhookResponseUtils.isAllowed(
                WebhookResponse.raw("x", Map.of("Refresh", "0; url=https://evil.com"), 200)))
                    .isFalse();
    }
}

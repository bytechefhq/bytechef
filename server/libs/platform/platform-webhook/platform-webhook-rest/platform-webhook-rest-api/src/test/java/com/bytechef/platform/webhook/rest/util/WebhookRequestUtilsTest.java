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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WebhookRequestUtilsTest {

    private final TempFileStorage tempFileStorage = mock(TempFileStorage.class);

    @Test
    void testJsonBodyIsParsed() throws Exception {
        WebhookRequest webhookRequest = getWebhookRequest("application/json", "{\"orderId\": 1001}");

        assertThat(webhookRequest.body()
            .getContent()).isEqualTo(Map.of("orderId", 1001));
    }

    @Test
    void testEmptyJsonBodyIsTreatedAsNoBody() throws Exception {
        WebhookRequest webhookRequest = getWebhookRequest("application/json", "");

        assertThat(webhookRequest.body()).isNull();
    }

    @Test
    void testBlankJsonBodyIsTreatedAsNoBody() throws Exception {
        WebhookRequest webhookRequest = getWebhookRequest("application/json; charset=utf-8", "  \n ");

        assertThat(webhookRequest.body()).isNull();
    }

    @Test
    void testMalformedJsonBodyIsRejectedAsInvalidArgument() {
        assertThatThrownBy(() -> getWebhookRequest("application/json", "{\"orderId\": "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith("Invalid JSON request body");
    }

    @Test
    void testEmptyXmlBodyIsTreatedAsNoBody() throws Exception {
        WebhookRequest webhookRequest = getWebhookRequest("application/xml", "");

        assertThat(webhookRequest.body()).isNull();
    }

    @Test
    void testMalformedXmlBodyIsRejectedAsInvalidArgument() {
        assertThatThrownBy(() -> getWebhookRequest("application/xml", "<order><id>1001</order>"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith("Invalid XML request body");
    }

    private WebhookRequest getWebhookRequest(String contentType, String content) throws Exception {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest("POST", "/webhooks/id");

        mockHttpServletRequest.setContentType(contentType);
        mockHttpServletRequest.setContent(content.getBytes(StandardCharsets.UTF_8));

        return WebhookRequestUtils.getWebhookRequest(mockHttpServletRequest, tempFileStorage);
    }
}

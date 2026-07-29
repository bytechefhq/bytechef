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

package com.bytechef.platform.notification.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bytechef.test.extension.ObjectMapperSetupExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SlackNotificationClientTest {

    @Test
    void testSendWrapsTextInSlackPayloadAndDelegates() {
        WebhookNotificationClient webhookNotificationClient = mock(WebhookNotificationClient.class);

        SlackNotificationClient slackNotificationClient = new SlackNotificationClient(webhookNotificationClient);

        slackNotificationClient.send("https://hooks.slack.com/services/T000/B000/XXX", ":rotating_light: *Alert*");

        ArgumentCaptor<WebhookDeliveryRequest> requestCaptor = ArgumentCaptor.forClass(WebhookDeliveryRequest.class);

        verify(webhookNotificationClient).deliver(requestCaptor.capture());

        WebhookDeliveryRequest request = requestCaptor.getValue();

        assertThat(request.url()).isEqualTo("https://hooks.slack.com/services/T000/B000/XXX");
        assertThat(request.eventType()).isEqualTo("slack.message");
        assertThat(request.payloadJson()).contains("\":rotating_light: *Alert*\"");
        assertThat(request.secret()).isNull();
    }
}

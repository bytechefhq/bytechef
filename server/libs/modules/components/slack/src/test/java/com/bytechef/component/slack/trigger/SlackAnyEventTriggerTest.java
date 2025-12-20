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

package com.bytechef.component.slack.trigger;

import static com.bytechef.component.slack.constant.SlackConstants.CHALLENGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.component.definition.TypeReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class SlackAnyEventTriggerTest {

    private final HttpHeaders mockedHttpHeaders = mock(HttpHeaders.class);
    private final HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final Parameters mockedWebhookEnableOutput = mock(Parameters.class);
    private final WebhookMethod mockedWebhookMethod = mock(WebhookMethod.class);

    @Test
    void testWebhookRequest() {
        Map<String, Object> responseMap = new HashMap<>();

        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(Map.of("event", responseMap));

        Object result = SlackAnyEventTrigger.webhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

        assertEquals(responseMap, result);
    }

    @Test
    void testWebhookValidateOnEnable() {
        when(mockedWebhookBody.getContent(any(TypeReference.class)))
            .thenReturn(Map.of(CHALLENGE, "123456789"));

        WebhookValidateResponse webhookValidateResponse = SlackAnyEventTrigger.webhookValidateOnEnable(
            mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody, mockedWebhookMethod,
            mockedTriggerContext);

        assertEquals(200, webhookValidateResponse.status());
        assertEquals(Map.of("Content-type", List.of("text/plain")), webhookValidateResponse.headers());
        assertEquals("123456789", webhookValidateResponse.body());
    }
}

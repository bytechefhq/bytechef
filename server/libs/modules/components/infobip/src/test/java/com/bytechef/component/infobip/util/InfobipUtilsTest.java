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

package com.bytechef.component.infobip.util;

import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.infobip.constant.InfobipConstants.CONFIGURATION_KEY;
import static com.bytechef.component.infobip.constant.InfobipConstants.FROM;
import static com.bytechef.component.infobip.constant.InfobipConstants.NUMBER;
import static com.bytechef.component.infobip.constant.InfobipConstants.TEMPLATE_NAME;
import static com.bytechef.component.infobip.constant.InfobipConstants.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class InfobipUtilsTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Parameters mockedParameters =
        MockParametersFactory.create(Map.of(FROM, "1234567890", TEMPLATE_NAME, "template"));

    @Test
    void testCreatePlaceholderProperties() {
        List<Map<String, Object>> templates = List.of(
            Map.of(
                "name", "template",
                "structure", Map.of("body", Map.of(TEXT, "Hello {{1}}! You have {{2}} new messages."))));

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("templates", templates));

        List<? extends ValueProperty<?>> properties = InfobipUtils.createPlaceholderProperties(
            mockedParameters, mockedParameters, Map.of(), mockedActionContext);

        List<ValueProperty<?>> expectedProperties = List.of(
            string("_1")
                .label("1")
                .required(true),
            string("_2")
                .label("2")
                .required(true));

        assertEquals(expectedProperties, properties);
    }

    @Test
    void testGetTemplates() {
        List<Map<String, String>> templates = List.of(Map.of("name", "template", "language", "en"));

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("templates", templates));

        assertEquals(templates, InfobipUtils.getTemplates(anyString(), mockedActionContext));
    }

    @Test
    void testGetWebhookEnableOutput() {
        Map<String, Object> responseMap = Map.of(CONFIGURATION_KEY, "abc");

        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        WebhookEnableOutput webhookEnableOutput =
            InfobipUtils.getWebhookEnableOutput("number", "SMS", null, "webhookUrl", mockedTriggerContext);

        assertEquals(new WebhookEnableOutput(Map.of(CONFIGURATION_KEY, "abc"), null), webhookEnableOutput);

        Http.Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> expectedBody = Map.of(
            "channel", "SMS",
            NUMBER, "number",
            "forwarding", Map.of("type", "HTTP_FORWARD", "url", "webhookUrl"));

        assertEquals(expectedBody, body.getContent());
    }
}

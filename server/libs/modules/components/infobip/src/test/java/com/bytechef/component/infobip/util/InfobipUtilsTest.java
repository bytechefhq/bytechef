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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ValueProperty;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class InfobipUtilsTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(FROM, "1234567890", TEMPLATE_NAME, "template"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testCreatePlaceholderProperties(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        List<Map<String, Object>> templates = List.of(
            Map.of("name", "template", "structure",
                Map.of("body", Map.of(TEXT, "Hello {{1}}! You have {{2}} new messages."))));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("templates", templates));

        List<? extends ValueProperty<?>> properties = InfobipUtils.createPlaceholderProperties(
            mockedParameters, mockedParameters, Map.of(), mockedContext);

        List<ValueProperty<?>> expectedProperties = List.of(
            string("_1")
                .label("1")
                .required(true),
            string("_2")
                .label("2")
                .required(true));

        assertEquals(expectedProperties, properties);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/whatsapp/2/senders/1234567890/templates", stringArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
    }

    @Test
    void testGetTemplates(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        List<Map<String, String>> templates = List.of(Map.of("name", "template", "language", "en"));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("templates", templates));

        assertEquals(templates, InfobipUtils.getTemplates(anyString(), mockedContext));
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/whatsapp/2/senders//templates", stringArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
    }

    @Test
    void testGetWebhookEnableOutput(
        TriggerContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(CONFIGURATION_KEY, "abc"));

        WebhookEnableOutput webhookEnableOutput = InfobipUtils.getWebhookEnableOutput(
            "number", "SMS", null, "webhookUrl", mockedContext);

        assertEquals(new WebhookEnableOutput(Map.of(CONFIGURATION_KEY, "abc"), null), webhookEnableOutput);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/resource-management/1/inbound-message-configurations", stringArgumentCaptor.getValue());
        assertEquals(
            Body.of("channel", "SMS", NUMBER, "number", "forwarding",
                Map.of("type", "HTTP_FORWARD", "url", "webhookUrl")),
            bodyArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
    }
}

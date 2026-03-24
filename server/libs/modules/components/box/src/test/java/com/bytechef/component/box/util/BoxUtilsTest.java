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

package com.bytechef.component.box.util;

import static com.bytechef.component.box.constant.BoxConstants.ID;
import static com.bytechef.component.box.constant.BoxConstants.NAME;
import static com.bytechef.component.box.constant.BoxConstants.TYPE;
import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
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
class BoxUtilsTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private Parameters mockedParameters = mock(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testGetFileIdOptions(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        mockedParameters = MockParametersFactory.create(Map.of(ID, "xy"));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("entries", List.of(Map.of(NAME, "file name", ID, "fileId", TYPE, "file"))));

        assertEquals(
            List.of(option("file name", "fileId")),
            BoxUtils.getFileIdOptions(mockedParameters, mockedParameters, Map.of(), "", mockedContext));

        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/folders/xy/items", stringArgumentCaptor.getValue());
    }

    @Test
    void testSubscribeWebhok(
        TriggerContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(ID, "123"));

        assertEquals("123",
            BoxUtils.subscribeWebhook("webhookUrl", mockedContext, "type", "triggerEvent", "id"));

        Map<String, Object> expectedBody = Map.of(
            "address", "webhookUrl",
            "triggers", List.of("triggerEvent"),
            "target", Map.of(ID, "id", TYPE, "type"));

        assertEquals(Body.of(expectedBody, BodyContentType.JSON), bodyArgumentCaptor.getValue());
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/webhooks", stringArgumentCaptor.getValue());
    }

    @Test
    void testUnsubscribeWebhook(
        TriggerContext mockedContext, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor) {

        mockedParameters = MockParametersFactory.create(Map.of(ID, "123"));

        when(mockedHttp.delete(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        BoxUtils.unsubscribeWebhook(mockedParameters, mockedContext);

        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/webhooks/123", stringArgumentCaptor.getValue());
    }
}

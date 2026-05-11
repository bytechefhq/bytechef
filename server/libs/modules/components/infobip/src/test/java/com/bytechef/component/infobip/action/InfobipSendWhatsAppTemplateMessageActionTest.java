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

package com.bytechef.component.infobip.action;

import static com.bytechef.component.infobip.constant.InfobipConstants.CONTENT;
import static com.bytechef.component.infobip.constant.InfobipConstants.FROM;
import static com.bytechef.component.infobip.constant.InfobipConstants.LANGUAGE;
import static com.bytechef.component.infobip.constant.InfobipConstants.NAME;
import static com.bytechef.component.infobip.constant.InfobipConstants.PLACEHOLDERS;
import static com.bytechef.component.infobip.constant.InfobipConstants.TEMPLATE_NAME;
import static com.bytechef.component.infobip.constant.InfobipConstants.TO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.infobip.util.InfobipUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class InfobipSendWhatsAppTemplateMessageActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(FROM, "123", TO, "456", TEMPLATE_NAME, "template", PLACEHOLDERS,
            Map.of("_1", "value1", "_2", "value2", "_3", "value3")));
    private final Map<String, Object> responseMap = Map.of("result", List.of("123", "abc"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        try (MockedStatic<InfobipUtils> infobipUtilsMockedStatic = mockStatic(InfobipUtils.class)) {
            infobipUtilsMockedStatic
                .when(() -> InfobipUtils.getTemplates(stringArgumentCaptor.capture(), contextArgumentCaptor.capture()))
                .thenReturn(List.of(Map.of(NAME, "template", LANGUAGE, "en")));

            when(mockedHttp.post(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(responseMap);

            Map<String, Object> result = InfobipSendWhatsAppTemplateMessageAction.perform(
                mockedParameters, mockedParameters, mockedContext);

            assertEquals(responseMap, result);

            ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

            assertNotNull(capturedFunction);

            Http.Configuration.ConfigurationBuilder configurationBuilder =
                configurationBuilderArgumentCaptor.getValue();
            Http.Configuration configuration = configurationBuilder.build();
            Http.ResponseType responseType = configuration.getResponseType();

            Body body = bodyArgumentCaptor.getValue();

            Map<String, Object> expectedBody = Map.of(
                "messages", List.of(
                    Map.of(
                        FROM, "123",
                        TO, "456",
                        CONTENT, Map.of(
                            TEMPLATE_NAME, "template",
                            "templateData", Map.of("body", Map.of(PLACEHOLDERS, List.of("value1", "value2", "value3"))),
                            LANGUAGE, "en"))));

            assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
            assertEquals(expectedBody, body.getContent());
            assertEquals(List.of("123", "/whatsapp/1/message/template"), stringArgumentCaptor.getAllValues());
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
        }
    }
}

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

package com.bytechef.component.resend.action;

import static com.bytechef.component.resend.constant.ResendConstants.ATTACHMENTS;
import static com.bytechef.component.resend.constant.ResendConstants.BCC;
import static com.bytechef.component.resend.constant.ResendConstants.CC;
import static com.bytechef.component.resend.constant.ResendConstants.FROM;
import static com.bytechef.component.resend.constant.ResendConstants.HEADERS;
import static com.bytechef.component.resend.constant.ResendConstants.HTML;
import static com.bytechef.component.resend.constant.ResendConstants.REPLY_TO;
import static com.bytechef.component.resend.constant.ResendConstants.SUBJECT;
import static com.bytechef.component.resend.constant.ResendConstants.TAGS;
import static com.bytechef.component.resend.constant.ResendConstants.TEXT;
import static com.bytechef.component.resend.constant.ResendConstants.TO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
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
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.resend.util.ResendUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.HashMap;
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
class ResendSendEmailActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final Object mockedObject = mock(Object.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Parameters mockedParameters = MockParametersFactory.create(createParametersMap());
    private final List<Map<String, String>> attachments = List.of(Map.of("filename", "fileName"));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<ActionContext> actionContextArgumentCaptor = forClass(ActionContext.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<List<FileEntry>> listArgumentCaptor = forClass(List.class);

    @Test
    void testPerform(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody())
            .thenReturn(mockedObject);

        try (MockedStatic<ResendUtils> resendUtilsMockedStatic = mockStatic(ResendUtils.class)) {
            resendUtilsMockedStatic
                .when(() -> ResendUtils.getAttachments(
                    listArgumentCaptor.capture(), actionContextArgumentCaptor.capture()))
                .thenReturn(attachments);

            Object result = ResendSendEmailAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(mockedObject, result);
            assertEquals(List.of(mockedFileEntry), listArgumentCaptor.getValue());
            assertEquals(mockedContext, actionContextArgumentCaptor.getValue());
            assertNotNull(httpFunctionArgumentCaptor.getValue());
            assertEquals("/emails", stringArgumentCaptor.getValue());

            Map<String, Object> bodyMap = createParametersMap();
            bodyMap.put("attachments", attachments);

            assertEquals(Body.of(bodyMap, BodyContentType.JSON), bodyArgumentCaptor.getValue());

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();

            assertEquals(ResponseType.JSON, configuration.getResponseType());
        }
    }

    private Map<String, Object> createParametersMap() {
        Map<String, Object> map = new HashMap<>();

        map.put(FROM, "from@mail.com");
        map.put(TO, List.of("to@mail.com"));
        map.put(SUBJECT, "subject");
        map.put(BCC, List.of("bcc@mail.com"));
        map.put(CC, List.of("cc@mail.com"));
        map.put(REPLY_TO, List.of("reply@mail.com"));
        map.put(HTML, "html");
        map.put(TEXT, "text");
        map.put(HEADERS, Map.of("X-Entity-Ref-ID", "123456789"));
        map.put(ATTACHMENTS, List.of(mockedFileEntry));
        map.put(TAGS, List.of(Map.of("name", "category", "value", "confirm_email")));

        return map;
    }
}

/*
 * Copyright 2023-present ByteChef Inc.
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.resend.util.ResendUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
class ResendSendEmailActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final List<Map<String, String>> attachments = List.of(Map.of("filename", "fileName"));

    @Test
    void testPerform() {
        Map<String, Object> propertyStubsMap = createPropertyStubsMap();

        when(mockedParameters.getRequiredString(FROM))
            .thenReturn((String) propertyStubsMap.get(FROM));
        when(mockedParameters.getRequiredList(TO, String.class))
            .thenReturn((List<String>) propertyStubsMap.get(TO));
        when(mockedParameters.getRequiredString(SUBJECT))
            .thenReturn((String) propertyStubsMap.get(SUBJECT));
        when(mockedParameters.getList(BCC, String.class))
            .thenReturn((List<String>) propertyStubsMap.get(BCC));
        when(mockedParameters.getList(CC, String.class))
            .thenReturn((List<String>) propertyStubsMap.get(CC));
        when(mockedParameters.getList(REPLY_TO, String.class))
            .thenReturn((List<String>) propertyStubsMap.get(REPLY_TO));
        when(mockedParameters.getString(HTML))
            .thenReturn((String) propertyStubsMap.get(HTML));
        when(mockedParameters.getString(TEXT))
            .thenReturn((String) propertyStubsMap.get(TEXT));
        when(mockedParameters.getMap(HEADERS, String.class))
            .thenReturn((Map<String, String>) propertyStubsMap.get(HEADERS));
        when(mockedParameters.getFileEntries(ATTACHMENTS, List.of()))
            .thenReturn(List.of());
        when((List<Map<String, String>>) mockedParameters.getList(TAGS))
            .thenReturn((List<Map<String, String>>) propertyStubsMap.get(TAGS));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedObject);

        try (MockedStatic<ResendUtils> resendUtilsMockedStatic = mockStatic(ResendUtils.class)) {
            resendUtilsMockedStatic
                .when(() -> ResendUtils.getAttachments(List.of(), mockedContext))
                .thenReturn(attachments);

            Object result = ResendSendEmailAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(mockedObject, result);

            Http.Body bodyValue = bodyArgumentCaptor.getValue();

            assertEquals(propertyStubsMap, bodyValue.getContent());
        }
    }

    private Map<String, Object> createPropertyStubsMap() {
        Map<String, Object> propertyStubsMap = new HashMap<>();

        List<Map<String, String>> tags = List.of(Map.of(
            "name", "category",
            "value", "confirm_email"));

        Map<String, String> headerMap = Map.of("X-Entity-Ref-ID", "123456789");

        propertyStubsMap.put(FROM, "from@mail.com");
        propertyStubsMap.put(TO, List.of("to@mail.com"));
        propertyStubsMap.put(SUBJECT, "subject");
        propertyStubsMap.put(BCC, List.of("bcc@mail.com"));
        propertyStubsMap.put(CC, List.of("cc@mail.com"));
        propertyStubsMap.put(REPLY_TO, List.of("reply@mail.com"));
        propertyStubsMap.put(HTML, "html");
        propertyStubsMap.put(TEXT, "text");
        propertyStubsMap.put(HEADERS, headerMap);
        propertyStubsMap.put(ATTACHMENTS, attachments);
        propertyStubsMap.put(TAGS, tags);

        return propertyStubsMap;
    }
}

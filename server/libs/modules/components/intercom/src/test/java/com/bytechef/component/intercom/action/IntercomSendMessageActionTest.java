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

package com.bytechef.component.intercom.action;

import static com.bytechef.component.intercom.action.IntercomSendMessageAction.POST_MESSAGES_CONTEXT_FUNCTION;
import static com.bytechef.component.intercom.constant.IntercomConstants.BODY;
import static com.bytechef.component.intercom.constant.IntercomConstants.FROM;
import static com.bytechef.component.intercom.constant.IntercomConstants.MESSAGE_TYPE;
import static com.bytechef.component.intercom.constant.IntercomConstants.SUBJECT;
import static com.bytechef.component.intercom.constant.IntercomConstants.TEMPLATE;
import static com.bytechef.component.intercom.constant.IntercomConstants.TO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.intercom.util.IntercomUtils;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

class IntercomSendMessageActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final Map<String, Object> responseMap = Map.of("key", "value");
    private final Map<String, String> fromMap = Map.of("type", "type", "id", "123");
    private final Map<String, String> toMap = Map.of("type", "admin", "id", "234");

    @Test
    void testPerform() {
        Map<String, Object> propertyStubsMap = createPropertyStubsMap();

        when(mockedParameters.getRequiredString(MESSAGE_TYPE))
            .thenReturn((String) propertyStubsMap.get(MESSAGE_TYPE));
        when(mockedParameters.getRequiredString(SUBJECT))
            .thenReturn((String) propertyStubsMap.get(SUBJECT));
        when(mockedParameters.getRequiredString(BODY))
            .thenReturn((String) propertyStubsMap.get(BODY));
        when(mockedParameters.getRequiredString(TEMPLATE))
            .thenReturn((String) propertyStubsMap.get(TEMPLATE));
        when(mockedParameters.getString(TO))
            .thenReturn("to");

        try (MockedStatic<IntercomUtils> intercomUtilsMockedStatic = mockStatic(IntercomUtils.class)) {
            intercomUtilsMockedStatic
                .when(() -> IntercomUtils.getContactRole("to", mockedContext))
                .thenReturn(fromMap);

            intercomUtilsMockedStatic
                .when(() -> IntercomUtils.getAdminId(mockedContext))
                .thenReturn(toMap);

            when(mockedContext.http(POST_MESSAGES_CONTEXT_FUNCTION))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.configuration(any()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.execute())
                .thenReturn(mockedResponse);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(responseMap);

            Object result = IntercomSendMessageAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(responseMap, result);

            Http.Body body = bodyArgumentCaptor.getValue();

            assertEquals(propertyStubsMap, body.getContent());
        }
    }

    private Map<String, Object> createPropertyStubsMap() {
        Map<String, Object> propertyStubsMap = new HashMap<>();

        propertyStubsMap.put(MESSAGE_TYPE, "id");
        propertyStubsMap.put(SUBJECT, "subject");
        propertyStubsMap.put(BODY, "body");
        propertyStubsMap.put(TEMPLATE, "template");
        propertyStubsMap.put(FROM, fromMap);
        propertyStubsMap.put(TO, toMap);

        return propertyStubsMap;
    }
}

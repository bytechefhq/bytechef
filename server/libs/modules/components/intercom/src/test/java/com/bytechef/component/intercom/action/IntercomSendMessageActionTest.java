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

import static com.bytechef.component.intercom.constant.IntercomConstants.BODY;
import static com.bytechef.component.intercom.constant.IntercomConstants.FROM;
import static com.bytechef.component.intercom.constant.IntercomConstants.MESSAGE_TYPE;
import static com.bytechef.component.intercom.constant.IntercomConstants.SUBJECT;
import static com.bytechef.component.intercom.constant.IntercomConstants.TEMPLATE;
import static com.bytechef.component.intercom.constant.IntercomConstants.TO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IntercomSendMessageActionTest {

    ArgumentCaptor<Context.Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Context.Http.Body.class);
    ActionContext mockedContext = mock(ActionContext.class);
    Context.Http.Executor mockedExecutor = mock(Context.Http.Executor.class);
    Parameters mockedParameters = mock(Parameters.class);
    Context.Http.Response mockedResponse = mock(Context.Http.Response.class);
    Map<String, Object> responeseMap = Map.of("key", "value");

    @BeforeEach
    public void beforeEach() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(Context.TypeReference.class)))
            .thenReturn(responeseMap);
    }

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
        when(mockedParameters.getRequired(FROM))
            .thenReturn(propertyStubsMap.get(FROM));
        when(mockedParameters.getRequired(TO))
            .thenReturn(propertyStubsMap.get(TO));

        Object result = IntercomSendMessageAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responeseMap, result);

        Context.Http.Body body = bodyArgumentCaptor.getValue();

        assertEquals(propertyStubsMap, body.getContent());
    }

    private Map<String, Object> createPropertyStubsMap() {
        Map<String, Object> propertyStubsMap = new HashMap<>();
        Map<String, Object> fromMap = new HashMap<>();

        fromMap.put("type", null);
        fromMap.put("id", null);

        propertyStubsMap.put(MESSAGE_TYPE, "id");
        propertyStubsMap.put(SUBJECT, "subject");
        propertyStubsMap.put(BODY, "body");
        propertyStubsMap.put(TEMPLATE, "template");
        propertyStubsMap.put(FROM, fromMap);
        propertyStubsMap.put(TO, Collections.emptyMap());

        return propertyStubsMap;
    }
}

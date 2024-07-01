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

package com.bytechef.component.twilio.action;

import static com.bytechef.component.twilio.constant.TwilioConstants.ACCOUNT_SID;
import static com.bytechef.component.twilio.constant.TwilioConstants.AUTH_TOKEN;
import static com.bytechef.component.twilio.constant.TwilioConstants.BODY;
import static com.bytechef.component.twilio.constant.TwilioConstants.FROM;
import static com.bytechef.component.twilio.constant.TwilioConstants.TO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TwilioSendSMSActionTest {

    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Context.Http.Executor mockedExecutor = mock(Context.Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Context.Http.Response mockedResponse = mock(Context.Http.Response.class);
    private final Map<String, Object> responeseMap = Map.of("key", "value");
    private final Parameters mockedConnectionParameters = mock(Parameters.class);
    private final ArgumentCaptor<Context.Http.Body> bodyCaptor = ArgumentCaptor.forClass(Context.Http.Body.class);

    @Test
    void testPerform() {

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.headers(anyMap()))  // Make sure this returns mockedExecutor
            .thenReturn(mockedExecutor);
        when(mockedExecutor.header(anyString(), anyString()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(Context.TypeReference.class)))
            .thenReturn(responeseMap);

        Map<String, Object> propertyStubsMap = createPropertyStubsMap();

        when(mockedParameters.getRequiredString(BODY))
            .thenReturn((String) propertyStubsMap.get(BODY));
        when(mockedParameters.getString(FROM))
            .thenReturn((String) propertyStubsMap.get(FROM));
        when(mockedParameters.getString(TO))
            .thenReturn((String) propertyStubsMap.get(TO));
        when(mockedParameters.getString(ACCOUNT_SID))
            .thenReturn((String) propertyStubsMap.get(ACCOUNT_SID));
        when(mockedParameters.getString(AUTH_TOKEN))
            .thenReturn((String) propertyStubsMap.get(AUTH_TOKEN));

        when(mockedConnectionParameters.getRequiredString(ACCOUNT_SID)).thenReturn((String) propertyStubsMap.get(ACCOUNT_SID));
        when(mockedConnectionParameters.getRequiredString(AUTH_TOKEN)).thenReturn((String) propertyStubsMap.get(AUTH_TOKEN));

        Object result = TwilioSendSMSAction.perform(mockedParameters, mockedConnectionParameters, mockedContext);

        assertEquals(responeseMap, result);

        Context.Http.Body body = bodyCaptor.getValue();
        Map<String, Object> expectedBodyContent = Map.of("body", "body");
        assertEquals(expectedBodyContent, body.getContent());
    }

    private static Map<String, Object> createPropertyStubsMap() {
        Map<String, Object> propertyStubsMap = new HashMap<>();

        propertyStubsMap.put(BODY, "body");
        propertyStubsMap.put(FROM, "from");
        propertyStubsMap.put(TO, "to");
        propertyStubsMap.put(ACCOUNT_SID, "accountSid");
        propertyStubsMap.put(AUTH_TOKEN, "AuthToken");

        return propertyStubsMap;
    }
}

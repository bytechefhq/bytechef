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

import static com.bytechef.component.definition.Context.Http.Body;
import static com.bytechef.component.definition.Context.Http.Executor;
import static com.bytechef.component.definition.Context.Http.Response;
import static com.bytechef.component.definition.Context.TypeReference;
import static com.bytechef.component.intercom.action.IntercomCreateContactAction.POST_CONTACTS_CONTEXT_FUNCTION;
import static com.bytechef.component.intercom.constant.IntercomConstants.AVATAR;
import static com.bytechef.component.intercom.constant.IntercomConstants.EMAIL;
import static com.bytechef.component.intercom.constant.IntercomConstants.NAME;
import static com.bytechef.component.intercom.constant.IntercomConstants.PHONE;
import static com.bytechef.component.intercom.constant.IntercomConstants.ROLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class IntercomCreateContactActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Response mockedResponse = mock(Response.class);
    private final Map<String, Object> responseMap = Map.of("key", "value");

    @Test
    void testPerform() {
        Map<String, Object> propertyStubsMap = createPropertyStubsMap();

        when(mockedParameters.getRequiredString(ROLE))
            .thenReturn((String) propertyStubsMap.get(ROLE));
        when(mockedParameters.getRequiredString(EMAIL))
            .thenReturn((String) propertyStubsMap.get(EMAIL));
        when(mockedParameters.getString(NAME))
            .thenReturn((String) propertyStubsMap.get(NAME));
        when(mockedParameters.getString(PHONE))
            .thenReturn((String) propertyStubsMap.get(PHONE));
        when(mockedParameters.getString(AVATAR))
            .thenReturn((String) propertyStubsMap.get(AVATAR));

        when(mockedContext.http(POST_CONTACTS_CONTEXT_FUNCTION))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        Object result = IntercomCreateContactAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responseMap, result);

        Body body = bodyArgumentCaptor.getValue();

        assertEquals(propertyStubsMap, body.getContent());
    }

    private static Map<String, Object> createPropertyStubsMap() {
        Map<String, Object> propertyStubsMap = new HashMap<>();

        propertyStubsMap.put(ROLE, "role");
        propertyStubsMap.put(EMAIL, "email");
        propertyStubsMap.put(NAME, "name");
        propertyStubsMap.put(PHONE, "phone");
        propertyStubsMap.put(AVATAR, "avatar");

        return propertyStubsMap;
    }
}

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

package com.bytechef.component.zoho.action;

import static com.bytechef.component.zoho.constant.ZohoCrmConstants.EMAIL;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.FIRST_NAME;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.LAST_NAME;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.PROFILE;
import static com.bytechef.component.zoho.constant.ZohoCrmConstants.ROLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Luka Ljubić
 * @author Monika Kušter
 */
class ZohoCrmAddUserActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(ROLE, "user_role", FIRST_NAME, "first_name", EMAIL, "email",
            PROFILE, "user_profile", LAST_NAME, "last_name"));
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void testPerform() {
        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(mockedObject);

        Object result = ZohoCrmAddUserAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        assertEquals(mockedObject, result);

        Http.Body body = bodyArgumentCaptor.getValue();

        Map<String, Object> userMap = new HashMap<>();

        userMap.put(ROLE, "user_role");
        userMap.put(FIRST_NAME, "first_name");
        userMap.put(EMAIL, "email");
        userMap.put(PROFILE, "user_profile");
        userMap.put(LAST_NAME, "last_name");

        assertEquals(Map.of("users", List.of(userMap)), body.getContent());
    }
}

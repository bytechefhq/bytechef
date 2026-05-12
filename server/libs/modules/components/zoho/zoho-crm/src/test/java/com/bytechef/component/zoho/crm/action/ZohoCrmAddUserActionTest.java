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

package com.bytechef.component.zoho.crm.action;

import static com.bytechef.component.zoho.crm.constant.ZohoCrmConstants.EMAIL;
import static com.bytechef.component.zoho.crm.constant.ZohoCrmConstants.FIRST_NAME;
import static com.bytechef.component.zoho.crm.constant.ZohoCrmConstants.LAST_NAME;
import static com.bytechef.component.zoho.crm.constant.ZohoCrmConstants.PROFILE;
import static com.bytechef.component.zoho.crm.constant.ZohoCrmConstants.ROLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
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
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Luka Ljubić
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class ZohoCrmAddUserActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(ROLE, "user_role", FIRST_NAME, "first_name", EMAIL, "email", PROFILE, "user_profile",
            LAST_NAME, "last_name"));

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

        Object result = ZohoCrmAddUserAction.perform(mockedParameters, null, mockedContext);

        assertEquals(mockedObject, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
        assertEquals("/users", stringArgumentCaptor.getValue());

        Map<String, Object> userMap = new HashMap<>();

        userMap.put(ROLE, "user_role");
        userMap.put(FIRST_NAME, "first_name");
        userMap.put(EMAIL, "email");
        userMap.put(PROFILE, "user_profile");
        userMap.put(LAST_NAME, "last_name");

        assertEquals(Body.of(Map.of("users", List.of(userMap)), BodyContentType.JSON), bodyArgumentCaptor.getValue());
    }
}

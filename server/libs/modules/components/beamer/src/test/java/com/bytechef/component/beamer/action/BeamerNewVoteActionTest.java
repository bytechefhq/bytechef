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

package com.bytechef.component.beamer.action;

import static com.bytechef.component.beamer.constant.BeamerConstants.FEATURE_REQUEST_ID;
import static com.bytechef.component.beamer.constant.BeamerConstants.USER_EMAIL;
import static com.bytechef.component.beamer.constant.BeamerConstants.USER_FIRST_NAME;
import static com.bytechef.component.beamer.constant.BeamerConstants.USER_ID;
import static com.bytechef.component.beamer.constant.BeamerConstants.USER_LAST_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
@ExtendWith(MockContextSetupExtension.class)
class BeamerNewVoteActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(
        FEATURE_REQUEST_ID, 2, USER_ID, 1010, USER_EMAIL, "testUserEmail",
        USER_FIRST_NAME, "Jane", USER_LAST_NAME, "Doe"));
    private final Map<String, Object> responseMap = Map.of();
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void perform(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        Map<String, Object> result = BeamerNewVoteAction.perform(
            mockedParameters, mockedParameters, mockedContext);

        assertEquals(responseMap, result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/requests/2/votes", stringArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());

        Map<String, Object> expectedBody = Map.of(
            USER_ID, "1010", USER_EMAIL, "testUserEmail", USER_FIRST_NAME, "Jane",
            USER_LAST_NAME, "Doe");

        assertEquals(Body.of(expectedBody, Http.BodyContentType.JSON), bodyArgumentCaptor.getValue());
    }
}

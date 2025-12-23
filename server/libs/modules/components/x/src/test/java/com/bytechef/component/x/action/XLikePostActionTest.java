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

package com.bytechef.component.x.action;

import static com.bytechef.component.definition.Context.ContextFunction;
import static com.bytechef.component.definition.Context.Http;
import static com.bytechef.component.x.constant.XConstants.TWEET_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.BasePerformFunction;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.x.util.XUtils;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class XLikePostActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final ArgumentCaptor<Http.Configuration.ConfigurationBuilder> configurationBuilderArgumentCaptor =
        forClass(Http.Configuration.ConfigurationBuilder.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    @SuppressWarnings("unchecked")
    private final ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor =
        forClass(ContextFunction.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http mockedHttp = mock(Http.class);
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(TWEET_ID, "abc"));
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform() throws Exception {
        try (MockedStatic<XUtils> xUtilsMockedStatic = mockStatic(XUtils.class)) {
            xUtilsMockedStatic.when(() -> XUtils.getAuthenticatedUserId(contextArgumentCaptor.capture()))
                .thenReturn("user123");

            Optional<? extends BasePerformFunction> performFunction = XLikePostAction.ACTION_DEFINITION.getPerform();

            assertTrue(performFunction.isPresent());

            PerformFunction singleConnectionPerformFunction = (PerformFunction) performFunction.get();

            when(mockedActionContext.http(httpFunctionArgumentCaptor.capture()))
                .thenAnswer(inv -> {
                    ContextFunction<Http, Http.Executor> value = httpFunctionArgumentCaptor.getValue();

                    return value.apply(mockedHttp);
                });
            when(mockedHttp.post(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.configuration(configurationBuilderArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.execute())
                .thenReturn(mockedResponse);
            when(mockedResponse.getBody())
                .thenReturn(mockedObject);

            Object result = singleConnectionPerformFunction.apply(mockedParameters, null, mockedActionContext);

            assertEquals(mockedObject, result);
            assertEquals(mockedActionContext, contextArgumentCaptor.getValue());
            ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

            assertNotNull(capturedFunction);

            Http.Configuration.ConfigurationBuilder configurationBuilder =
                configurationBuilderArgumentCaptor.getValue();
            Http.Configuration configuration = configurationBuilder.build();
            Http.ResponseType responseType = configuration.getResponseType();

            assertEquals(Http.ResponseType.Type.JSON, responseType.getType());
            assertEquals("/users/user123/likes", stringArgumentCaptor.getValue());
            assertEquals(Http.Body.of(Map.of(TWEET_ID, "abc"), Http.BodyContentType.JSON),
                bodyArgumentCaptor.getValue());
        }
    }
}

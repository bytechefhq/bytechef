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

package com.bytechef.component.attio.action;

import static com.bytechef.component.attio.constant.AttioConstants.ASSIGNEES;
import static com.bytechef.component.attio.constant.AttioConstants.CONTENT;
import static com.bytechef.component.attio.constant.AttioConstants.DATA;
import static com.bytechef.component.attio.constant.AttioConstants.DEADLINE_AT;
import static com.bytechef.component.attio.constant.AttioConstants.FORMAT;
import static com.bytechef.component.attio.constant.AttioConstants.IS_COMPLETED;
import static com.bytechef.component.attio.constant.AttioConstants.REFERENCED_ACTOR_ID;
import static com.bytechef.component.attio.constant.AttioConstants.REFERENCED_ACTOR_TYPE;
import static com.bytechef.component.attio.constant.AttioConstants.WORKSPACE_MEMBER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
@ExtendWith(MockContextSetupExtension.class)
class AttioCreateTaskActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Http.Body.class);
    private final Map<String, Object> responseMap = Map.of();
    private final LocalDateTime localDateTime = LocalDateTime.of(2025, 5, 5, 10, 10, 0);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            DEADLINE_AT, localDateTime, CONTENT, "content", IS_COMPLETED, true, ASSIGNEES, List.of("testAssignee")));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        Map<String, Object> result = AttioCreateTaskAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(Map.of(), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/tasks", stringArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());

        Map<String, Map<String, Object>> expectedBody = Map.of(
            DATA, Map.of(
                CONTENT, "content",
                FORMAT, "plaintext",
                DEADLINE_AT, localDateTime,
                IS_COMPLETED, true,
                ASSIGNEES,
                List.of(Map.of(REFERENCED_ACTOR_TYPE, WORKSPACE_MEMBER, REFERENCED_ACTOR_ID, "testAssignee"))));

        assertEquals(Body.of(expectedBody, Http.BodyContentType.JSON), bodyArgumentCaptor.getValue());
    }
}

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

package com.bytechef.component.linkedin.action;

import static com.bytechef.component.linkedin.constant.LinkedInConstants.COUNT;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.START;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.URN;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class LinkedInGetCommentsActionTest {

    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Parameters mockedParameters = MockParametersFactory.create(
            Map.of(URN, "urn:li:share:123", START, 5, COUNT, 20));

        Map<String, Object> comment = Map.of(
            "commentUrn", "urn:li:comment:(urn:li:activity:1,2)", "message", Map.of("text", "Hello"));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("elements", List.of(comment)));

        List<Map<?, ?>> result = LinkedInGetCommentsAction.perform(mockedParameters, null, mockedContext);

        assertEquals(List.of(comment), result);
        assertNotNull(httpFunctionArgumentCaptor.getValue());
        assertEquals("/rest/socialActions/urn%3Ali%3Ashare%3A123/comments", stringArgumentCaptor.getValue());
        assertArrayEquals(new Object[] {
            START, 5, COUNT, 20
        }, objectsArgumentCaptor.getValue());

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Configuration configuration = configurationBuilder.build();

        assertEquals(ResponseType.JSON, configuration.getResponseType());
    }

    @Test
    void testPerformWithDefaultPaging(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Parameters mockedParameters = MockParametersFactory.create(Map.of(URN, "urn:li:ugcPost:456"));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("paging", Map.of()));

        List<Map<?, ?>> result = LinkedInGetCommentsAction.perform(mockedParameters, null, mockedContext);

        assertEquals(List.of(), result);
        assertEquals("/rest/socialActions/urn%3Ali%3AugcPost%3A456/comments", stringArgumentCaptor.getValue());
        assertArrayEquals(new Object[] {
            START, 0, COUNT, 10
        }, objectsArgumentCaptor.getValue());
    }
}

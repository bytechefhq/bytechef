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

package com.bytechef.component.google.meet.action;

import static com.bytechef.component.google.meet.constant.GoogleMeetConstants.CONFERENCE_RECORDS;
import static com.bytechef.component.google.meet.constant.GoogleMeetConstants.NAME;
import static com.bytechef.component.google.meet.constant.GoogleMeetConstants.NEXT_PAGE_TOKEN;
import static com.bytechef.component.google.meet.constant.GoogleMeetConstants.PAGE_SIZE;
import static com.bytechef.component.google.meet.constant.GoogleMeetConstants.PAGE_TOKEN;
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
 * @author Marija Horvat
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class GoogleMeetListParticipantsActionTest {

    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(CONFERENCE_RECORDS, "test"));
    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of("participants", List.of(Map.of(NAME, "first")), NEXT_PAGE_TOKEN, "t1"),
                Map.of("participants", List.of(Map.of(NAME, "second"))));

        Object result = GoogleMeetListParticipantsAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(List.of(Map.of(NAME, "first"), Map.of(NAME, "second")), result);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();

        Configuration configuration = configurationBuilder.build();

        ResponseType responseType = configuration.getResponseType();

        assertEquals(ResponseType.Type.JSON, responseType.getType());
        assertEquals(List.of("/test/participants", "/test/participants"), stringArgumentCaptor.getAllValues());

        List<Object[]> allQueryParams = objectsArgumentCaptor.getAllValues();

        assertEquals(2, allQueryParams.size());

        Object[] queryParameters1 = {
            PAGE_SIZE, 250,
            PAGE_TOKEN, null
        };
        Object[] queryParameters2 = {
            PAGE_SIZE, 250,
            PAGE_TOKEN, "t1"
        };

        assertArrayEquals(queryParameters1, allQueryParams.get(0));
        assertArrayEquals(queryParameters2, allQueryParams.get(1));
    }
}

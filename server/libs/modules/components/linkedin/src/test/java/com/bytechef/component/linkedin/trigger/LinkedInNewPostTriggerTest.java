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

package com.bytechef.component.linkedin.trigger;

import static com.bytechef.component.linkedin.constant.LinkedInConstants.URN;
import static com.bytechef.component.linkedin.trigger.LinkedInNewPostTrigger.LAST_TIME_CHECKED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class LinkedInNewPostTriggerTest {

    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = ArgumentCaptor.forClass(Object[].class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(URN, "123"));
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<ZoneId> zoneIdArgumentCaptor = ArgumentCaptor.forClass(ZoneId.class);

    @Test
    void testPoll(
        TriggerContext mockedTriggerContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        LocalDateTime startDate = LocalDateTime.of(2000, 1, 1, 1, 1, 1);
        LocalDateTime endDateTime = LocalDateTime.of(2023, 1, 1, 1, 1, 1);

        long epochMilli = startDate.atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();

        Parameters closureParameters = MockParametersFactory.create(Map.of(LAST_TIME_CHECKED, startDate));

        try (MockedStatic<LocalDateTime> localDateTimeMockedStatic = mockStatic(
            LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {

            localDateTimeMockedStatic.when(() -> LocalDateTime.now(zoneIdArgumentCaptor.capture()))
                .thenReturn(endDateTime);

            when(mockedHttp.get(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(
                    Map.of(
                        "elements",
                        List.of(
                            Map.of("id", "post1", "createdAt", 123L),
                            Map.of("id", "post2", "createdAt", epochMilli + 10)),
                        "paging", Map.of("links", List.of(Map.of("rel", "next", "href", "nextLink")))),
                    Map.of("paging", Map.of("links", List.of(Map.of("rel", "prev", "href", "prevLink")))));

            PollOutput pollOutput = LinkedInNewPostTrigger.poll(
                mockedParameters, null, closureParameters, mockedTriggerContext);

            PollOutput expectedPollOutput = new PollOutput(
                List.of(Map.of("id", "post2", "createdAt", epochMilli + 10)),
                Map.of(LAST_TIME_CHECKED, endDateTime), false);

            assertEquals(expectedPollOutput, pollOutput);
            assertNotNull(httpFunctionArgumentCaptor.getValue());
            assertEquals(List.of("/rest/posts", "nextLink"), stringArgumentCaptor.getAllValues());

            Object[] queryParameters = {
                "q", "author", "author", "urn:li:organization:123", "sortBy", "CREATED", "count", 100
            };

            assertArrayEquals(queryParameters, objectsArgumentCaptor.getValue());
            assertEquals(ZoneId.systemDefault(), zoneIdArgumentCaptor.getValue());

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();

            assertEquals(ResponseType.JSON, configuration.getResponseType());
        }
    }
}

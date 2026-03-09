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

package com.bytechef.microsoft.commons;

import static com.bytechef.microsoft.commons.MicrosoftConstants.ID;
import static com.bytechef.microsoft.commons.MicrosoftConstants.LAST_TIME_CHECKED;
import static com.bytechef.microsoft.commons.MicrosoftConstants.NAME;
import static com.bytechef.microsoft.commons.MicrosoftConstants.VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
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

/**
 * @author Nikolina Spehar
 */
@ExtendWith(MockContextSetupExtension.class)
class MicrosoftTriggerUtilsTest {

    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPoll(
        TriggerContext mockedTriggerContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        String mockedUrl = "mocked/url";
        String mockedContainsKey = "key";
        Map<String, String> fileMap = Map.of(
            NAME, "some name", ID, "abc", "createdDateTime", "2024-01-01T14:28:23Z",
            "key", "key");

        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 2, 0, 0, 0);

        Parameters mockedClosureParameters = MockParametersFactory.create(Map.of(LAST_TIME_CHECKED, startDate));

        try (MockedStatic<LocalDateTime> localDateTimeMockedStatic = mockStatic(
            LocalDateTime.class, CALLS_REAL_METHODS)) {

            localDateTimeMockedStatic.when(() -> LocalDateTime.now(any(ZoneId.class)))
                .thenReturn(endDate);

            when(mockedHttp.get(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(Map.of(VALUE, List.of(fileMap)));

            PollOutput pollOutput = MicrosoftTriggerUtils.poll(
                mockedUrl, mockedContainsKey, mockedClosureParameters, mockedTriggerContext);

            PollOutput expectedPollOutput = new PollOutput(
                List.of(fileMap), Map.of(LAST_TIME_CHECKED, endDate), false);

            assertEquals(expectedPollOutput, pollOutput);

            ContextFunction<Http, Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

            assertNotNull(capturedFunction);

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();
            ResponseType responseType = configuration.getResponseType();

            assertEquals(ResponseType.JSON, responseType);
            assertEquals("mocked/url", stringArgumentCaptor.getValue());
        }
    }
}

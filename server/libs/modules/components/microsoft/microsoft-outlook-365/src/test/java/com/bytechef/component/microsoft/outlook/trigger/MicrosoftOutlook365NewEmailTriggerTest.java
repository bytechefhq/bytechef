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

package com.bytechef.component.microsoft.outlook.trigger;

import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FORMAT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ODATA_NEXT_LINK;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.VALUE;
import static com.bytechef.component.microsoft.outlook.trigger.MicrosoftOutlook365NewEmailTrigger.LAST_TIME_CHECKED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
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
import com.bytechef.component.microsoft.outlook.definition.Format;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import com.bytechef.microsoft.commons.MicrosoftUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
class MicrosoftOutlook365NewEmailTriggerTest {

    LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
    private final Parameters mockedClosureParameters =
        MockParametersFactory.create(Map.of(LAST_TIME_CHECKED, startDate));
    private final Parameters mockedInputParameters = MockParametersFactory.create(Map.of(FORMAT, Format.FULL));
    private final ArgumentCaptor<Object[]> queryArgumentCaptor = forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPoll(
        TriggerContext mockedTriggerContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        Map<String, String> firstMail = Map.of(ID, "abc", "receivedDateTime", "2024-01-01T14:28:23Z");

        LocalDateTime endDate = LocalDateTime.of(2024, 1, 2, 0, 0, 0);

        try (MockedStatic<LocalDateTime> localDateTimeMockedStatic = mockStatic(
            LocalDateTime.class, Mockito.CALLS_REAL_METHODS);

            MockedStatic<MicrosoftUtils> microsoftUtilsMockedStatic = mockStatic(MicrosoftUtils.class)) {

            localDateTimeMockedStatic.when(() -> LocalDateTime.now(any(ZoneId.class)))
                .thenReturn(endDate);

            Map<String, String> secondMail = Map.of(ID, "cdf", "receivedDateTime", "2024-01-01T18:23:44Z");

            microsoftUtilsMockedStatic
                .when(() -> MicrosoftUtils.getItemsFromNextPage("link", mockedTriggerContext))
                .thenReturn(List.of(secondMail));

            when(mockedHttp.get(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(Map.of(VALUE, List.of(firstMail), ODATA_NEXT_LINK, "link"));

            PollOutput pollOutput = MicrosoftOutlook365NewEmailTrigger.poll(
                mockedInputParameters, null, mockedClosureParameters, mockedTriggerContext);

            PollOutput expectedPollOutput = new PollOutput(
                List.of(firstMail, secondMail), Map.of(LAST_TIME_CHECKED, endDate), false);

            assertEquals(expectedPollOutput, pollOutput);
            assertNotNull(httpFunctionArgumentCaptor.getValue());
            assertEquals("/me/mailFolders/Inbox/messages", stringArgumentCaptor.getValue());

            ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
            Configuration configuration = configurationBuilder.build();

            assertEquals(ResponseType.JSON, configuration.getResponseType());

            Object[] query = queryArgumentCaptor.getValue();

            String formattedStartDate = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .withZone(ZoneId.systemDefault()));

            Object[] objects = {
                "$filter", "isRead eq false and receivedDateTime ge " + formattedStartDate,
                "$orderby", "receivedDateTime desc"
            };

            assertArrayEquals(objects, query);
        }
    }
}

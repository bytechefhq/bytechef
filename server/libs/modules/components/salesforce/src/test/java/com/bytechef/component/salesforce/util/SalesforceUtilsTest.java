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

package com.bytechef.component.salesforce.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.salesforce.constant.SalesforceConstants.LAST_TIME_CHECKED;
import static com.bytechef.component.salesforce.constant.SalesforceConstants.OBJECT;
import static com.bytechef.component.salesforce.constant.SalesforceConstants.Q;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class SalesforceUtilsTest {

    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);

    @Test
    void testExecuteSOQLQuery() {
        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of());

        String query = "SELECT Id FROM Account";
        Map<String, ?> result = SalesforceUtils.executeSOQLQuery(mockedActionContext, query);

        assertEquals(Map.of(), result);

        assertEquals(
            List.of("q", URLEncoder.encode(query, StandardCharsets.UTF_8)),
            stringArgumentCaptor.getAllValues());
    }

    @Test
    void testGetPollOutput() {
        List<Map<String, String>> records = List.of(Map.of("Id", "123"));
        LocalDateTime startDate = LocalDateTime.of(2000, 1, 1, 1, 1, 1);

        Parameters mockedParameters =
            MockParametersFactory.create(Map.of(LAST_TIME_CHECKED, startDate, OBJECT, "Account"));

        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("records", records));

        PollOutput pollOutput = SalesforceUtils.getPollOutput(
            mockedParameters, mockedParameters, mockedTriggerContext, "CreatedDate");

        assertEquals(records, pollOutput.records());
        assertFalse(pollOutput.pollImmediately());

        String query =
            "SELECT FIELDS(ALL) FROM Account WHERE CreatedDate > 2000-01-01T01:01:01Z ORDER BY CreatedDate ASC LIMIT 200 OFFSET 0";
        assertEquals(
            List.of(Q, URLEncoder.encode(query, StandardCharsets.UTF_8)),
            stringArgumentCaptor.getAllValues());
    }

    @Test
    void testGetRecordIdOptions() {
        Parameters parameters = MockParametersFactory.create(Map.of(OBJECT, "Account"));

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("records", List.of(Map.of("Id", "123"))));

        List<Option<String>> result =
            SalesforceUtils.getRecordIdOptions(parameters, parameters, Map.of(), null, mockedActionContext);

        assertEquals(List.of(option("123", "123")), result);

        assertEquals(
            List.of(Q, URLEncoder.encode("SELECT Id FROM Account", StandardCharsets.UTF_8)),
            stringArgumentCaptor.getAllValues());
    }

    @Test
    void testGetSalesforceObjectOptions() {
        Parameters parameters = MockParametersFactory.create(Map.of(OBJECT, "Account"));

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("sobjects", List.of(Map.of("name", "123", "label", "abc"))));

        List<Option<String>> result =
            SalesforceUtils.getSalesforceObjectOptions(parameters, parameters, Map.of(), null, mockedActionContext);

        assertEquals(List.of(option("abc", "123")), result);
    }
}

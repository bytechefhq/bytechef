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

package com.bytechef.component.zendesk.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.COMMENT;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.NAME;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.SUBJECT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
class ZendeskUtilsTest {

    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(COMMENT, "comment"));
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final Map<String, Object> responseMap = Map.of(
        "tickets", List.of(
            Map.of(SUBJECT, "subject1", "id", 1),
            Map.of(SUBJECT, "subject2", "id", 2)),
        "users", List.of(
            Map.of("id", 1, NAME, "name1"),
            Map.of("id", 2, NAME, "name2")));
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testCheckIfNull() {
        String nullValue = ZendeskUtils.checkIfNull(null);
        assertEquals("", nullValue);

        String notNullValue = ZendeskUtils.checkIfNull("test");
        assertEquals("test", notNullValue);
    }

    @Test
    void testGetTicketIdOptions() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.header(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        List<Option<Long>> result = ZendeskUtils.getTicketIdOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<Long>> expected = List.of(
            option("subject1", 1),
            option("subject2", 2));

        assertEquals(expected, result);

        List<String> expectedHeader = List.of("Accept", "application/json");
        assertEquals(expectedHeader, stringArgumentCaptor.getAllValues());
    }
}

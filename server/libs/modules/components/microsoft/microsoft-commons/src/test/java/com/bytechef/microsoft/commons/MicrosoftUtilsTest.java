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

import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class MicrosoftUtilsTest {

    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void testGetOptionsWithoutNextLink() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        Map<String, Object> body = Map.of(
            MicrosoftUtils.VALUE,
            List.of(Map.of("displayName", "Team A", "id", "1")));

        List<Option<String>> result = MicrosoftUtils.getOptions(mockedContext, body, "displayName", "id");

        assertEquals(List.of(option("Team A", "1")), result);
    }

    @Test
    void testGetOptionsWithNextLinkMultiplePages() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        Map<String, Object> initialBody = Map.of(
            MicrosoftUtils.VALUE,
            List.of(Map.of("displayName", "Team A", "id", "1")),
            MicrosoftUtils.ODATA_NEXT_LINK, "https://graph.microsoft.com/v1.0/teams?$skiptoken=abc");

        Map<String, Object> page1 = Map.of(
            MicrosoftUtils.VALUE,
            List.of(Map.of("displayName", "Team B", "id", "2")),
            MicrosoftUtils.ODATA_NEXT_LINK, "https://graph.microsoft.com/v1.0/teams?$skiptoken=def");

        Map<String, Object> page2 = Map.of(
            MicrosoftUtils.VALUE,
            List.of(Map.of("displayName", "Team C", "id", "3")));

        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(page1)
            .thenReturn(page2);

        List<Option<String>> result = MicrosoftUtils.getOptions(mockedContext, initialBody, "displayName", "id");

        List<Option<String>> expected = new ArrayList<>();

        expected.add(option("Team A", "1"));
        expected.add(option("Team B", "2"));
        expected.add(option("Team C", "3"));

        assertEquals(expected, result);
    }

    @Test
    void testGetItemsFromNextPageNullLink() {
        List<Map<?, ?>> items = MicrosoftUtils.getItemsFromNextPage(null, mockedContext);

        assertTrue(items.isEmpty());
    }

    @Test
    void testProcessErrorResponseExtractsMessageFromJson() {
        when(mockedContext.json(any()))
            .thenReturn(Map.of("error", Map.of("message", "Something went wrong")));

        ProviderException providerException = MicrosoftUtils.processErrorResponse(
            400, "{\"error\":{\"message\":\"Something went wrong\"}}", mockedContext);

        assertEquals(400, providerException.getStatusCode());
        assertEquals("Something went wrong", providerException.getMessage());
    }

    @Test
    void testProcessErrorResponseFallbackToBodyToString() {
        when(mockedContext.json(any()))
            .thenReturn("not a map");

        ProviderException providerException = MicrosoftUtils.processErrorResponse(500, "raw error", mockedContext);

        assertEquals(500, providerException.getStatusCode());
        assertEquals("raw error", providerException.getMessage());
    }
}

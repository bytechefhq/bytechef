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

package com.bytechef.component.greenhouse.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class GreenhouseUtilsTest {

    @Test
    void testGetRecordByIdNotFound(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {
        when(mockedHttp.get("/jobs"))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter("ids", "6404"))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of());

        ProviderException providerException = assertThrows(
            ProviderException.class, () -> GreenhouseUtils.getRecordById("/jobs", "6404", "job", mockedContext));

        assertEquals("Greenhouse job with ID 6404 was not found.", providerException.getMessage());
    }

    @Test
    void testGetAllRecordsFollowsNextCursor(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        List<Map<String, List<String>>> requestedQueryParameters = new ArrayList<>();

        when(mockedHttp.get("/scorecards"))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(any(Map.class)))
            .thenAnswer(invocation -> {
                requestedQueryParameters.add(invocation.getArgument(0));

                return mockedExecutor;
            });
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(Map.of("id", 2)), List.of(Map.of("id", 1)));
        when(mockedResponse.getHeader("link"))
            .thenReturn(
                List.of("<https://harvest.greenhouse.io/v3/scorecards?cursor=abc%3D%3D>; rel=\"next\""), List.of());

        List<Map<String, Object>> result = GreenhouseUtils.getAllRecords(
            "/scorecards", Map.of("application_ids", List.of("23558552")), mockedContext);

        assertEquals(List.of(Map.of("id", 2), Map.of("id", 1)), result);
        assertEquals(
            List.of(
                Map.of("application_ids", List.of("23558552"), "per_page", List.of("500")),
                Map.of("cursor", List.of("abc=="))),
            requestedQueryParameters);
    }

    @Test
    void testGetNextCursor() {
        assertEquals(
            "abc==",
            GreenhouseUtils.getNextCursor(
                List.of("<https://harvest.greenhouse.io/v3/jobs?cursor=abc%3D%3D>; rel=\"next\"")));
        assertEquals(
            "xyz",
            GreenhouseUtils.getNextCursor(
                List.of("<https://harvest.greenhouse.io/v3/jobs?per_page=2&cursor=xyz>;rel=\"next\"")));
        assertNull(GreenhouseUtils.getNextCursor(List.of("")));
        assertNull(GreenhouseUtils.getNextCursor(null));
    }
}

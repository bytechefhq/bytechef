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

package com.bytechef.component.okta.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Option;
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
class OktaUtilsTest {

    private static final String NEXT_LINK =
        "<https://your-org.okta.com/api/v1/users?limit=200>; rel=\"self\", " +
            "<https://your-org.okta.com/api/v1/users?after=00u2%3Dx&limit=200>; rel=\"next\"";

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private final ArgumentCaptor<Map<String, List<String>>> queryParametersArgumentCaptor =
        (ArgumentCaptor) forClass(Map.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testEncodePathSegment() {
        assertEquals("john.doe%40example.com", OktaUtils.encodePathSegment("john.doe@example.com"));
        assertEquals("john%20doe", OktaUtils.encodePathSegment("john doe"));
    }

    @Test
    void testGetApplicationIdOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryParametersArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(Map.of("id", "0oa1", "label", "Salesforce")));

        List<Option<String>> result = OktaUtils.getApplicationIdOptions(null, null, Map.of(), "Sales", mockedContext);

        assertEquals(List.of(option("Salesforce", "0oa1")), result);
        assertEquals("/apps", stringArgumentCaptor.getValue());
        assertEquals(Map.of("q", List.of("Sales"), "limit", List.of("200")), queryParametersArgumentCaptor.getValue());
    }

    @Test
    void testGetGroupIdOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryParametersArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(Map.of("id", "00g1", "profile", Map.of("name", "Everyone")), Map.of("id", "00g2")));

        List<Option<String>> result = OktaUtils.getGroupIdOptions(null, null, Map.of(), null, mockedContext);

        assertEquals(List.of(option("Everyone", "00g1"), option("00g2", "00g2")), result);
        assertEquals("/groups", stringArgumentCaptor.getValue());
        assertEquals(Map.of("limit", List.of("200")), queryParametersArgumentCaptor.getValue());
    }

    @Test
    void testGetItemsFollowsTheLinkHeader(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryParametersArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(Map.of("id", "00u1")))
            .thenReturn(List.of(Map.of("id", "00u2")));
        when(mockedResponse.getHeader("Link"))
            .thenReturn(List.of(NEXT_LINK))
            .thenReturn(List.of());

        List<Map<String, Object>> result = OktaUtils.getItems(mockedContext, "/users", Map.of(), null);

        assertEquals(List.of(Map.of("id", "00u1"), Map.of("id", "00u2")), result);
        assertEquals(List.of("/users", "/users"), stringArgumentCaptor.getAllValues());

        List<Map<String, List<String>>> queryParameters = queryParametersArgumentCaptor.getAllValues();

        assertEquals(Map.of("limit", List.of("200")), queryParameters.get(0));
        assertEquals(Map.of("limit", List.of("200"), "after", List.of("00u2=x")), queryParameters.get(1));
    }

    @Test
    void testGetItemsStopsAtLimit(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryParametersArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(Map.of("id", "00u1"), Map.of("id", "00u2")));
        when(mockedResponse.getHeader("Link"))
            .thenReturn(List.of(NEXT_LINK));

        List<Map<String, Object>> result = OktaUtils.getItems(mockedContext, "/users", Map.of(), 2);

        assertEquals(List.of(Map.of("id", "00u1"), Map.of("id", "00u2")), result);
        assertEquals(List.of("/users"), stringArgumentCaptor.getAllValues());
        assertEquals(Map.of("limit", List.of("2")), queryParametersArgumentCaptor.getValue());
    }

    @Test
    void testGetNextCursor() {
        assertEquals("00u2=x", OktaUtils.getNextCursor(List.of(NEXT_LINK)));
        assertEquals(
            "00u3",
            OktaUtils.getNextCursor(
                List.of(
                    "<https://your-org.okta.com/api/v1/users?limit=200>; rel=\"self\"",
                    "<https://your-org.okta.com/api/v1/users?limit=200&after=00u3>; rel=\"next\"")));
        assertNull(
            OktaUtils.getNextCursor(List.of("<https://your-org.okta.com/api/v1/users?limit=200>; rel=\"self\"")));
        assertNull(OktaUtils.getNextCursor(null));
    }

    @Test
    void testGetQueryParameters() {
        Parameters parameters = MockParametersFactory.create(Map.of("search", "status eq \"ACTIVE\"", "q", " "));

        assertEquals(
            Map.of("search", List.of("status eq \"ACTIVE\"")),
            OktaUtils.getQueryParameters(parameters, "search", "filter", "q"));
    }

    @Test
    void testNormalizeDomain() {
        assertEquals("your-org.okta.com", OktaUtils.normalizeDomain("your-org.okta.com"));
        assertEquals("your-org.okta.com", OktaUtils.normalizeDomain(" https://your-org.okta.com/ "));
        assertEquals("your-org.okta.com", OktaUtils.normalizeDomain("http://your-org.okta.com"));
    }
}

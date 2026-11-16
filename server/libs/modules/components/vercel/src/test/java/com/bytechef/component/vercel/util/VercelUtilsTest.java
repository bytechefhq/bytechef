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

package com.bytechef.component.vercel.util;

import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.Context.Http;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
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
class VercelUtilsTest {

    private final ArgumentCaptor<Object[]> queryArgumentCaptor = forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testGetProjectIdOptions(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {
        Parameters parameters = MockParametersFactory.create(Map.of("teamId", "team_1"));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("projects", List.of(Map.of("id", "prj_1", "name", "web"))));

        List<Option<String>> result = VercelUtils.getProjectIdOptions(parameters, null, null, "we", mockedContext);

        assertEquals(List.of(option("web", "prj_1")), result);
        assertEquals("/v10/projects", stringArgumentCaptor.getValue());
        assertArrayEquals(new Object[] {
            "teamId", "team_1", "search", "we"
        }, queryArgumentCaptor.getValue());
    }

    @Test
    void testGetProjectsFollowsPagination(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of(
                    "pagination", Map.of("count", 1, "next", 1700000000000L),
                    "projects", List.of(Map.of("id", "prj_1", "name", "web"))),
                Map.of(
                    "pagination", Map.of("count", 1),
                    "projects", List.of(Map.of("id", "prj_2", "name", "api"))));

        List<Map<String, Object>> result = VercelUtils.getProjects(mockedContext, "team_1", null);

        assertEquals(
            List.of(Map.of("id", "prj_1", "name", "web"), Map.of("id", "prj_2", "name", "api")), result);
        assertArrayEquals(new Object[] {
            "teamId", "team_1", "from", "1700000000000"
        }, queryArgumentCaptor.getAllValues()
            .get(1));
    }

    @Test
    void testGetProjectsAcceptsBareArray(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(Map.of("id", "prj_1", "name", "web", "framework", "nextjs")));

        List<Map<String, Object>> result = VercelUtils.getProjects(mockedContext, null, null);

        assertEquals(List.of(Map.of("id", "prj_1", "name", "web")), result);
    }

    @Test
    void testGetProjectsStopsAtPageCap(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of(
                    "pagination", Map.of("count", 1, "next", 5),
                    "projects", List.of(Map.of("id", "prj_1", "name", "web"))));

        List<Map<String, Object>> result = VercelUtils.getProjects(mockedContext, null, null);

        assertEquals(50, result.size());
        verify(mockedHttp, times(50)).get("/v10/projects");
    }

    @Test
    void testToDeploymentSummaryPrependsScheme() {
        Map<String, Object> bare = new java.util.HashMap<>();

        bare.put("id", "dpl_1");
        bare.put("url", "web-abc.vercel.app");

        Map<String, Object> prefixed = new java.util.HashMap<>();

        prefixed.put("url", "http://web.example.com");

        Map<String, Object> missing = new java.util.HashMap<>();

        assertEquals("https://web-abc.vercel.app", VercelUtils.toDeploymentSummary(bare)
            .get("url"));
        assertEquals("http://web.example.com", VercelUtils.toDeploymentSummary(prefixed)
            .get("url"));
        assertNull(VercelUtils.toDeploymentSummary(missing)
            .get("url"));
    }

    @Test
    void testToDeploymentSummaryOfAnEmptyBodyHasNoValues() {
        Map<String, Object> summary = VercelUtils.toDeploymentSummary(null);

        assertNull(summary.get("id"));
        assertNull(summary.get("url"));
        assertNull(summary.get("state"));
    }

    @Test
    void testEncodePathSegmentEscapesSeparatorsAndSpaces() {
        assertEquals("dpl_1", VercelUtils.encodePathSegment("dpl_1"));
        assertEquals("a%2Fb%3Fc%23d%20e", VercelUtils.encodePathSegment("a/b?c#d e"));
    }
}

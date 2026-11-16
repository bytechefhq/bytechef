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

package com.bytechef.component.vercel.action;

import static com.bytechef.component.definition.Context.Http;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockContextSetupExtension.class, ObjectMapperSetupExtension.class
})
class VercelCreateDeploymentActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<Object[]> queryArgumentCaptor = forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerformProduction(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Parameters parameters = MockParametersFactory.create(
            Map.of(
                "project", "prj_1", "org", "acme", "repo", "web", "ref", "main", "sha", "abc123",
                "target", "production", "teamId", "team_1"));

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("id", "dpl_1", "url", "web-abc.vercel.app", "readyState", "QUEUED"));

        Map<String, Object> result = VercelCreateDeploymentAction.perform(parameters, null, mockedContext);

        assertEquals("dpl_1", result.get("id"));
        assertEquals("https://web-abc.vercel.app", result.get("url"));
        assertEquals("QUEUED", result.get("state"));
        assertEquals("/v13/deployments", stringArgumentCaptor.getValue());
        assertArrayEquals(new Object[] {
            "teamId", "team_1"
        }, queryArgumentCaptor.getValue());
        assertEquals(
            Body.of(
                Map.of(
                    "name", "prj_1", "project", "prj_1", "target", "production",
                    "gitSource",
                    Map.of("type", "github", "org", "acme", "repo", "web", "ref", "main", "sha", "abc123")),
                BodyContentType.JSON),
            bodyArgumentCaptor.getValue());
    }

    @Test
    void testPerformPreviewOmitsTargetAndTeamId(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Parameters parameters = MockParametersFactory.create(
            Map.of("project", "web", "org", "acme", "repo", "web", "ref", "feature", "target", "preview"));

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("id", "dpl_2", "url", "web-feature.vercel.app", "readyState", "QUEUED"));

        Map<String, Object> result = VercelCreateDeploymentAction.perform(parameters, null, mockedContext);

        assertNull(result.get("errorMessage"));
        verify(mockedExecutor, never()).queryParameters(any(Object[].class));
        assertEquals(
            Body.of(
                Map.of(
                    "name", "web", "project", "web",
                    "gitSource", Map.of("type", "github", "org", "acme", "repo", "web", "ref", "feature")),
                BodyContentType.JSON),
            bodyArgumentCaptor.getValue());
    }
}

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
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class VercelGetDeploymentActionTest {

    private final ArgumentCaptor<Object[]> queryArgumentCaptor = forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerformFailedDeployment(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Parameters parameters = MockParametersFactory.create(Map.of("deploymentId", "dpl_1", "teamId", "team_1"));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of(
                    "id", "dpl_1", "url", "web-abc.vercel.app", "readyState", "ERROR", "errorCode", "BUILD_FAILED",
                    "errorMessage", "Command failed"));

        Map<String, Object> result = VercelGetDeploymentAction.perform(parameters, null, mockedContext);

        assertEquals("dpl_1", result.get("id"));
        assertEquals("https://web-abc.vercel.app", result.get("url"));
        assertEquals("ERROR", result.get("state"));
        assertEquals("BUILD_FAILED", result.get("errorCode"));
        assertEquals("Command failed", result.get("errorMessage"));
        assertEquals("/v13/deployments/dpl_1", stringArgumentCaptor.getValue());
        assertArrayEquals(new Object[] {
            "teamId", "team_1"
        }, queryArgumentCaptor.getValue());
    }

    @Test
    void testPerformEncodesTheDeploymentIdInThePath(
        ActionContext mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Parameters parameters = MockParametersFactory.create(Map.of("deploymentId", "../projects?x=1 y"));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(null);

        Map<String, Object> result = VercelGetDeploymentAction.perform(parameters, null, mockedContext);

        assertEquals("/v13/deployments/..%2Fprojects%3Fx%3D1%20y", stringArgumentCaptor.getValue());
        assertNull(result.get("id"));
        assertNull(result.get("state"));
    }
}

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

package com.bytechef.component.github.action;

import static com.bytechef.component.github.constant.GithubConstants.INPUTS;
import static com.bytechef.component.github.constant.GithubConstants.OWNER;
import static com.bytechef.component.github.constant.GithubConstants.REF;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.constant.GithubConstants.WORKFLOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
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
class GithubTriggerWorkflowDispatchActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    void testPerform(Context mockedContext, Executor mockedExecutor, Http mockedHttp) {
        Parameters parameters = MockParametersFactory.create(
            Map.of(
                OWNER, "bytechefhq", REPOSITORY, "bytechef", WORKFLOW, "changelog-media.yml", REF, "master",
                INPUTS, Map.of("tag", "v1.0.0")));

        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);

        Map<String, Object> result = GithubTriggerWorkflowDispatchAction.perform(parameters, null, mockedContext);

        assertEquals(Map.of(WORKFLOW, "changelog-media.yml", REF, "master", "dispatched", true), result);
        assertEquals(
            "/repos/bytechefhq/bytechef/actions/workflows/changelog-media.yml/dispatches",
            stringArgumentCaptor.getValue());
        assertEquals(
            Body.of(Map.of(REF, "master", INPUTS, Map.of("tag", "v1.0.0")), BodyContentType.JSON),
            bodyArgumentCaptor.getValue());
    }

    @Test
    void testPerformDefaultsRefToTheDefaultBranch(
        Context mockedContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Parameters parameters = MockParametersFactory.create(
            Map.of(OWNER, "bytechefhq", REPOSITORY, "bytechef", WORKFLOW, "build.yml"));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedHttp.post(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of("default_branch", "main"));

        Map<String, Object> result = GithubTriggerWorkflowDispatchAction.perform(parameters, null, mockedContext);

        assertEquals("main", result.get(REF));
        assertEquals(
            List.of("/repos/bytechefhq/bytechef", "/repos/bytechefhq/bytechef/actions/workflows/build.yml/dispatches"),
            stringArgumentCaptor.getAllValues());
        assertEquals(Body.of(Map.of(REF, "main", INPUTS, Map.of()), BodyContentType.JSON),
            bodyArgumentCaptor.getValue());
    }
}

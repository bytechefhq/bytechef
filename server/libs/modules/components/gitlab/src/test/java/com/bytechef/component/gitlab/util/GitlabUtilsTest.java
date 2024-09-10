/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.gitlab.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.gitlab.constant.GitlabConstants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * * @author Monika Kušter
 */
class GitlabUtilsTest {

    private final List<Option<String>> expectedOptions = List.of(option("some name", "123"));
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<Map<String, List<String>>> queryArgumentCaptor = ArgumentCaptor.forClass(Map.class);

    @BeforeEach()
    void beforeEach() {
        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(queryArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
    }

    @Test
    void testGetProjectOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(Map.of("name", "some name", ID, 123)));

        assertEquals(expectedOptions,
            GitlabUtils.getProjectOptions(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext));

        Map<String, List<String>> query = queryArgumentCaptor.getValue();

        assertEquals(Map.of("simple", List.of("true"), "membership", List.of("true")), query);
    }

    @Test
    void testGetIssueOptions() {
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(Map.of("title", "some name", "iid", 123)));

        assertEquals(expectedOptions,
            GitlabUtils.getIssueOptions(mockedParameters, mockedParameters, Map.of(), "", mockedActionContext));

    }

}

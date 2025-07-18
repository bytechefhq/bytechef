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

package com.bytechef.component.bitbucket.util;

import static com.bytechef.component.bitbucket.constant.BitbucketConstants.KEY;
import static com.bytechef.component.bitbucket.constant.BitbucketConstants.NAME;
import static com.bytechef.component.bitbucket.constant.BitbucketConstants.PAGE;
import static com.bytechef.component.bitbucket.constant.BitbucketConstants.SLUG;
import static com.bytechef.component.bitbucket.constant.BitbucketConstants.VALUES;
import static com.bytechef.component.bitbucket.constant.BitbucketConstants.WORKSPACE;
import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
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
class BitbucketUtilsTest {
    private final Context mockedContext = mock(Context.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(WORKSPACE, "workspace"));
    private final Response mockedResponse = mock(Response.class);
    private final Map<String, Object> responseMap = Map.of(VALUES, List.of(
        Map.of(NAME, "name1", KEY, "key1"), Map.of(NAME, "name2", KEY, "key2")));
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testGetPaginationList() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        String mockedUrl = "mockedUrl";
        List<Map<String, Object>> result = BitbucketUtils.getPaginationList(mockedContext, mockedUrl);

        assertEquals(responseMap.get(VALUES), result);

        assertEquals(List.of(PAGE, "1"), stringArgumentCaptor.getAllValues());
    }

    @Test
    void testGetKeyOptions() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        List<Option<String>> options = BitbucketUtils.getKeyOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expectedOptions = List.of(
            option("name1", "key1"),
            option("name2", "key2"));

        assertEquals(expectedOptions, options);

        assertEquals(List.of(PAGE, "1"), stringArgumentCaptor.getAllValues());
    }

    @Test
    void testGetWorkspaceOptions() {
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        List<Option<String>> options = BitbucketUtils.getWorkspaceOptions(
            mockedParameters, mockedParameters, Map.of(), "", mockedContext);

        List<Option<String>> expectedOptions = List.of(
            option("name1", "name1"),
            option("name2", "name2"));

        assertEquals(expectedOptions, options);

        assertEquals(List.of(PAGE, "1"), stringArgumentCaptor.getAllValues());
    }
}

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

package com.bytechef.component.beamer.util;

import static com.bytechef.component.beamer.constant.BeamerConstants.FEATURE_REQUEST;
import static com.bytechef.component.beamer.constant.BeamerConstants.ID;
import static com.bytechef.component.beamer.constant.BeamerConstants.POST;
import static com.bytechef.component.beamer.constant.BeamerConstants.TITLE;
import static com.bytechef.component.beamer.util.BeamerUtils.GET_FEATURE_REQUESTS_CONTEXT_FUNCTION;
import static com.bytechef.component.beamer.util.BeamerUtils.GET_POSTS_CONTEXT_FUNCTION;
import static com.bytechef.component.definition.ComponentDsl.option;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
class BeamerUtilsTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Body.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Executor mockedExecutor = mock(Executor.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of());
    private final Response mockedResponse = mock(Response.class);

    @Test
    void getPostsOptions() {
        testGetOptions(POST);
    }

    @Test
    void getFeatureRequestsOptions() {
        testGetOptions(FEATURE_REQUEST);
    }

    @Test
    void getPostCategoryOptions() {
        List<Option<String>> expectedCategoryOptions = getCategoryOptions();

        List<Option<String>> categoryOptions = BeamerUtils.getPostCategoryOptions();

        assertEquals(expectedCategoryOptions, categoryOptions);
    }

    private void testGetOptions(String option) {
        List<Map<String, Object>> responseList = getResponseList(option);

        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseList);

        List<Option<String>> options = httpCall(option);

        List<Option<String>> expectedPostOptions = List.of(
            option(option + "1", "1"), option(option + "2", "2"));

        assertEquals(expectedPostOptions, options);
    }

    private List<Option<String>> httpCall(String option) {
        List<Option<String>> options = new ArrayList<>();

        if (option.equals(FEATURE_REQUEST)) {
            options = BeamerUtils.getFeatureRequestsOptions(
                mockedParameters, mockedParameters, Map.of(), anyString(), mockedActionContext);

            verify(mockedActionContext, times(1)).http(GET_FEATURE_REQUESTS_CONTEXT_FUNCTION);
        }

        if (option.equals(POST)) {
            options = BeamerUtils.getPostsOptions(
                mockedParameters, mockedParameters, Map.of(), anyString(), mockedActionContext);

            verify(mockedActionContext, times(1)).http(GET_POSTS_CONTEXT_FUNCTION);
        }

        return options;
    }

    private List<Map<String, Object>> getResponseList(String title) {
        Map<String, Object> values1 = new HashMap<>();
        values1.put(TITLE, title + "1");

        Map<String, Object> post1 = new HashMap<>();
        post1.put("translations", List.of(values1));
        post1.put(ID, "1");

        Map<String, Object> values2 = new HashMap<>();
        values2.put(TITLE, title + "2");

        Map<String, Object> post2 = new HashMap<>();
        post2.put("translations", List.of(values2));
        post2.put(ID, "2");

        return List.of(post1, post2);
    }

    private List<Option<String>> getCategoryOptions() {
        return List.of(
            option("New", "new"),
            option("Improvement", "improvement"),
            option("Fix", "fix"),
            option("Coming soon", "comingsoon"),
            option("Announcement", "announcement"),
            option("Other", "other"));
    }
}

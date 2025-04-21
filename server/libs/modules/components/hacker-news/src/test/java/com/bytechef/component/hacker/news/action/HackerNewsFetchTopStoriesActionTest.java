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

package com.bytechef.component.hacker.news.action;

import static com.bytechef.component.hacker.news.constant.HackerNewsConstants.NUMBER_OF_STORIES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Marija Horvat
 */
class HackerNewsFetchTopStoriesActionTest {

    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(NUMBER_OF_STORIES, 2));
    private final Http.Response mockedTopStoriesResponse = mock(Http.Response.class);
    private final Http.Response mockedItem1Response = mock(Http.Response.class);
    private final Http.Response mockedItem2Response = mock(Http.Response.class);

    @Test
    void testPerform() {
        List<String> topStoryIds = List.of("1", "2");
        Map<String, Object> item1 = Map.of("id", "1", "title", "test1");
        Map<String, Object> item2 = Map.of("id", "2", "title", "test2");

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedTopStoriesResponse, mockedItem1Response, mockedItem2Response);
        when(mockedTopStoriesResponse.getBody(any(TypeReference.class)))
            .thenReturn(topStoryIds);
        when(mockedItem1Response.getBody())
            .thenReturn(item1);
        when(mockedItem2Response.getBody())
            .thenReturn(item2);

        Object result = HackerNewsFetchTopStoriesAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(List.of(item1, item2), result);
    }
}

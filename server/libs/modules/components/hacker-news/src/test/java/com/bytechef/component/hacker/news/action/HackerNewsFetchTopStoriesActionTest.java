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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import com.bytechef.component.definition.Context.Http.ResponseType;
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
 * @author Marija Horvat
 */
@ExtendWith(MockContextSetupExtension.class)
class HackerNewsFetchTopStoriesActionTest {

    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(NUMBER_OF_STORIES, 2));
    private final Http.Response mockedTopStoriesResponse = mock(Http.Response.class);
    private final Http.Response mockedItem1Response = mock(Http.Response.class);
    private final Http.Response mockedItem2Response = mock(Http.Response.class);

    @Test
    void testPerform(
        Context mockedContext, Http.Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Http.Executor>> httpFunctionArgumentCaptor,
        ArgumentCaptor<ConfigurationBuilder> configurationBuilderArgumentCaptor) {

        List<String> topStoryIds = List.of("1", "2");
        Map<String, Object> item1 = Map.of("id", "1", "title", "test1");
        Map<String, Object> item2 = Map.of("id", "2", "title", "test2");

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedTopStoriesResponse, mockedItem1Response, mockedItem2Response);
        when(mockedTopStoriesResponse.getBody(any(TypeReference.class)))
            .thenReturn(topStoryIds);
        when(mockedItem1Response.getBody())
            .thenReturn(item1);
        when(mockedItem2Response.getBody())
            .thenReturn(item2);

        Object result = HackerNewsFetchTopStoriesAction.perform(mockedParameters, null, mockedContext);

        assertEquals(List.of(item1, item2), result);

        ContextFunction<Http, Http.Executor> capturedFunction = httpFunctionArgumentCaptor.getValue();

        assertNotNull(capturedFunction);

        ConfigurationBuilder configurationBuilder = configurationBuilderArgumentCaptor.getValue();
        Http.Configuration configuration = configurationBuilder.build();

        ResponseType responseType = configuration.getResponseType();
        assertEquals(ResponseType.Type.JSON, responseType.getType());
        assertEquals(
            List.of(
                "https://hacker-news.firebaseio.com/v0/topstories.json",
                "https://hacker-news.firebaseio.com/v0/item/1.json",
                "https://hacker-news.firebaseio.com/v0/item/2.json"),
            stringArgumentCaptor.getAllValues());
    }
}

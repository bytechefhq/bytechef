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

package com.bytechef.component.linkedin.trigger;

import static com.bytechef.component.linkedin.constant.LinkedInConstants.URN;
import static com.bytechef.component.linkedin.trigger.LinkedInNewCommentTrigger.LAST_TIME_CHECKED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Context.Http.Response;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class LinkedInNewCommentTriggerTest {

    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = ArgumentCaptor.forClass(Object[].class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(URN, "123"));
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPollEmitsEachNewCommentOnce(
        TriggerContext mockedTriggerContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        LocalDateTime startDateTime = LocalDateTime.of(2026, 1, 1, 10, 0, 0);
        LocalDateTime firstPollDateTime = LocalDateTime.of(2026, 1, 1, 11, 0, 0);
        LocalDateTime secondPollDateTime = LocalDateTime.of(2026, 1, 1, 12, 0, 0);

        long startEpochMilli = toEpochMilli(startDateTime);

        Map<String, Object> oldComment = Map.of(
            "id", "1", "created", Map.of("time", startEpochMilli - 1000));
        Map<String, Object> newComment = Map.of(
            "id", "2", "created", Map.of("time", startEpochMilli + 1000));

        Map<String, Object> postsBody = Map.of(
            "elements", List.of(Map.of("id", "urn:li:share:1"), Map.of("id", "urn:li:ugcPost:2")));
        Map<String, Object> firstPostCommentsBody = Map.of("elements", List.of(oldComment, newComment));
        Map<String, Object> secondPostCommentsBody = Map.of("elements", List.of());

        Parameters firstClosureParameters = MockParametersFactory.create(Map.of(LAST_TIME_CHECKED, startDateTime));

        try (MockedStatic<LocalDateTime> localDateTimeMockedStatic = mockStatic(
            LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {

            localDateTimeMockedStatic.when(() -> LocalDateTime.now(any(ZoneId.class)))
                .thenReturn(firstPollDateTime, secondPollDateTime);

            when(mockedHttp.get(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedResponse.getBody(any(TypeReference.class)))
                .thenReturn(
                    postsBody, firstPostCommentsBody, secondPostCommentsBody,
                    postsBody, firstPostCommentsBody, secondPostCommentsBody);

            PollOutput firstPollOutput = LinkedInNewCommentTrigger.poll(
                mockedParameters, null, firstClosureParameters, mockedTriggerContext);

            assertEquals(
                new PollOutput(List.of(newComment), Map.of(LAST_TIME_CHECKED, firstPollDateTime), false),
                firstPollOutput);

            Parameters secondClosureParameters = MockParametersFactory.create(
                new HashMap<>(firstPollOutput.closureParameters()));

            PollOutput secondPollOutput = LinkedInNewCommentTrigger.poll(
                mockedParameters, null, secondClosureParameters, mockedTriggerContext);

            assertEquals(
                new PollOutput(List.of(), Map.of(LAST_TIME_CHECKED, secondPollDateTime), false),
                secondPollOutput);
        }

        assertEquals(
            List.of(
                "/rest/posts",
                "/rest/socialActions/urn%3Ali%3Ashare%3A1/comments",
                "/rest/socialActions/urn%3Ali%3AugcPost%3A2/comments",
                "/rest/posts",
                "/rest/socialActions/urn%3Ali%3Ashare%3A1/comments",
                "/rest/socialActions/urn%3Ali%3AugcPost%3A2/comments"),
            stringArgumentCaptor.getAllValues());

        List<Object[]> queryParameters = new ArrayList<>(objectsArgumentCaptor.getAllValues());

        assertArrayEquals(
            new Object[] {
                "q", "author", "author", "urn:li:organization:123", "sortBy", "CREATED", "count", 10
            },
            queryParameters.getFirst());
        assertArrayEquals(new Object[] {
            "start", 0, "count", 100
        }, queryParameters.get(1));
    }

    @Test
    void testPollRequestsNextCommentsPageWhenPageIsFull(
        TriggerContext mockedTriggerContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        LocalDateTime startDateTime = LocalDateTime.of(2026, 1, 1, 10, 0, 0);

        long startEpochMilli = toEpochMilli(startDateTime);

        List<Map<String, Object>> fullPage = new ArrayList<>();

        for (int index = 0; index < LinkedInNewCommentTrigger.COMMENTS_PAGE_SIZE; index++) {
            fullPage.add(Map.of("id", "page1-" + index, "created", Map.of("time", startEpochMilli - 1000)));
        }

        Map<String, Object> lastComment = Map.of("id", "page2-0", "created", Map.of("time", startEpochMilli + 1000));

        Parameters closureParameters = MockParametersFactory.create(Map.of(LAST_TIME_CHECKED, startDateTime));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of("elements", List.of(Map.of("id", "urn:li:share:1"))),
                Map.of("elements", fullPage),
                Map.of("elements", List.of(lastComment)));

        PollOutput pollOutput = LinkedInNewCommentTrigger.poll(
            mockedParameters, null, closureParameters, mockedTriggerContext);

        assertEquals(List.of(lastComment), pollOutput.records());

        List<Object[]> queryParameters = objectsArgumentCaptor.getAllValues();

        assertEquals(3, queryParameters.size());
        assertArrayEquals(new Object[] {
            "start", 100, "count", 100
        }, queryParameters.get(2));
    }

    private static long toEpochMilli(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();
    }
}

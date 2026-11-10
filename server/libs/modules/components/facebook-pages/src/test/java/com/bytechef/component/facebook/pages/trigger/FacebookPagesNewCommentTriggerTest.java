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

package com.bytechef.component.facebook.pages.trigger;

import static com.bytechef.component.facebook.pages.constant.FacebookPagesConstants.PAGE_ID;
import static com.bytechef.component.facebook.pages.trigger.FacebookPagesNewCommentTrigger.LAST_TIME_CHECKED;
import static com.bytechef.component.facebook.pages.trigger.FacebookPagesNewCommentTrigger.POST_ID;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
class FacebookPagesNewCommentTriggerTest {

    private static final Map<String, Object> OLD_COMMENT = createComment("c0", "2026-11-10T08:59:59+0000");
    private static final Map<String, Object> FIRST_COMMENT = createComment("c1", "2026-11-10T09:30:00+0000");
    private static final Map<String, Object> SECOND_COMMENT = createComment("c2", "2026-11-10T10:30:00+0000");

    private final Parameters inputParameters = MockParametersFactory.create(Map.of(PAGE_ID, "123"));
    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = ArgumentCaptor.forClass(Object[].class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPollEmitsEachCommentOnce(
        TriggerContext mockedTriggerContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        LocalDateTime startTime = LocalDateTime.of(2026, 11, 10, 9, 0, 0);
        LocalDateTime firstPollTime = LocalDateTime.of(2026, 11, 10, 10, 0, 0);
        LocalDateTime secondPollTime = LocalDateTime.of(2026, 11, 10, 11, 0, 0);

        Map<String, Object> postsBody = Map.of("data", List.of(Map.of("id", "123_456")));

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(any(String.class), any(String.class)))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.header(any(String.class), any(String.class)))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of("access_token", "pageToken"), postsBody,
                Map.of("data", List.of(FIRST_COMMENT, OLD_COMMENT)),
                Map.of("access_token", "pageToken"), postsBody,
                Map.of("data", List.of(SECOND_COMMENT, FIRST_COMMENT, OLD_COMMENT)));

        try (MockedStatic<LocalDateTime> localDateTimeMockedStatic = mockStatic(
            LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {

            localDateTimeMockedStatic.when(() -> LocalDateTime.now(any(ZoneId.class)))
                .thenReturn(firstPollTime, secondPollTime);

            PollOutput firstPollOutput = FacebookPagesNewCommentTrigger.poll(
                inputParameters, null, MockParametersFactory.create(Map.of(LAST_TIME_CHECKED, startTime)),
                mockedTriggerContext);

            assertEquals(List.of(withPostId(FIRST_COMMENT)), firstPollOutput.records());
            assertEquals(Map.of(LAST_TIME_CHECKED, firstPollTime), firstPollOutput.closureParameters());
            assertFalse(firstPollOutput.pollImmediately());

            Map<String, Object> firstClosureParameters = new HashMap<>(firstPollOutput.closureParameters());

            PollOutput secondPollOutput = FacebookPagesNewCommentTrigger.poll(
                inputParameters, null, MockParametersFactory.create(firstClosureParameters), mockedTriggerContext);

            assertEquals(List.of(withPostId(SECOND_COMMENT)), secondPollOutput.records());
            assertEquals(Map.of(LAST_TIME_CHECKED, secondPollTime), secondPollOutput.closureParameters());
        }

        assertEquals(
            List.of("/123", "/123/posts", "/123_456/comments", "/123", "/123/posts", "/123_456/comments"),
            stringArgumentCaptor.getAllValues());

        List<Object[]> queryParameters = objectsArgumentCaptor.getAllValues();

        assertArrayEquals(new Object[] {
            "fields", "id", "limit", 10
        }, queryParameters.get(0));
        assertArrayEquals(
            new Object[] {
                "fields", "id,message,from,created_time", "order", "reverse_chronological", "limit", 100
            },
            queryParameters.get(1));
    }

    @Test
    void testPollWithoutClosureStartsFromNow(
        TriggerContext mockedTriggerContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        LocalDateTime pollTime = LocalDateTime.of(2026, 11, 10, 10, 0, 0);

        when(mockedHttp.get(any(String.class)))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(any(String.class), any(String.class)))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(any(Object[].class)))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.header(any(String.class), any(String.class)))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                Map.of("access_token", "pageToken"), Map.of("data", List.of(Map.of("id", "123_456"))),
                Map.of("data", List.of(FIRST_COMMENT)));

        try (MockedStatic<LocalDateTime> localDateTimeMockedStatic = mockStatic(
            LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {

            localDateTimeMockedStatic.when(() -> LocalDateTime.now(any(ZoneId.class)))
                .thenReturn(pollTime);

            PollOutput pollOutput = FacebookPagesNewCommentTrigger.poll(
                inputParameters, null, MockParametersFactory.create(Map.of()), mockedTriggerContext);

            assertEquals(List.of(), pollOutput.records());
            assertEquals(Map.of(LAST_TIME_CHECKED, pollTime), pollOutput.closureParameters());
        }
    }

    private static Map<String, Object> createComment(String id, String createdTime) {
        return Map.of(
            "id", id, "message", "Comment " + id, "created_time", createdTime,
            "from", Map.of("id", "u1", "name", "User"));
    }

    private static Map<String, Object> withPostId(Map<String, Object> comment) {
        Map<String, Object> commentWithPostId = new HashMap<>(comment);

        commentWithPostId.put(POST_ID, "123_456");

        return commentWithPostId;
    }
}

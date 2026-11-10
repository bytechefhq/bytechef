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

package com.bytechef.component.instagram.trigger;

import static com.bytechef.component.instagram.trigger.InstagramNewCommentTrigger.LAST_TIME_CHECKED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockContextSetupExtension.class)
class InstagramNewCommentTriggerTest {

    private static final Map<String, Object> MEDIA_RESPONSE = Map.of(
        "data", List.of(Map.of("id", "media-1"), Map.of("id", "media-2")));
    private static final Map<String, Object> NEW_COMMENT = Map.of(
        "id", "comment-new", "text", "New", "timestamp", "2025-06-01T12:00:00+0000");
    private static final Map<String, Object> OLD_COMMENT = Map.of(
        "id", "comment-old", "text", "Old", "timestamp", "2024-12-31T23:59:59+0000");
    private static final Map<String, Object> USER_RESPONSE = Map.of("user_id", "user-1");

    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<Object[]> objectsArgumentCaptor = forClass(Object[].class);

    @Test
    void testPollEmitsCommentsAfterLastPollOnce(
        TriggerContext mockedTriggerContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        Map<String, Object> futureComment = Map.of(
            "id", "comment-future", "text", "Later", "timestamp", "2099-01-01T00:00:00+0000");

        when(mockedHttp.get(stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(anyString(), anyString()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(objectsArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                USER_RESPONSE, MEDIA_RESPONSE, Map.of("data", List.of(NEW_COMMENT, OLD_COMMENT)),
                Map.of("data", List.of()),
                USER_RESPONSE, MEDIA_RESPONSE, Map.of("data", List.of(NEW_COMMENT, OLD_COMMENT)),
                Map.of("data", List.of(futureComment)));

        Parameters firstClosureParameters = MockParametersFactory.create(
            Map.of(LAST_TIME_CHECKED, LocalDateTime.of(2025, 1, 1, 0, 0, 0)));

        PollOutput firstPollOutput = InstagramNewCommentTrigger.poll(
            null, null, firstClosureParameters, mockedTriggerContext);

        assertEquals(List.of(withMediaId(NEW_COMMENT, "media-1")), firstPollOutput.records());
        assertFalse(firstPollOutput.pollImmediately());
        assertTrue(
            firstPollOutput.closureParameters()
                .get(LAST_TIME_CHECKED) instanceof LocalDateTime);

        Parameters secondClosureParameters = MockParametersFactory.create(
            new LinkedHashMap<String, Object>(firstPollOutput.closureParameters()));

        PollOutput secondPollOutput = InstagramNewCommentTrigger.poll(
            null, null, secondClosureParameters, mockedTriggerContext);

        assertEquals(List.of(withMediaId(futureComment, "media-2")), secondPollOutput.records());
        assertEquals(
            List.of(
                "/me", "/user-1/media", "/media-1/comments", "/media-2/comments",
                "/me", "/user-1/media", "/media-1/comments", "/media-2/comments"),
            stringArgumentCaptor.getAllValues());

        Object[] expectedMediaQueryParameters = {
            "fields", "id,timestamp", "limit", 10
        };

        assertArrayEquals(expectedMediaQueryParameters, objectsArgumentCaptor.getValue());
    }

    @Test
    void testPollWithoutLastPollTimeSkipsExistingComments(
        TriggerContext mockedTriggerContext, Response mockedResponse, Executor mockedExecutor, Http mockedHttp) {

        when(mockedHttp.get(anyString()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(anyString(), anyString()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameters(any(Object[].class)))
            .thenReturn(mockedExecutor);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(
                USER_RESPONSE, Map.of("data", List.of(Map.of("id", "media-1"))),
                Map.of("data", List.of(NEW_COMMENT, OLD_COMMENT)));

        PollOutput pollOutput = InstagramNewCommentTrigger.poll(
            null, null, MockParametersFactory.create(Map.of()), mockedTriggerContext);

        assertTrue(
            pollOutput.records()
                .isEmpty());
    }

    private static Map<String, Object> withMediaId(Map<String, Object> comment, String mediaId) {
        Map<String, Object> expected = new LinkedHashMap<>(comment);

        expected.put("media_id", mediaId);

        return expected;
    }
}

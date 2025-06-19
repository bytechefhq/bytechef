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

package com.bytechef.component.wordpress.trigger;

import static com.bytechef.component.wordpress.constant.WordpressConstants.LAST_TIME_CHECKED;
import static com.bytechef.component.wordpress.constant.WordpressConstants.WEBSITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Nikolina Spehar
 */
class WordPressNewPostTriggerTest {

    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final LocalDateTime mockedLocalDateTime = LocalDateTime.of(2025, 6, 17, 9, 0);
    private final Parameters mockedParameters =
        MockParametersFactory.create(Map.of(WEBSITE, "mockWebsite", LAST_TIME_CHECKED, mockedLocalDateTime));
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPoll() {
        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.queryParameter(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(Map.of("id", "1")));

        PollOutput pollOutput = WordPressNewPostTrigger.poll(
            mockedParameters, mockedParameters, mockedParameters, mockedTriggerContext);

        assertEquals(List.of(Map.of("id", "1")), pollOutput.records());
        assertFalse(pollOutput.pollImmediately());

        List<String> expectedQueryArguments = List.of(
            "after", mockedLocalDateTime.format(DateTimeFormatter.ISO_DATE_TIME));

        assertEquals(expectedQueryArguments, stringArgumentCaptor.getAllValues());
    }
}

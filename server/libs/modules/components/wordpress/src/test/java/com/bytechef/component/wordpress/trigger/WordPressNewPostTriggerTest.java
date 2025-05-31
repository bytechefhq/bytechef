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

import static com.bytechef.component.wordpress.constant.WordpressConstants.ALL_POSTS;
import static com.bytechef.component.wordpress.constant.WordpressConstants.WEBSITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Nikolina Spehar
 */
class WordPressNewPostTriggerTest {

    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = MockParametersFactory.create(Map.of(WEBSITE, "mockWebsite"));
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);

    @Test
    void poll() {
        when(mockedTriggerContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(List.of(Map.of("id", "1")));

        PollOutput pollOutput = WordPressNewPostTrigger.poll(
            mockedParameters, mockedParameters, mockedParameters, mockedTriggerContext);

        PollOutput expectedPollOutput = new PollOutput(
            List.of(Map.of("id", "1")), Map.of(ALL_POSTS, List.of("1")), false);

        assertEquals(expectedPollOutput, pollOutput);
    }
}

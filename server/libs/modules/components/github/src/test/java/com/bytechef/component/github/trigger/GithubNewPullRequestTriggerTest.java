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

package com.bytechef.component.github.trigger;

import static com.bytechef.component.github.constant.GithubConstants.ID;
import static com.bytechef.component.github.constant.GithubConstants.PULL_REQUESTS;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.github.util.GithubUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GithubNewPullRequestTriggerTest {

    private final ArgumentCaptor<Boolean> booleanArgumentCaptor = ArgumentCaptor.forClass(Boolean.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);

    @Test
    void testPoll() {
        Parameters mockedInputParameters = MockParametersFactory.create(Map.of(REPOSITORY, "testRepo"));
        Parameters mockedClosureParameters = MockParametersFactory.create(Map.of(PULL_REQUESTS, List.of(1L)));

        List<Map<String, ?>> responseList = List.of(Map.of(ID, 2L));

        try (MockedStatic<GithubUtils> githubUtilsMockedStatic = mockStatic(GithubUtils.class)) {
            githubUtilsMockedStatic.when(() -> GithubUtils.getOwnerName(contextArgumentCaptor.capture()))
                .thenReturn("owner");
            githubUtilsMockedStatic.when(() -> GithubUtils.getItems(
                contextArgumentCaptor.capture(), stringArgumentCaptor.capture(), booleanArgumentCaptor.capture(),
                stringArgumentCaptor.capture(), stringArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                stringArgumentCaptor.capture()))
                .thenReturn(responseList);

            when(mockedTriggerContext.isEditorEnvironment())
                .thenReturn(false);

            PollOutput pollOutput = GithubNewPullRequestTrigger.poll(
                mockedInputParameters, null, mockedClosureParameters, mockedTriggerContext);

            assertEquals(
                new PollOutput(List.of(Map.of(ID, 2L)), Map.of(PULL_REQUESTS, List.of(2L)), false),
                pollOutput);
            assertEquals(
                List.of("/repos/owner/testRepo/pulls", "sort", "created", "direction", "desc"),
                stringArgumentCaptor.getAllValues());
            assertEquals(List.of(mockedTriggerContext, mockedTriggerContext), contextArgumentCaptor.getAllValues());
            assertEquals(false, booleanArgumentCaptor.getValue());
        }
    }
}

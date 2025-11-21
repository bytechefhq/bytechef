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

import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.trigger.GithubNewIssueTrigger.LAST_TIME_CHECKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.github.util.GithubUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Monika Kušter
 */
class GithubNewIssueTriggerTest {

    private final ArgumentCaptor<Boolean> booleanArgumentCaptor = ArgumentCaptor.forClass(Boolean.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);

    @Test
    void testPoll() {
        LocalDateTime startDate = LocalDateTime.of(2000, 1, 1, 1, 1, 1);
        LocalDateTime endDate = LocalDateTime.of(2024, 1, 2, 0, 0, 0);

        Parameters mockedInputParameters = MockParametersFactory.create(Map.of(REPOSITORY, "testRepo"));
        Parameters mockedClosureParameters = MockParametersFactory.create(Map.of(LAST_TIME_CHECKED, startDate));

        try (MockedStatic<GithubUtils> githubUtilsMockedStatic = mockStatic(GithubUtils.class);
            MockedStatic<LocalDateTime> localDateTimeMockedStatic = mockStatic(
                LocalDateTime.class, Mockito.CALLS_REAL_METHODS)) {

            localDateTimeMockedStatic.when(() -> LocalDateTime.now(any(ZoneId.class)))
                .thenReturn(endDate);
            githubUtilsMockedStatic.when(() -> GithubUtils.getOwnerName(contextArgumentCaptor.capture()))
                .thenReturn("owner");
            githubUtilsMockedStatic.when(() -> GithubUtils.getItems(
                contextArgumentCaptor.capture(), stringArgumentCaptor.capture(), booleanArgumentCaptor.capture(),
                stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(List.of());

            when(mockedTriggerContext.isEditorEnvironment())
                .thenReturn(false);

            PollOutput pollOutput = GithubNewIssueTrigger.poll(
                mockedInputParameters, null, mockedClosureParameters, mockedTriggerContext);

            assertEquals(new PollOutput(List.of(), Map.of(LAST_TIME_CHECKED, endDate), false), pollOutput);
            assertEquals(List.of("/repos/owner/testRepo/issues", "since", "2000-01-01T01:01:01Z"),
                stringArgumentCaptor.getAllValues());
            assertEquals(List.of(mockedTriggerContext, mockedTriggerContext), contextArgumentCaptor.getAllValues());
            assertEquals(false, booleanArgumentCaptor.getValue());
        }
    }
}

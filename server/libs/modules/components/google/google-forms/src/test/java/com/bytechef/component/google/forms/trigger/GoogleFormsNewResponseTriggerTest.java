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

package com.bytechef.component.google.forms.trigger;

import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.FORM_ID;
import static com.bytechef.component.google.forms.trigger.GoogleFormsNewResponseTrigger.LAST_TIME_CHECKED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.google.forms.util.GoogleFormsUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Monika Kušter
 */
class GoogleFormsNewResponseTriggerTest {

    private final ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPoll() {
        Instant startDate = Instant.parse("2000-01-01T01:01:01Z");
        Instant endDate = Instant.parse("2024-01-02T00:00:00Z");

        Parameters mockedClosureParameters = MockParametersFactory.create(Map.of(LAST_TIME_CHECKED, startDate));
        Parameters mockedInputParameters = MockParametersFactory.create(Map.of(FORM_ID, "123"));

        List<Map<String, String>> responseList = List.of(Map.of("id", "abc"));

        try (MockedStatic<GoogleFormsUtils> googleFormsUtilsMockedStatic = mockStatic(GoogleFormsUtils.class);
            MockedStatic<Instant> instantMockedStatic = mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS)) {

            instantMockedStatic.when(Instant::now)
                .thenReturn(endDate);
            googleFormsUtilsMockedStatic.when(() -> GoogleFormsUtils.getCustomResponses(
                contextArgumentCaptor.capture(), stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(responseList);
            when(mockedTriggerContext.isEditorEnvironment())
                .thenReturn(false);

            PollOutput pollOutput = GoogleFormsNewResponseTrigger.poll(
                mockedInputParameters, null, mockedClosureParameters, mockedTriggerContext);

            assertEquals(new PollOutput(responseList, Map.of(LAST_TIME_CHECKED, endDate), false), pollOutput);
            assertEquals(mockedTriggerContext, contextArgumentCaptor.getValue());
            assertEquals(List.of("123", "2000-01-01T01:01:01Z"), stringArgumentCaptor.getAllValues());
        }
    }
}

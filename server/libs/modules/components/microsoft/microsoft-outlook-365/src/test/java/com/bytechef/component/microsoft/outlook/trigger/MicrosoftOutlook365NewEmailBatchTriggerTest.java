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

package com.bytechef.component.microsoft.outlook.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class MicrosoftOutlook365NewEmailBatchTriggerTest {

    private final ArgumentCaptor<TriggerContext> contextArgumentCaptor = forClass(TriggerContext.class);
    private final TriggerContext mockedContext = mock(TriggerContext.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);
    private final PollOutput pollOutput = new PollOutput(List.of(), Map.of(), false);

    @Test
    void testPoll() {
        try (MockedStatic<MicrosoftOutlook365Utils> microsoftOutlook365UtilsMockedStatic =
            mockStatic(MicrosoftOutlook365Utils.class)) {

            microsoftOutlook365UtilsMockedStatic.when(() -> MicrosoftOutlook365Utils.getPollOutput(
                parametersArgumentCaptor.capture(),
                parametersArgumentCaptor.capture(),
                contextArgumentCaptor.capture()))
                .thenReturn(pollOutput);

            PollOutput result = MicrosoftOutlook365NewEmailBatchTrigger.poll(
                mockedParameters, mockedParameters, mockedParameters, mockedContext);

            assertEquals(pollOutput, result);
            assertEquals(List.of(mockedParameters, mockedParameters), parametersArgumentCaptor.getAllValues());
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
        }
    }
}

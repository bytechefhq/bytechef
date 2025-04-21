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

package com.bytechef.component.salesforce.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.salesforce.util.SalesforceUtils;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class SalesforceNewRecordTriggerTest {

    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final PollOutput mockedPollOutput = mock(PollOutput.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<TriggerContext> triggerContextArgumentCaptor =
        ArgumentCaptor.forClass(TriggerContext.class);

    @Test
    void testPoll() {
        try (MockedStatic<SalesforceUtils> salesforceUtilsMockedStatic = mockStatic(SalesforceUtils.class)) {
            salesforceUtilsMockedStatic
                .when(() -> SalesforceUtils.getPollOutput(
                    parametersArgumentCaptor.capture(), parametersArgumentCaptor.capture(),
                    triggerContextArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(mockedPollOutput);

            PollOutput pollOutput = SalesforceNewRecordTrigger.poll(
                mockedParameters, mockedParameters, mockedParameters, mockedTriggerContext);

            assertEquals(mockedPollOutput, pollOutput);
            assertEquals(List.of(mockedParameters, mockedParameters), parametersArgumentCaptor.getAllValues());
            assertEquals(mockedTriggerContext, triggerContextArgumentCaptor.getValue());
            assertEquals("CreatedDate", stringArgumentCaptor.getValue());
        }
    }
}

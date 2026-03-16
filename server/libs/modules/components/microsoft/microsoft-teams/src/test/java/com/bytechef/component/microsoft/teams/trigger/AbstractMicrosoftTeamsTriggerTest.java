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

package com.bytechef.component.microsoft.teams.trigger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.microsoft.commons.MicrosoftTriggerUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
public class AbstractMicrosoftTeamsTriggerTest {
    protected MockedStatic<MicrosoftTriggerUtils> microsoftTriggerUtilsMockedStatic;
    protected PollOutput mockedPollOutput = mock(PollOutput.class);
    protected TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    protected ArgumentCaptor<Parameters> parametersArgumentCaptor = ArgumentCaptor.forClass(Parameters.class);
    protected ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    protected ArgumentCaptor<TriggerContext> triggerContextArgumentCaptor = ArgumentCaptor.forClass(
        TriggerContext.class);

    @BeforeEach
    void beforeEach() {
        microsoftTriggerUtilsMockedStatic = mockStatic(MicrosoftTriggerUtils.class);
    }

    @AfterEach
    void afterEach() {
        microsoftTriggerUtilsMockedStatic.close();
    }
}

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

package com.bytechef.component.microsoft.share.point.trigger;

import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.PARENT_FOLDER;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.SITE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.microsoft.commons.MicrosoftTriggerUtils;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Nikolina Spehar
 */
class MicrosoftSharePointNewFileTriggerTest {

    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(SITE_ID, "siteId", PARENT_FOLDER, "parentFolder"));
    private final PollOutput mockedPollOutput = mock(PollOutput.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<TriggerContext> triggerContextArgumentCaptor = forClass(TriggerContext.class);

    @Test
    void testPoll() {
        try (MockedStatic<MicrosoftTriggerUtils> microsoftTriggerUtilsMockedStatic =
            mockStatic(MicrosoftTriggerUtils.class)) {

            microsoftTriggerUtilsMockedStatic.when(() -> MicrosoftTriggerUtils.poll(
                stringArgumentCaptor.capture(),
                stringArgumentCaptor.capture(),
                parametersArgumentCaptor.capture(),
                triggerContextArgumentCaptor.capture()))
                .thenReturn(mockedPollOutput);

            PollOutput result = MicrosoftSharePointNewFileTrigger.poll(
                mockedParameters, null, mockedParameters, mockedTriggerContext);

            assertEquals(mockedPollOutput, result);
            assertEquals(
                List.of("/sites/siteId/drive/items/parentFolder/children", "file"),
                stringArgumentCaptor.getAllValues());
            assertEquals(mockedParameters, parametersArgumentCaptor.getValue());
            assertEquals(mockedTriggerContext, triggerContextArgumentCaptor.getValue());
        }
    }
}

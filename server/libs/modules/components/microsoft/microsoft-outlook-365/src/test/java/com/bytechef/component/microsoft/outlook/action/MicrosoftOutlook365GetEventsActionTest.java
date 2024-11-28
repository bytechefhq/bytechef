/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.microsoft.outlook.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365CustomEventUtils;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365CustomEventUtils.CustomEvent;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class MicrosoftOutlook365GetEventsActionTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final CustomEvent mockedCustomEvent = mock(CustomEvent.class);
    private final Parameters mockedParameters = mock(Parameters.class);

    @Test
    void testPerform() {
        try (MockedStatic<MicrosoftOutlook365CustomEventUtils> microsoftOutlook365CustomEventUtilsMockedStatic =
            mockStatic(MicrosoftOutlook365CustomEventUtils.class)) {

            microsoftOutlook365CustomEventUtilsMockedStatic
                .when(() -> MicrosoftOutlook365CustomEventUtils.retrieveCustomEvents(mockedParameters,
                    mockedActionContext))
                .thenReturn(List.of(mockedCustomEvent));

            List<CustomEvent> result =
                MicrosoftOutlook365GetEventsAction.perform(mockedParameters, mockedParameters, mockedActionContext);

            assertEquals(List.of(mockedCustomEvent), result);
        }
    }
}

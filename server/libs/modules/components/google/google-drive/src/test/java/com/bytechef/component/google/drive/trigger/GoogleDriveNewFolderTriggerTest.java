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

package com.bytechef.component.google.drive.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.google.drive.util.GoogleDriveUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleDriveNewFolderTriggerTest {

    private final Parameters mockedParameters = MockParametersFactory.create(Map.of());
    private final PollOutput mockedPollPOutput = mock(PollOutput.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);

    @Test
    void testPool() {
        try (MockedStatic<GoogleDriveUtils> googleDriveUtilsMockedStatic = mockStatic(GoogleDriveUtils.class)) {
            googleDriveUtilsMockedStatic
                .when(() -> GoogleDriveUtils.getPollOutput(
                    mockedParameters, mockedParameters, mockedParameters, mockedTriggerContext, false))
                .thenReturn(mockedPollPOutput);

            PollOutput pollOutput = GoogleDriveNewFolderTrigger.poll(
                mockedParameters, mockedParameters, mockedParameters, mockedTriggerContext);

            assertEquals(mockedPollPOutput, pollOutput);
        }
    }
}

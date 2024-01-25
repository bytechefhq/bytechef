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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserRequestBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
public abstract class AbstractMicrosoftOutlook365ActionTest {

    protected MockedStatic<MicrosoftOutlook365Utils> microsoftOutlook365UtilsMockedStatic;
    protected ActionContext mockedContext = mock(ActionContext.class);
    @SuppressWarnings("rawtypes")

    protected GraphServiceClient mockedGraphServiceClient = mock(GraphServiceClient.class);
    protected Parameters mockedParameters = mock(Parameters.class);
    protected UserRequestBuilder mockedUserRequestBuilder = mock(UserRequestBuilder.class);

    @BeforeEach
    public void beforeEach() {
        microsoftOutlook365UtilsMockedStatic = mockStatic(MicrosoftOutlook365Utils.class);

        microsoftOutlook365UtilsMockedStatic
            .when(MicrosoftOutlook365Utils::getGraphServiceClient)
            .thenReturn(mockedGraphServiceClient);

        when(mockedGraphServiceClient.me())
            .thenReturn(mockedUserRequestBuilder);
    }

    @AfterEach
    public void afterEach() {
        microsoftOutlook365UtilsMockedStatic.close();
    }
}

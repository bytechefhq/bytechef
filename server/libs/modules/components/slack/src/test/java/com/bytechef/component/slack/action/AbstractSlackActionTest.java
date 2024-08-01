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

package com.bytechef.component.slack.action;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.slack.util.SlackUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedStatic;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
abstract class AbstractSlackActionTest {

    protected ActionContext mockedContext = mock(ActionContext.class);
    protected Object mockedObject = mock(Object.class);
    protected Parameters mockedParameters = mock(Parameters.class);
    protected MockedStatic<SlackUtils> shopifyUtilsMockedStatic;

    @BeforeEach
    public void beforeEach() {
        shopifyUtilsMockedStatic = mockStatic(SlackUtils.class);

        shopifyUtilsMockedStatic.when(
            () -> SlackUtils.sendMessage(mockedParameters, mockedContext))
            .thenReturn(mockedObject);

    }

    @AfterEach
    public void afterEach() {
        shopifyUtilsMockedStatic.close();
    }
}

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

package com.bytechef.component.google.mail.action;

import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.MINIMAL;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SIMPLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.google.mail.util.GoogleMailUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.google.api.services.gmail.model.Message;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleMailGetMailActionTest extends AbstractGoogleMailActionTest {

    private final Message mockedMessage = mock(Message.class);
    private final GoogleMailUtils.SimpleMessage mockedSimpleMessage = mock(GoogleMailUtils.SimpleMessage.class);

    @Test
    void testPerform() throws IOException {
        Parameters parameters = MockParametersFactory.create(Map.of(FORMAT, MINIMAL));

        try (MockedStatic<GoogleMailUtils> googleMailUtilsMockedStatic = mockStatic(GoogleMailUtils.class)) {
            googleMailUtilsMockedStatic.when(() -> GoogleMailUtils.getMessage(parameters, mockedGmail))
                .thenReturn(mockedMessage);

            Object result = GoogleMailGetMailAction.perform(parameters, parameters, mockedActionContext);

            assertEquals(mockedMessage, result);
        }
    }

    @Test
    void testPerformForSimpleFormat() throws IOException {
        Parameters parameters = MockParametersFactory.create(Map.of(FORMAT, SIMPLE));

        try (MockedStatic<GoogleMailUtils> googleMailUtilsMockedStatic = mockStatic(GoogleMailUtils.class)) {
            googleMailUtilsMockedStatic.when(() -> GoogleMailUtils.getMessage(parameters, mockedGmail))
                .thenReturn(mockedMessage);
            googleMailUtilsMockedStatic
                .when(() -> GoogleMailUtils.getSimpleMessage(mockedMessage, mockedActionContext, mockedGmail))
                .thenReturn(mockedSimpleMessage);

            Object result = GoogleMailGetMailAction.perform(parameters, parameters, mockedActionContext);

            assertEquals(mockedSimpleMessage, result);
        }
    }
}

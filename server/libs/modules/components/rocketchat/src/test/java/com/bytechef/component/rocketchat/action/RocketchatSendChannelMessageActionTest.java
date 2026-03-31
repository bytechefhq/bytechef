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

package com.bytechef.component.rocketchat.action;

import static com.bytechef.component.rocketchat.constant.RocketchatConstants.ROOM_ID;
import static com.bytechef.component.rocketchat.constant.RocketchatConstants.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.rocketchat.util.RocketchatUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Marija Horvat
 */
@ExtendWith(MockContextSetupExtension.class)
class RocketchatSendChannelMessageActionTest {

    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(ROOM_ID, "test", TEXT, "This is test."));

    @Test
    void testPerform(Context mockedContext) {
        try (MockedStatic<RocketchatUtils> rocketchatUtilsMockedStatic = mockStatic(RocketchatUtils.class)) {
            rocketchatUtilsMockedStatic
                .when(() -> RocketchatUtils.sendMessage(
                    stringArgumentCaptor.capture(), stringArgumentCaptor.capture(), contextArgumentCaptor.capture()))
                .thenReturn(mockedObject);

            Object result = RocketchatSendChannelMessageAction.perform(mockedParameters, null, mockedContext);

            assertEquals(mockedObject, result);
            assertEquals(List.of("test", "This is test."), stringArgumentCaptor.getAllValues());
        }
    }
}

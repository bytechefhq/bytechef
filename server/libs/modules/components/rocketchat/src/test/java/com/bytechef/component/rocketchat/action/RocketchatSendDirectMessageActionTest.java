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
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.rocketchat.util.RocketchatUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Marija Horvat
 */
class RocketchatSendDirectMessageActionTest {

    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    private final Context mockedContext = mock(Context.class);
    private final Object mockedObject = mock(Object.class);
    private final Parameters mockedParameters =
        MockParametersFactory.create(Map.of(ROOM_ID, "test", TEXT, "This is test."));

    @Test
    void testPerform() {
        try (MockedStatic<RocketchatUtils> rocketchatUtilsMockedStatic = Mockito.mockStatic(RocketchatUtils.class)) {
            rocketchatUtilsMockedStatic
                .when(() -> RocketchatUtils.sendMessage(
                    stringArgumentCaptor.capture(), stringArgumentCaptor.capture(), contextArgumentCaptor.capture()))
                .thenReturn(mockedObject);

            Object result =
                RocketchatSendDirectMessageAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(mockedObject, result);
            assertEquals(List.of("@test", "This is test."), stringArgumentCaptor.getAllValues());
        }
    }
}

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

package com.bytechef.component.slack.action;

import static com.bytechef.component.slack.constant.SlackConstants.CHANNEL;
import static com.bytechef.component.slack.constant.SlackConstants.POST_AT;
import static com.bytechef.component.slack.constant.SlackConstants.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
class SlackSendDirectMessageActionTest extends AbstractSlackActionTest {

    @Test
    void testPerform() {
        Object result = SlackSendDirectMessageAction.perform(mockedParameters, mockedParameters, mockedActionContext);

        assertEquals(mockedObject, result);
        assertEquals(List.of("abc", "efg"), stringArgumentCaptor.getAllValues());
        assertNull(listArgumentCaptor.getValue());
        assertEquals(mockedActionContext, actionContextArgumentCaptor.getValue());
    }

    @Test
    void testPerformScheduledMessage() {

        LocalDateTime schedule = LocalDateTime.of(2000, 1, 1, 1, 1, 1);
        Parameters scheduledMockedParameters =
            MockParametersFactory.create(Map.of(CHANNEL, "abc", TEXT, "efg", POST_AT, schedule));

        Object result = SlackSendDirectMessageAction.perform(scheduledMockedParameters, scheduledMockedParameters,
            mockedActionContext);

        assertEquals(mockedObject, result);

        assertEquals(schedule, localDateTimeArgumentCaptor.getValue());
        assertEquals(List.of("abc", "efg"), stringArgumentCaptor.getAllValues());
        assertNull(listArgumentCaptor.getValue());
        assertEquals(mockedActionContext, actionContextArgumentCaptor.getValue());
    }
}

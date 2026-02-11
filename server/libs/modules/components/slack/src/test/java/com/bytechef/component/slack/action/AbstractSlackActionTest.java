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
import static com.bytechef.component.slack.constant.SlackConstants.TEXT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.slack.util.SlackUtils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
abstract class AbstractSlackActionTest {

    protected ActionContext mockedActionContext = mock(ActionContext.class);
    protected Object mockedObject = mock(Object.class);
    protected Parameters mockedParameters = MockParametersFactory.create(Map.of(CHANNEL, "abc", TEXT, "efg"));
    protected ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    protected ArgumentCaptor<ActionContext> actionContextArgumentCaptor = ArgumentCaptor.forClass(ActionContext.class);
    protected ArgumentCaptor<List> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
    protected ArgumentCaptor<LocalDateTime> localDateTimeArgumentCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
    protected MockedStatic<SlackUtils> slackUtilsMockedStatic;

    @BeforeEach
    void beforeEach() {
        slackUtilsMockedStatic = mockStatic(SlackUtils.class);

        slackUtilsMockedStatic.when(
            () -> SlackUtils.sendMessage(stringArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                listArgumentCaptor.capture(), actionContextArgumentCaptor.capture()))
            .thenReturn(mockedObject);

        slackUtilsMockedStatic.when(
            () -> SlackUtils.scheduleMessage(stringArgumentCaptor.capture(), stringArgumentCaptor.capture(),
                localDateTimeArgumentCaptor.capture(),
                listArgumentCaptor.capture(), actionContextArgumentCaptor.capture()))
            .thenReturn(mockedObject);
    }

    @AfterEach
    void afterEach() {
        slackUtilsMockedStatic.close();
    }
}

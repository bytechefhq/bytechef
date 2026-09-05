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

package com.bytechef.component.logger.action;

import static com.bytechef.component.logger.constant.LoggerConstants.TEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.ContextConsumer;
import com.bytechef.component.definition.Context.Log;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class LoggerActionsTest {

    private final List<String> messages = new ArrayList<>();
    private final Log log = mock(Log.class);
    private final ActionContext actionContext = mock(ActionContext.class);
    private final Parameters inputParameters = mock(Parameters.class);
    private final Parameters connectionParameters = mock(Parameters.class);

    LoggerActionsTest() {
        doAnswer(invocation -> {
            ContextConsumer<Log> logConsumer = invocation.getArgument(0);

            logConsumer.accept(log);

            return null;
        }).when(actionContext)
            .log(any());

        doAnswer(invocation -> messages.add(invocation.getArgument(0))).when(log)
            .debug(any(String.class));
        doAnswer(invocation -> messages.add(invocation.getArgument(0))).when(log)
            .info(any(String.class));
        doAnswer(invocation -> messages.add(invocation.getArgument(0))).when(log)
            .warn(any(String.class));
        doAnswer(invocation -> messages.add(invocation.getArgument(0))).when(log)
            .error(any(String.class));
    }

    @Test
    void testPerformDebugLogsEmptyMessageWhenTextIsMissing() {
        when(inputParameters.get(TEXT)).thenReturn(null);

        LoggerDebugAction.perform(inputParameters, connectionParameters, actionContext);

        assertEquals(List.of(""), messages);
    }

    @Test
    void testPerformInfoLogsEmptyMessageWhenTextIsMissing() {
        when(inputParameters.get(TEXT)).thenReturn(null);

        LoggerInfoAction.perform(inputParameters, connectionParameters, actionContext);

        assertEquals(List.of(""), messages);
    }

    @Test
    void testPerformWarnLogsEmptyMessageWhenTextIsMissing() {
        when(inputParameters.get(TEXT)).thenReturn(null);

        LoggerWarnAction.perform(inputParameters, connectionParameters, actionContext);

        assertEquals(List.of(""), messages);
    }

    @Test
    void testPerformErrorLogsEmptyMessageWhenTextIsMissing() {
        when(inputParameters.get(TEXT)).thenReturn(null);

        LoggerErrorAction.perform(inputParameters, connectionParameters, actionContext);

        assertEquals(List.of(""), messages);
    }

    @Test
    void testPerformDebugLogsTheGivenText() {
        when(inputParameters.get(TEXT)).thenReturn("Hello");

        LoggerDebugAction.perform(inputParameters, connectionParameters, actionContext);

        assertEquals(List.of("Hello"), messages);
    }
}

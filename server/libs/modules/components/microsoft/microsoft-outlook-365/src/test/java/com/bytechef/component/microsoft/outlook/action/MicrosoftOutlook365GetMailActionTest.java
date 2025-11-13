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

package com.bytechef.component.microsoft.outlook.action;

import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FORMAT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ID;
import static com.bytechef.component.microsoft.outlook.definition.Format.FULL;
import static com.bytechef.component.microsoft.outlook.definition.Format.SIMPLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class MicrosoftOutlook365GetMailActionTest {

    private final ArgumentCaptor<Context> contextArgumentCaptor = ArgumentCaptor.forClass(Context.class);
    private final ArgumentCaptor<Map> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private Parameters mockedParameters;
    private final Http.Response mockedResponse = mock(Http.Response.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() {
        Map<String, String> responseMap = Map.of("key", "value");
        mockedParameters = MockParametersFactory.create(Map.of(FORMAT, FULL, ID, "messageId"));

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        Object result = MicrosoftOutlook365GetMailAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(responseMap, result);
    }

    @Test
    void testPerformForSimpleFormat() {
        mockedParameters = MockParametersFactory.create(Map.of(FORMAT, SIMPLE, ID, "messageId"));
        Map<String, String> responseMap = Map.of("key", "value");

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(responseMap);

        try (MockedStatic<MicrosoftOutlook365Utils> microsoftOutlook365UtilsMockedStatic =
            mockStatic(MicrosoftOutlook365Utils.class)) {

            MicrosoftOutlook365Utils.SimpleMessage simpleMessage = new MicrosoftOutlook365Utils.SimpleMessage(
                "id", "conversationId", "subject", "from", List.of("toRecipient"), List.of("ccRecipient"),
                List.of("bccRecipient"), "bodyPreview", "bodyHtml", List.of(), List.of(), "https://example.com");

            microsoftOutlook365UtilsMockedStatic
                .when(() -> MicrosoftOutlook365Utils.createSimpleMessage(
                    contextArgumentCaptor.capture(), mapArgumentCaptor.capture(), stringArgumentCaptor.capture()))
                .thenReturn(simpleMessage);

            Object result = MicrosoftOutlook365GetMailAction.perform(mockedParameters, mockedParameters, mockedContext);

            assertEquals(simpleMessage, result);
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
            assertEquals(responseMap, mapArgumentCaptor.getValue());
            assertEquals("messageId", stringArgumentCaptor.getValue());
        }
    }
}

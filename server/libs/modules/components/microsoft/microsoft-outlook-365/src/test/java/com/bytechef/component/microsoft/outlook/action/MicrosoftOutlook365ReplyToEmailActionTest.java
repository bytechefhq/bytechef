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

import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ADDRESS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.BCC_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CC_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.COMMENT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONTENT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONTENT_TYPE;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.EMAIL_ADDRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.outlook.constant.ContentType;
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
class MicrosoftOutlook365ReplyToEmailActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(CONTENT_TYPE, ContentType.TEXT.name(), CONTENT, "test", CC_RECIPIENTS, List.of("address1"),
            BCC_RECIPIENTS, List.of("address2")));
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    @SuppressWarnings("unchecked")
    void testPerform() {
        try (MockedStatic<MicrosoftOutlook365Utils> microsoftOutlook365UtilsMockedStatic =
            mockStatic(MicrosoftOutlook365Utils.class)) {

            microsoftOutlook365UtilsMockedStatic.when(
                () -> MicrosoftOutlook365Utils.createRecipientList(listArgumentCaptor.capture()))
                .thenReturn(
                    List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "address1"))),
                    List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "address2"))));

            when(mockedActionContext.http(any()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.configuration(any()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.execute())
                .thenReturn(mockedResponse);

            Object result = MicrosoftOutlook365ReplyToEmailAction.perform(
                mockedParameters, mockedParameters, mockedActionContext);

            assertNull(result);

            Http.Body body = bodyArgumentCaptor.getValue();

            assertEquals(
                Map.of(
                    "message", Map.of(
                        CC_RECIPIENTS, List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "address1"))),
                        BCC_RECIPIENTS, List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "address2")))),
                    COMMENT, "test"),
                body.getContent());
        }
    }
}

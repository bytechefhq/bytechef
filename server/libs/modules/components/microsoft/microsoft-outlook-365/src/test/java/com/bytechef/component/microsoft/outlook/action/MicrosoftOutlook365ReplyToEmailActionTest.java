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
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.BodyContentType;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.outlook.constant.ContentType;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.component.test.definition.extension.MockContextSetupExtension;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
@ExtendWith(MockContextSetupExtension.class)
class MicrosoftOutlook365ReplyToEmailActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> listArgumentCaptor = forClass(List.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            ID, "xy",
            CONTENT_TYPE, ContentType.TEXT.name(), CONTENT, "test", CC_RECIPIENTS, List.of("address1"),
            BCC_RECIPIENTS, List.of("address2")));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    @SuppressWarnings("unchecked")
    void testPerform(
        Context mockedContext, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<ContextFunction<Http, Executor>> httpFunctionArgumentCaptor) {

        try (MockedStatic<MicrosoftOutlook365Utils> microsoftOutlook365UtilsMockedStatic =
            mockStatic(MicrosoftOutlook365Utils.class)) {

            microsoftOutlook365UtilsMockedStatic.when(
                () -> MicrosoftOutlook365Utils.createRecipientList(listArgumentCaptor.capture()))
                .thenReturn(
                    List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "address1"))),
                    List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "address2"))));

            when(mockedHttp.post(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);

            Object result = MicrosoftOutlook365ReplyToEmailAction.perform(mockedParameters, null, mockedContext);

            assertNull(result);
            assertNotNull(httpFunctionArgumentCaptor.getValue());
            assertEquals("/me/messages/xy/reply", stringArgumentCaptor.getValue());
            assertEquals(List.of(List.of("address1"), List.of("address2")), listArgumentCaptor.getAllValues());

            Map<String, Object> expectedBody = Map.of(
                "message", Map.of(
                    CC_RECIPIENTS, List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "address1"))),
                    BCC_RECIPIENTS, List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "address2")))),
                COMMENT, "test");

            assertEquals(Body.of(expectedBody, BodyContentType.JSON), bodyArgumentCaptor.getValue());
        }
    }
}

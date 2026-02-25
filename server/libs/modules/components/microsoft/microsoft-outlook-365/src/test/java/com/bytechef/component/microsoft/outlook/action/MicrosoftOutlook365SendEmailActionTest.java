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
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ATTACHMENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.BCC_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.BODY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CC_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONTENT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONTENT_BYTES;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONTENT_TYPE;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.EMAIL_ADDRESS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.NAME;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.REPLY_TO;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SUBJECT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.TO_RECIPIENTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.Executor;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
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
class MicrosoftOutlook365SendEmailActionTest {

    private final ArgumentCaptor<Body> bodyArgumentCaptor = forClass(Body.class);
    private final ArgumentCaptor<Context> contextArgumentCaptor = forClass(Context.class);
    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> listArgumentCaptor = forClass(List.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(
            SUBJECT, "testSubject", BODY, Map.of(CONTENT, "test", CONTENT_TYPE, "text"),
            TO_RECIPIENTS, List.of("address1"), CC_RECIPIENTS, List.of("address2"),
            BCC_RECIPIENTS, List.of("address3"), REPLY_TO, List.of("address4"), ATTACHMENTS, List.of(mockedFileEntry)));
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @SuppressWarnings("unchecked")
    @Test
    void testPerform(
        Context mockedContext, Executor mockedExecutor, Http mockedHttp,
        ArgumentCaptor<Context.ContextFunction<Http, Executor>> httpFunctionArgumentCaptor) {

        try (MockedStatic<MicrosoftOutlook365Utils> microsoftOutlook365UtilsMockedStatic =
            mockStatic(MicrosoftOutlook365Utils.class)) {

            microsoftOutlook365UtilsMockedStatic.when(
                () -> MicrosoftOutlook365Utils.createRecipientList(listArgumentCaptor.capture()))
                .thenReturn(
                    List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "address1"))),
                    List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "address2"))),
                    List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "address3"))),
                    List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "address4"))));

            byte[] fileContent = new byte[] {
                1, 2, 3
            };

            String encodedToString = EncodingUtils.base64EncodeToString(fileContent);

            microsoftOutlook365UtilsMockedStatic.when(
                () -> MicrosoftOutlook365Utils.getAttachments(
                    contextArgumentCaptor.capture(), listArgumentCaptor.capture()))
                .thenReturn(List.of(
                    Map.of(
                        "@odata.type", "#microsoft.graph.fileAttachment",
                        NAME, "file.txt",
                        CONTENT_TYPE, "text/plain",
                        CONTENT_BYTES, encodedToString)));

            when(mockedHttp.post(stringArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);
            when(mockedExecutor.body(bodyArgumentCaptor.capture()))
                .thenReturn(mockedExecutor);

            Object result = MicrosoftOutlook365SendEmailAction.perform(mockedParameters, null, mockedContext);

            assertNull(result);
            assertNotNull(httpFunctionArgumentCaptor.getValue());
            assertEquals("/me/sendMail", stringArgumentCaptor.getValue());
            assertEquals(mockedContext, contextArgumentCaptor.getValue());
            assertEquals(
                List.of(List.of("address1"), List.of("address2"), List.of("address3"), List.of("address4"),
                    List.of(mockedFileEntry)),
                listArgumentCaptor.getAllValues());

            Body body = bodyArgumentCaptor.getValue();

            Map<String, Map<String, Object>> expectedBody = Map.of(
                "message",
                Map.of(
                    SUBJECT, "testSubject",
                    BODY, Map.of(CONTENT, "test", CONTENT_TYPE, "text"),
                    TO_RECIPIENTS, List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "address1"))),
                    CC_RECIPIENTS, List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "address2"))),
                    BCC_RECIPIENTS, List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "address3"))),
                    REPLY_TO, List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "address4"))),
                    ATTACHMENTS, List.of(
                        Map.of(
                            "@odata.type", "#microsoft.graph.fileAttachment",
                            NAME, "file.txt",
                            CONTENT_TYPE, "text/plain",
                            CONTENT_BYTES, encodedToString))));

            assertEquals(expectedBody, body.getContent());
        }
    }
}

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.EncodingUtils;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class MicrosoftOutlook365SendEmailActionTest {

    private final ArgumentCaptor<Http.Body> bodyArgumentCaptor = ArgumentCaptor.forClass(Http.Body.class);
    private final Context mockedContext = mock(Context.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void testPerform() {
        when(mockedParameters.getRequiredString(SUBJECT))
            .thenReturn("testSubject");
        when(mockedParameters.get(BODY))
            .thenReturn(Map.of(CONTENT, "test", CONTENT_TYPE, "text"));
        when(mockedParameters.getList(TO_RECIPIENTS, String.class))
            .thenReturn(List.of("address1"));
        when(mockedParameters.getList(CC_RECIPIENTS, String.class))
            .thenReturn(List.of("address2"));
        when(mockedParameters.getList(BCC_RECIPIENTS, String.class))
            .thenReturn(List.of("address3"));
        when(mockedParameters.getList(REPLY_TO, String.class))
            .thenReturn(List.of("address4"));
        when(mockedParameters.getList(ATTACHMENTS, FileEntry.class))
            .thenReturn(List.of(mockedFileEntry));

        byte[] fileContent = new byte[] {
            1, 2, 3
        };

        String encodedToString = EncodingUtils.base64EncodeToString(fileContent);

        when(mockedFileEntry.getName())
            .thenReturn("file.txt");
        when(mockedFileEntry.getMimeType())
            .thenReturn("text/plain");
        when(mockedContext.file(any()))
            .thenReturn(fileContent);
        when(mockedContext.encoder(any()))
            .thenReturn(encodedToString);
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.body(bodyArgumentCaptor.capture()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);

        Object result = MicrosoftOutlook365SendEmailAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertNull(result);

        Http.Body body = bodyArgumentCaptor.getValue();

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

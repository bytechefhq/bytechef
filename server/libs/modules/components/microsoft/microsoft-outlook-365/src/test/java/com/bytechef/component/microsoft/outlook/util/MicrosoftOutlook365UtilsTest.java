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

package com.bytechef.component.microsoft.outlook.util;

import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ADDRESS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.BCC_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.BODY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CC_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONTENT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONTENT_BYTES;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.EMAIL_ADDRESS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FROM;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SUBJECT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.TO_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.TypeReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Kušter
 */
class MicrosoftOutlook365UtilsTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Context mockedContext = mock(Context.class);
    private final FileEntry mockedFileEntry = mock(FileEntry.class);
    private final Http.Executor mockedExecutor = mock(Http.Executor.class);
    private final Http.Response mockedResponse = mock(Http.Response.class);

    @Test
    void testCreateSimpleMessage() {
        when(mockedContext.file(any()))
            .thenReturn(mockedFileEntry);
        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(VALUE, List.of(Map.of(
                CONTENT_BYTES, "encode"))));

        Map<String, Object> messageBody = Map.of(
            ID, "messageId",
            SUBJECT, "Test Subject",
            FROM, Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "test@mail.com")),
            BODY, Map.of(CONTENT, "Hello World!"),
            "hasAttachments", true,
            TO_RECIPIENTS, List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "address1"))),
            CC_RECIPIENTS, List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "address2"))),
            BCC_RECIPIENTS, List.of(Map.of(EMAIL_ADDRESS, Map.of(ADDRESS, "address3"))),
            "bodyPreview", "Hello World!");

        MicrosoftOutlook365Utils.SimpleMessage result = MicrosoftOutlook365Utils.createSimpleMessage(
            mockedContext, messageBody, "messageId");

        assertEquals(
            new MicrosoftOutlook365Utils.SimpleMessage(
                "messageId",
                "Test Subject",
                "test@mail.com",
                List.of("address1"),
                List.of("address2"),
                List.of("address3"),
                "Hello World!",
                "Hello World!",
                List.of(mockedFileEntry)),
            result);
    }

    @Test
    void testGetItemsFromNextPage() {
        List<Map<String, String>> items = List.of(Map.of("displayName", "abc"));

        Map<String, Object> body = Map.of(VALUE, items);

        when(mockedContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(body);

        List<Map<?, ?>> result = MicrosoftOutlook365Utils.getItemsFromNextPage("link", mockedContext);

        assertEquals(items, result);
    }

    @Test
    void testGetMailboxTImeZone() {
        when(mockedActionContext.http(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.configuration(any()))
            .thenReturn(mockedExecutor);
        when(mockedExecutor.execute())
            .thenReturn(mockedResponse);
        when(mockedResponse.getBody(any(TypeReference.class)))
            .thenReturn(Map.of(VALUE, "zone"));

        String result = MicrosoftOutlook365Utils.getMailboxTimeZone(mockedActionContext);

        assertEquals("zone", result);
    }
}

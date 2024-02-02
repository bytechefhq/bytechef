/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.google.mail.action;

import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.METADATA_HEADERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Thread;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Domiter
 */
class GoogleMailGetThreadActionTest extends AbstractGoogleMailActionTest {

    private final ArgumentCaptor<String> idArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> formatArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final Gmail.Users.Threads.Get mockedGet = mock(Gmail.Users.Threads.Get.class);
    private final Thread mockedThread = mock(Thread.class);
    private final Gmail.Users.Threads mockedThreads = mock(Gmail.Users.Threads.class);
    private final Gmail.Users mockedUsers = mock(Gmail.Users.class);
    private final ArgumentCaptor<List> metadataArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private final ArgumentCaptor<String> userIdArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() throws IOException {
        List<String> metadata = List.of("metadata");

        when(mockedParameters.getRequiredString(ID))
            .thenReturn("id");
        when(mockedParameters.getString(FORMAT))
            .thenReturn("minimal");
        when(mockedParameters.getList(METADATA_HEADERS, String.class, List.of()))
            .thenReturn(metadata);

        when(mockedGmail.users())
            .thenReturn(mockedUsers);
        when(mockedUsers.threads())
            .thenReturn(mockedThreads);
        when(mockedThreads.get(userIdArgumentCaptor.capture(), idArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.setFormat(formatArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.setMetadataHeaders(metadataArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.execute())
            .thenReturn(mockedThread);

        Thread thread = GoogleMailGetThreadAction.perform(mockedParameters, mockedParameters, mockedContext);

        assertEquals(mockedThread, thread);
        assertEquals("me", userIdArgumentCaptor.getValue());
        assertEquals("id", idArgumentCaptor.getValue());
        assertEquals("minimal", formatArgumentCaptor.getValue());
        assertEquals(metadata, metadataArgumentCaptor.getValue());
    }
}

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

import static com.bytechef.component.google.mail.constant.GoogleMailConstants.BODY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.CATEGORY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FROM;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.INCLUDE_SPAM_TRASH;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.LABEL;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.LABEL_IDS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.MAX_RESULTS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.PAGE_TOKEN;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SUBJECT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.TO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.test.component.properties.ParametersFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Monika Kušter
 */
class GoogleMailSearchEmailActionTest extends AbstractGoogleMailActionTest {

    private final ArgumentCaptor<Boolean> includeSpamTrashArgumentCaptor = ArgumentCaptor.forClass(Boolean.class);
    private final ArgumentCaptor<List> labelIDsArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private final ArgumentCaptor<Long> maxResultsArgumentCaptor = ArgumentCaptor.forClass(Long.class);
    private final Gmail.Users.Messages.List mockedList = mock(Gmail.Users.Messages.List.class);
    private final ListMessagesResponse mockedListMessagesResponse = mock(ListMessagesResponse.class);
    private final Gmail.Users.Messages mockedMessages = mock(Gmail.Users.Messages.class);
    private final Gmail.Users mockedUsers = mock(Gmail.Users.class);
    private final ArgumentCaptor<String> pageTokenArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> qArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> userIdArgumentCaptor = ArgumentCaptor.forClass(String.class);

    @Test
    void testPerform() throws IOException {
        List<String> labelIDs = List.of("id1", "id2");

        Parameters parameters = ParametersFactory.createParameters(
            Map.of(FROM, "from@mail.com", TO, "to@mail.com", SUBJECT, "subject", BODY, "body", MAX_RESULTS, 1L,
                PAGE_TOKEN, "pageToken", CATEGORY, "social", LABEL, "label", LABEL_IDS, List.of("id1", "id2"),
                INCLUDE_SPAM_TRASH, true));

        when(mockedGmail.users())
            .thenReturn(mockedUsers);
        when(mockedUsers.messages())
            .thenReturn(mockedMessages);
        when(mockedMessages.list(userIdArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setMaxResults(maxResultsArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setPageToken(pageTokenArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setQ(qArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setLabelIds(labelIDsArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setIncludeSpamTrash(includeSpamTrashArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.execute())
            .thenReturn(mockedListMessagesResponse);

        ListMessagesResponse response = GoogleMailSearchEmailAction.perform(
            parameters, parameters, mockedActionContext);

        assertEquals(mockedListMessagesResponse, response);
        assertEquals(ME, userIdArgumentCaptor.getValue());
        assertEquals(1, maxResultsArgumentCaptor.getValue());
        assertEquals("pageToken", pageTokenArgumentCaptor.getValue());
        assertEquals(" from:from@mail.com to:to@mail.com subject:subject category:social label:label",
            qArgumentCaptor.getValue());
        assertEquals(labelIDs, labelIDsArgumentCaptor.getValue());
        assertEquals(true, includeSpamTrashArgumentCaptor.getValue());
    }
}

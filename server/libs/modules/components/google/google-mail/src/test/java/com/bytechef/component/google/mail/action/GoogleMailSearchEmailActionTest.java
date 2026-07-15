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

package com.bytechef.component.google.mail.action;

import static com.bytechef.component.google.mail.constant.GoogleMailConstants.BODY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.CATEGORY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FROM;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.INCLUDE_SPAM_TRASH;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.LABEL_IDS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.MAX_RESULTS;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.PAGE_TOKEN;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.SUBJECT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.TO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Kušter
 */
class GoogleMailSearchEmailActionTest {

    private final ArgumentCaptor<Boolean> booleanArgumentCaptor = forClass(Boolean.class);
    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> listArgumentCaptor = forClass(List.class);
    private final ArgumentCaptor<Long> longArgumentCaptor = forClass(Long.class);
    private final Parameters mockedConnectionParameters = mock(Parameters.class);
    private final Gmail mockedGmail = mock(Gmail.class);
    private final Parameters mockedInputParameters = MockParametersFactory.create(
        Map.of(FROM, "from@mail.com", TO, "to@mail.com", SUBJECT, "subject", BODY, "body", MAX_RESULTS, 1L,
            PAGE_TOKEN, "pageToken", CATEGORY, "social", LABEL_IDS, List.of("id1", "id2"),
            INCLUDE_SPAM_TRASH, true));
    private final Gmail.Users.Messages.List mockedList = mock(Gmail.Users.Messages.List.class);
    private final ListMessagesResponse mockedListMessagesResponse = mock(ListMessagesResponse.class);
    private final Gmail.Users.Messages mockedMessages = mock(Gmail.Users.Messages.class);
    private final Gmail.Users mockedUsers = mock(Gmail.Users.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);

    @Test
    @SuppressWarnings("unchecked")
    void testPerform() throws IOException {
        try (MockedStatic<GoogleServices> googleServicesMockedStatic = mockStatic(GoogleServices.class)) {
            googleServicesMockedStatic
                .when(() -> GoogleServices.getMail(parametersArgumentCaptor.capture()))
                .thenReturn(mockedGmail);
            when(mockedGmail.users())
                .thenReturn(mockedUsers);
            when(mockedUsers.messages())
                .thenReturn(mockedMessages);
            when(mockedMessages.list(stringArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.setMaxResults(longArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.setPageToken(stringArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.setQ(stringArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.setLabelIds(listArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.setIncludeSpamTrash(booleanArgumentCaptor.capture()))
                .thenReturn(mockedList);
            when(mockedList.execute())
                .thenReturn(mockedListMessagesResponse);

            ListMessagesResponse response = GoogleMailSearchEmailAction.perform(
                mockedInputParameters, mockedConnectionParameters, mock(ActionContext.class));

            assertEquals(mockedListMessagesResponse, response);
            assertEquals(mockedConnectionParameters, parametersArgumentCaptor.getValue());
            assertEquals(
                List.of(ME, "pageToken", " from:from@mail.com to:to@mail.com subject:subject category:social"),
                stringArgumentCaptor.getAllValues());
            assertEquals(1, longArgumentCaptor.getValue());
            assertEquals(List.of("id1", "id2"), listArgumentCaptor.getValue());
            assertEquals(true, booleanArgumentCaptor.getValue());
        }
    }
}

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

package com.bytechef.component.google.mail.trigger;

import static com.bytechef.component.google.mail.constant.GoogleMailConstants.HISTORY_ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.TOPIC_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.Gmail.Users;
import com.google.api.services.gmail.Gmail.Users.Messages;
import com.google.api.services.gmail.Gmail.Users.Messages.Get;
import com.google.api.services.gmail.Gmail.Users.Stop;
import com.google.api.services.gmail.Gmail.Users.Watch;
import com.google.api.services.gmail.model.History;
import com.google.api.services.gmail.model.HistoryMessageAdded;
import com.google.api.services.gmail.model.ListHistoryResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.WatchRequest;
import com.google.api.services.gmail.model.WatchResponse;
import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */

class GoogleMailNewEmailTriggerTest {

    private final DynamicWebhookEnableOutput mockedDynamicWebhookEnableOutput = mock(DynamicWebhookEnableOutput.class);
    private final Get mockedGet = mock(Get.class);
    protected MockedStatic<GoogleServices> mockedGoogleServices;
    private final Gmail mockedGmail = mock(Gmail.class);
    private final Users.History mockedHistory = mock(Users.History.class);
    private final HttpHeaders mockedHttpHeaders = mock(HttpHeaders.class);
    private final HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    private final Users.History.List mockedList = mock(Users.History.List.class);
    private final Messages mockedMessages = mock(Messages.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final Stop mockedStop = mock(Stop.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final Users mockedUsers = mock(Users.class);
    private final Watch mockedWatch = mock(Watch.class);
    private final WatchResponse mockedWatchResponse = mock(WatchResponse.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final WebhookMethod mockedWebhookMethod = mock(WebhookMethod.class);
    private final ArgumentCaptor<BigInteger> historyIdArgumentCaptor = ArgumentCaptor.forClass(BigInteger.class);
    private final ArgumentCaptor<String> userIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> userIdTwoArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> messageIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<WatchRequest> watchRequestArgumentCaptor = ArgumentCaptor.forClass(WatchRequest.class);
    private static final String workflowExecutionId = "testWorkflowExecutionId";

    @BeforeEach
    public void beforeEach() {
        mockedGoogleServices = mockStatic(GoogleServices.class);

        mockedGoogleServices.when(() -> GoogleServices.getMail(mockedParameters))
            .thenReturn(mockedGmail);
    }

    @AfterEach
    public void afterEach() {
        mockedGoogleServices.close();
    }

    @Test
    void testDynamicWebhookEnable() throws IOException {
        String webhookUrl = "testWebhookUrl";

        when(mockedParameters.getRequiredString(TOPIC_NAME))
            .thenReturn("topic");

        when(mockedGmail.users())
            .thenReturn(mockedUsers);
        when(mockedUsers.watch(userIdArgumentCaptor.capture(), watchRequestArgumentCaptor.capture()))
            .thenReturn(mockedWatch);
        when(mockedWatch.execute())
            .thenReturn(mockedWatchResponse);
        when(mockedWatchResponse.getHistoryId())
            .thenReturn(new BigInteger("123"));

        DynamicWebhookEnableOutput dynamicWebhookEnableOutput = GoogleMailNewEmailTrigger.dynamicWebhookEnable(
            mockedParameters, mockedParameters, webhookUrl, workflowExecutionId, mockedTriggerContext);

        Map<String, BigInteger> expectedParameters = Map.of(HISTORY_ID, new BigInteger("123"));
        LocalDateTime webhookExpirationDate = dynamicWebhookEnableOutput.webhookExpirationDate();

        assertEquals(expectedParameters, dynamicWebhookEnableOutput.parameters());
        assertNull(webhookExpirationDate);

        WatchRequest watchRequestArgumentCaptorValue = watchRequestArgumentCaptor.getValue();

        assertEquals("topic", watchRequestArgumentCaptorValue.getTopicName());
        assertEquals(List.of("INBOX"), watchRequestArgumentCaptorValue.getLabelIds());

        assertEquals(ME, userIdArgumentCaptor.getValue());
    }

    @Test
    void testDynamicWebhookDisable() throws IOException {
        when(mockedGmail.users())
            .thenReturn(mockedUsers);
        when(mockedUsers.stop(userIdArgumentCaptor.capture()))
            .thenReturn(mockedStop);

        GoogleMailNewEmailTrigger.dynamicWebhookDisable(
            mockedParameters, mockedParameters, mockedParameters, workflowExecutionId, mockedTriggerContext);

        assertEquals(ME, userIdArgumentCaptor.getValue());
    }

    @Test
    void testDynamicWebhookRequest() throws IOException {
        Message message = new Message().setId("2");

        ListHistoryResponse listHistoryResponse = new ListHistoryResponse()
            .setHistory(
                List.of(new History().setMessagesAdded(List.of(new HistoryMessageAdded().setMessage(message)))));

        when(mockedTriggerContext.data(any()))
            .thenReturn(Optional.of(123));
        when(mockedDynamicWebhookEnableOutput.parameters())
            .thenReturn((Map) Map.of(HISTORY_ID, 123));
        when(mockedGmail.users())
            .thenReturn(mockedUsers);
        when(mockedUsers.history())
            .thenReturn(mockedHistory);
        when(mockedHistory.list(userIdArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setStartHistoryId(historyIdArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.execute())
            .thenReturn(listHistoryResponse);
        when(mockedUsers.messages())
            .thenReturn(mockedMessages);
        when(mockedMessages.get(userIdTwoArgumentCaptor.capture(), messageIdArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.execute())
            .thenReturn(message);

        List<Message> messages = GoogleMailNewEmailTrigger.dynamicWebhookRequest(
            mockedParameters, mockedParameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedDynamicWebhookEnableOutput, mockedTriggerContext);

        assertEquals(List.of(message), messages);

        assertEquals(ME, userIdArgumentCaptor.getValue());
        assertEquals(new BigInteger("123"), historyIdArgumentCaptor.getValue());
        assertEquals(ME, userIdTwoArgumentCaptor.getValue());
        assertEquals("2", messageIdArgumentCaptor.getValue());
    }
}

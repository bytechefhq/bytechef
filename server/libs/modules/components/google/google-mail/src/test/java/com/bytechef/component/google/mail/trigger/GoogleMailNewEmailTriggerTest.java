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

package com.bytechef.component.google.mail.trigger;

import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.HISTORY_ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.TOPIC_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.google.mail.definition.Format;
import com.bytechef.component.test.definition.MockParametersFactory;
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

    private final Parameters mockedWebhookEnableOutput = MockParametersFactory.create(Map.of(HISTORY_ID, 123));
    private final Get mockedGet = mock(Get.class);
    protected MockedStatic<GoogleServices> mockedGoogleServices;
    private final Gmail mockedGmail = mock(Gmail.class);
    private final Users.History mockedHistory = mock(Users.History.class);
    private final HttpHeaders mockedHttpHeaders = mock(HttpHeaders.class);
    private final HttpParameters mockedHttpParameters = mock(HttpParameters.class);
    private final Users.History.List mockedList = mock(Users.History.List.class);
    private final Messages mockedMessages = mock(Messages.class);
    private Parameters parameters;
    private final Stop mockedStop = mock(Stop.class);
    private final TriggerContext mockedTriggerContext = mock(TriggerContext.class);
    private final Users mockedUsers = mock(Users.class);
    private final Watch mockedWatch = mock(Watch.class);
    private final WebhookBody mockedWebhookBody = mock(WebhookBody.class);
    private final WebhookMethod mockedWebhookMethod = mock(WebhookMethod.class);
    private final ArgumentCaptor<BigInteger> bigIntegerArgumentCaptor = forClass(BigInteger.class);
    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> listArgumentCaptor = forClass(List.class);
    private final ArgumentCaptor<Parameters> parametersArgumentCaptor = forClass(Parameters.class);
    private final ArgumentCaptor<String> stringArgumentCaptor = forClass(String.class);
    private final ArgumentCaptor<WatchRequest> watchRequestArgumentCaptor = forClass(WatchRequest.class);
    private static final String workflowExecutionId = "testWorkflowExecutionId";

    @BeforeEach
    void beforeEach() {
        mockedGoogleServices = mockStatic(GoogleServices.class);

        mockedGoogleServices.when(() -> GoogleServices.getMail(parametersArgumentCaptor.capture()))
            .thenReturn(mockedGmail);

        parameters = MockParametersFactory.create(Map.of(TOPIC_NAME, "topic", FORMAT, Format.FULL));
    }

    @AfterEach
    void afterEach() {
        assertEquals(parameters, parametersArgumentCaptor.getValue());
        mockedGoogleServices.close();
    }

    @Test
    void testWebhookEnable() throws IOException {
        String webhookUrl = "testWebhookUrl";

        when(mockedGmail.users())
            .thenReturn(mockedUsers);
        when(mockedUsers.watch(stringArgumentCaptor.capture(), watchRequestArgumentCaptor.capture()))
            .thenReturn(mockedWatch);
        when(mockedWatch.execute())
            .thenReturn(
                new WatchResponse().setHistoryId(new BigInteger("123")));

        WebhookEnableOutput result = GoogleMailNewEmailTrigger.webhookEnable(
            parameters, parameters, webhookUrl, workflowExecutionId, mockedTriggerContext);

        WebhookEnableOutput expectedWebhookEnableOutput =
            new WebhookEnableOutput(Map.of(HISTORY_ID, new BigInteger("123")), null);

        assertEquals(expectedWebhookEnableOutput, result);
        assertEquals(ME, stringArgumentCaptor.getValue());

        WatchRequest watchRequest = new WatchRequest()
            .setTopicName("topic")
            .setLabelIds(List.of("INBOX"));

        assertEquals(watchRequest, watchRequestArgumentCaptor.getValue());
    }

    @Test
    void testWebhookDisable() throws IOException {
        when(mockedGmail.users())
            .thenReturn(mockedUsers);
        when(mockedUsers.stop(stringArgumentCaptor.capture()))
            .thenReturn(mockedStop);

        GoogleMailNewEmailTrigger.webhookDisable(
            parameters, parameters, parameters, workflowExecutionId, mockedTriggerContext);

        assertEquals(ME, stringArgumentCaptor.getValue());
    }

    @Test
    void testWebhookRequest() throws IOException {
        Message message = new Message().setId("2");

        ListHistoryResponse listHistoryResponse = new ListHistoryResponse()
            .setHistory(
                List.of(new History().setMessagesAdded(List.of(new HistoryMessageAdded().setMessage(message)))));

        when(mockedTriggerContext.data(any()))
            .thenReturn(Optional.of(123));
        when(mockedGmail.users())
            .thenReturn(mockedUsers);
        when(mockedUsers.history())
            .thenReturn(mockedHistory);
        when(mockedHistory.list(stringArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setStartHistoryId(bigIntegerArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.setHistoryTypes(listArgumentCaptor.capture()))
            .thenReturn(mockedList);
        when(mockedList.execute())
            .thenReturn(listHistoryResponse);
        when(mockedUsers.messages())
            .thenReturn(mockedMessages);
        when(mockedMessages.get(stringArgumentCaptor.capture(), stringArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.setFormat(stringArgumentCaptor.capture()))
            .thenReturn(mockedGet);
        when(mockedGet.execute())
            .thenReturn(message);

        List<Object> messages = GoogleMailNewEmailTrigger.webhookRequest(
            parameters, parameters, mockedHttpHeaders, mockedHttpParameters, mockedWebhookBody,
            mockedWebhookMethod, mockedWebhookEnableOutput, mockedTriggerContext);

        assertEquals(List.of(message), messages);

        assertEquals(List.of(ME, ME, "2", Format.FULL.getMapping()), stringArgumentCaptor.getAllValues());
        assertEquals(new BigInteger("123"), bigIntegerArgumentCaptor.getValue());
        assertEquals(List.of("messageAdded"), listArgumentCaptor.getValue());
    }
}

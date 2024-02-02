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

package com.bytechef.component.infobip.action;

import static com.bytechef.component.definition.Authorization.VALUE;
import static com.bytechef.component.infobip.constant.InfobipConstants.APPLICATION_ID;
import static com.bytechef.component.infobip.constant.InfobipConstants.CALLBACK_DATA;
import static com.bytechef.component.infobip.constant.InfobipConstants.CONTENT;
import static com.bytechef.component.infobip.constant.InfobipConstants.ENTITY_ID;
import static com.bytechef.component.infobip.constant.InfobipConstants.FROM;
import static com.bytechef.component.infobip.constant.InfobipConstants.MESSAGE_ID;
import static com.bytechef.component.infobip.constant.InfobipConstants.NOTIFY_URL;
import static com.bytechef.component.infobip.constant.InfobipConstants.TO;
import static com.bytechef.component.infobip.constant.InfobipConstants.URL_OPTIONS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.infobip.ApiException;
import com.infobip.api.WhatsAppApi;
import com.infobip.model.WhatsAppSingleMessageInfo;
import com.infobip.model.WhatsAppTextContent;
import com.infobip.model.WhatsAppTextMessage;
import com.infobip.model.WhatsAppUrlOptions;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

/**
 * @author Monika Domiter
 */
class InfobipSendWhatsappTextMessageActionTest extends AbstractInfobipActionTest {

    private final ArgumentCaptor<WhatsAppTextMessage> whatsAppTextMessageArgumentCaptor = ArgumentCaptor.forClass(
        WhatsAppTextMessage.class);
    private final WhatsAppSingleMessageInfo mockedWhatsAppSingleMessageInfo = mock(WhatsAppSingleMessageInfo.class);
    private final WhatsAppApi.SendWhatsAppTextMessageRequest mockedSendSmsMessageRequest =
        mock(WhatsAppApi.SendWhatsAppTextMessageRequest.class);

    @Test
    void testPerform() throws ApiException {

        WhatsAppTextContent whatsAppTextContent = new WhatsAppTextContent().text("text");
        WhatsAppUrlOptions whatsAppUrlOptions = new WhatsAppUrlOptions().shortenUrl(true);

        when(mockedParameters.getRequiredString(VALUE))
            .thenReturn("value");
        when(mockedParameters.getRequiredString(FROM))
            .thenReturn("from");
        when(mockedParameters.getRequiredString(TO))
            .thenReturn("to");
        when(mockedParameters.getString(MESSAGE_ID))
            .thenReturn("messageId");
        when(mockedParameters.getRequired(CONTENT, WhatsAppTextContent.class))
            .thenReturn(whatsAppTextContent);
        when(mockedParameters.getString(CALLBACK_DATA))
            .thenReturn("callbackData");
        when(mockedParameters.getString(NOTIFY_URL))
            .thenReturn("notifyUrl");
        when(mockedParameters.get(URL_OPTIONS, WhatsAppUrlOptions.class))
            .thenReturn(whatsAppUrlOptions);
        when(mockedParameters.getString(ENTITY_ID))
            .thenReturn("entityId");
        when(mockedParameters.getString(APPLICATION_ID))
            .thenReturn("applicationId");

        try (MockedConstruction<WhatsAppApi> whatsAppApiMockedConstruction = mockConstruction(
            WhatsAppApi.class,
            ((whatsAppApi, context) -> when(whatsAppApi.sendWhatsAppTextMessage(any()))
                .thenReturn(mockedSendSmsMessageRequest)))) {
            when(mockedSendSmsMessageRequest.execute())
                .thenReturn(mockedWhatsAppSingleMessageInfo);

            WhatsAppSingleMessageInfo messageInfo = InfobipSendWhatsappTextMesageAction.perform(
                mockedParameters, mockedParameters, mockedContext);

            List<WhatsAppApi> whatsAppApis = whatsAppApiMockedConstruction.constructed();

            assertEquals(1, whatsAppApis.size());
            assertEquals(mockedWhatsAppSingleMessageInfo, messageInfo);

            WhatsAppApi whatsAppApi = whatsAppApis.getFirst();

            verify(whatsAppApi, times(1)).sendWhatsAppTextMessage(whatsAppTextMessageArgumentCaptor.capture());

            WhatsAppTextMessage whatsAppTextMessage = whatsAppTextMessageArgumentCaptor.getValue();

            assertEquals("from", whatsAppTextMessage.getFrom());
            assertEquals("to", whatsAppTextMessage.getTo());
            assertEquals("messageId", whatsAppTextMessage.getMessageId());
            assertEquals(whatsAppTextContent, whatsAppTextMessage.getContent());
            assertEquals("callbackData", whatsAppTextMessage.getCallbackData());
            assertEquals("notifyUrl", whatsAppTextMessage.getNotifyUrl());
            assertEquals(whatsAppUrlOptions, whatsAppTextMessage.getUrlOptions());
            assertEquals("entityId", whatsAppTextMessage.getEntityId());
            assertEquals("applicationId", whatsAppTextMessage.getApplicationId());
        }
    }
}

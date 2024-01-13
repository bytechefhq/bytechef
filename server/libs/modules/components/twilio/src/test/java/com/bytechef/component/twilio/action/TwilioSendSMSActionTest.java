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

package com.bytechef.component.twilio.action;

import static com.bytechef.component.definition.Authorization.PASSWORD;
import static com.bytechef.component.definition.Authorization.USERNAME;
import static com.bytechef.component.twilio.constant.TwilioConstants.ACCOUNT_SID;
import static com.bytechef.component.twilio.constant.TwilioConstants.BODY;
import static com.bytechef.component.twilio.constant.TwilioConstants.FROM;
import static com.bytechef.component.twilio.constant.TwilioConstants.MEDIA_URL;
import static com.bytechef.component.twilio.constant.TwilioConstants.MESSAGING_SERVICE_SID;
import static com.bytechef.component.twilio.constant.TwilioConstants.TO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageCreator;
import com.twilio.type.PhoneNumber;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
class TwilioSendSMSActionTest {

    private final ArgumentCaptor<String> accountSidStringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> bodyStringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<PhoneNumber> fromPhoneNumberArgumentCaptor = ArgumentCaptor.forClass(
        PhoneNumber.class);
    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private final ArgumentCaptor<String> messagingServidSIDStringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Message mockedMessage = mock(Message.class);
    private final MessageCreator mockedMessageCreator = mock(MessageCreator.class);
    private final ArgumentCaptor<PhoneNumber> toPhoneNumberArgumentCaptor = ArgumentCaptor.forClass(PhoneNumber.class);

    @BeforeEach
    public void beforeEach() {
        when(mockedParameters.getRequiredString(USERNAME))
            .thenReturn("username");
        when(mockedParameters.getRequiredString(PASSWORD))
            .thenReturn("password");
        when(mockedParameters.getString(TO))
            .thenReturn("+15592585054");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPerformFirstCase() {
        List<URI> uriList = List.of();

        when(mockedParameters.getString(MESSAGING_SERVICE_SID))
            .thenReturn("messaging_service_sid");
        when(mockedParameters.getList(MEDIA_URL, URI.class))
            .thenReturn(uriList);

        try (MockedStatic<Twilio> twilioMockedStatic = mockStatic(Twilio.class)) {
            twilioMockedStatic.when(
                () -> Twilio.init(
                    mockedParameters.getRequiredString(USERNAME), mockedParameters.getRequiredString(PASSWORD)))
                .thenAnswer(Answers.RETURNS_DEFAULTS);

            try (MockedStatic<Message> messageMockedStatic = mockStatic(Message.class)) {
                messageMockedStatic.when(
                    () -> Message.creator(
                        toPhoneNumberArgumentCaptor.capture(), messagingServidSIDStringArgumentCaptor.capture(),
                        listArgumentCaptor.capture()))
                    .thenReturn(mockedMessageCreator);

                when(mockedMessageCreator.create())
                    .thenReturn(mockedMessage);

                Message result = TwilioSendSMSAction.perform(mockedParameters, mockedParameters, mockedContext);

                assertEquals(mockedMessage, result);

                PhoneNumber toPhoneNumber = toPhoneNumberArgumentCaptor.getValue();

                assertEquals("+15592585054", toPhoneNumber.getEndpoint());

                assertEquals("messaging_service_sid", messagingServidSIDStringArgumentCaptor.getValue());
                assertEquals(uriList, listArgumentCaptor.getValue());
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPerformSecondCase() {
        List<URI> uriList = List.of();

        when(mockedParameters.getString(ACCOUNT_SID))
            .thenReturn("account_sid");
        when(mockedParameters.getString(MESSAGING_SERVICE_SID))
            .thenReturn("messaging_service_sid");
        when(mockedParameters.getList(MEDIA_URL, URI.class))
            .thenReturn(uriList);

        try (MockedStatic<Twilio> twilioMockedStatic = mockStatic(Twilio.class)) {
            twilioMockedStatic.when(
                () -> Twilio.init(
                    mockedParameters.getRequiredString(USERNAME), mockedParameters.getRequiredString(PASSWORD)))
                .thenAnswer(Answers.RETURNS_DEFAULTS);

            try (MockedStatic<Message> messageMockedStatic = mockStatic(Message.class)) {
                messageMockedStatic.when(
                    () -> Message.creator(
                        accountSidStringArgumentCaptor.capture(), toPhoneNumberArgumentCaptor.capture(),
                        messagingServidSIDStringArgumentCaptor.capture(), listArgumentCaptor.capture()))
                    .thenReturn(mockedMessageCreator);

                when(mockedMessageCreator.create())
                    .thenReturn(mockedMessage);

                assertEquals(
                    mockedMessage, TwilioSendSMSAction.perform(mockedParameters, mockedParameters, mockedContext));

                String accountSid = accountSidStringArgumentCaptor.getValue();

                assertEquals("account_sid", accountSid);

                PhoneNumber toPhoneNumber = toPhoneNumberArgumentCaptor.getValue();

                assertEquals("+15592585054", toPhoneNumber.getEndpoint());

                assertEquals("messaging_service_sid", messagingServidSIDStringArgumentCaptor.getValue());
                assertEquals(uriList, listArgumentCaptor.getValue());
            }
        }
    }

    @Test
    void testPerformThirdCase() {
        when(mockedParameters.getString(MESSAGING_SERVICE_SID))
            .thenReturn("messaging_service_sid");
        when(mockedParameters.getString(BODY))
            .thenReturn("body");

        try (MockedStatic<Twilio> twilioMockedStatic = mockStatic(Twilio.class)) {
            twilioMockedStatic.when(
                () -> Twilio.init(
                    mockedParameters.getRequiredString(USERNAME), mockedParameters.getRequiredString(PASSWORD)))
                .thenAnswer(
                    Answers.RETURNS_DEFAULTS);

            try (MockedStatic<Message> messageMockedStatic = mockStatic(Message.class)) {
                messageMockedStatic.when(
                    () -> Message.creator(
                        toPhoneNumberArgumentCaptor.capture(), messagingServidSIDStringArgumentCaptor.capture(),
                        bodyStringArgumentCaptor.capture()))
                    .thenReturn(mockedMessageCreator);

                when(mockedMessageCreator.create())
                    .thenReturn(mockedMessage);

                assertEquals(
                    mockedMessage, TwilioSendSMSAction.perform(mockedParameters, mockedParameters, mockedContext));

                PhoneNumber toPhoneNumber = toPhoneNumberArgumentCaptor.getValue();

                assertEquals("+15592585054", toPhoneNumber.getEndpoint());

                assertEquals("messaging_service_sid",  messagingServidSIDStringArgumentCaptor.getValue());
                assertEquals("body", bodyStringArgumentCaptor.getValue());
            }
        }
    }

    @Test
    void testPerformFourthCase() {
        when(mockedParameters.getString(ACCOUNT_SID))
            .thenReturn("account_sid");
        when(mockedParameters.getString(MESSAGING_SERVICE_SID))
            .thenReturn("messaging_service_sid");
        when(mockedParameters.getString(BODY))
            .thenReturn("body");

        try (MockedStatic<Twilio> twilioMockedStatic = mockStatic(Twilio.class)) {
            twilioMockedStatic.when(
                () -> Twilio.init(
                    mockedParameters.getRequiredString(USERNAME), mockedParameters.getRequiredString(PASSWORD)))
                .thenAnswer(Answers.RETURNS_DEFAULTS);

            try (MockedStatic<Message> messageMockedStatic = mockStatic(Message.class)) {
                messageMockedStatic.when(
                    () -> Message.creator(
                        accountSidStringArgumentCaptor.capture(), toPhoneNumberArgumentCaptor.capture(),
                        messagingServidSIDStringArgumentCaptor.capture(), bodyStringArgumentCaptor.capture()))
                    .thenReturn(mockedMessageCreator);

                when(mockedMessageCreator.create())
                    .thenReturn(mockedMessage);

                assertEquals(
                    mockedMessage, TwilioSendSMSAction.perform(mockedParameters, mockedParameters, mockedContext));

                assertEquals("account_sid", accountSidStringArgumentCaptor.getValue());

                PhoneNumber toPhoneNumber = toPhoneNumberArgumentCaptor.getValue();

                assertEquals("+15592585054", toPhoneNumber.getEndpoint());

                assertEquals("messaging_service_sid", messagingServidSIDStringArgumentCaptor.getValue());
                assertEquals("body", bodyStringArgumentCaptor.getValue());
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPerformFifthCase() {
        List<URI> uriList = List.of();

        when(mockedParameters.getString(FROM))
            .thenReturn("+15592582024");
        when(mockedParameters.getList(MEDIA_URL, URI.class))
            .thenReturn(uriList);

        try (MockedStatic<Twilio> twilioMockedStatic = mockStatic(Twilio.class)) {
            twilioMockedStatic.when(
                () -> Twilio.init(
                    mockedParameters.getRequiredString(USERNAME), mockedParameters.getRequiredString(PASSWORD)))
                .thenAnswer(Answers.RETURNS_DEFAULTS);

            try (MockedStatic<Message> messageMockedStatic = mockStatic(Message.class)) {
                messageMockedStatic.when(
                    () -> Message.creator(
                        toPhoneNumberArgumentCaptor.capture(), fromPhoneNumberArgumentCaptor.capture(),
                        listArgumentCaptor.capture()))
                    .thenReturn(mockedMessageCreator);

                when(mockedMessageCreator.create())
                    .thenReturn(mockedMessage);

                assertEquals(
                    mockedMessage, TwilioSendSMSAction.perform(mockedParameters, mockedParameters, mockedContext));

                PhoneNumber toPhoneNumber = toPhoneNumberArgumentCaptor.getValue();

                assertEquals("+15592585054", toPhoneNumber.getEndpoint());

                PhoneNumber fromPhoneNumber = fromPhoneNumberArgumentCaptor.getValue();

                assertEquals("+15592582024", fromPhoneNumber.getEndpoint());

                assertEquals(uriList, listArgumentCaptor.getValue());
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPerformSixthCase() {
        List<URI> uriList = List.of();

        when(mockedParameters.getString(ACCOUNT_SID))
            .thenReturn("account_sid");
        when(mockedParameters.getString(FROM))
            .thenReturn("+15592582024");
        when(mockedParameters.getList(MEDIA_URL, URI.class))
            .thenReturn(uriList);

        try (MockedStatic<Twilio> twilioMockedStatic = mockStatic(Twilio.class)) {
            twilioMockedStatic.when(
                () -> Twilio.init(
                    mockedParameters.getRequiredString(USERNAME), mockedParameters.getRequiredString(PASSWORD)))
                .thenAnswer(Answers.RETURNS_DEFAULTS);

            try (MockedStatic<Message> messageMockedStatic = mockStatic(Message.class)) {
                messageMockedStatic.when(
                    () -> Message.creator(
                        accountSidStringArgumentCaptor.capture(), toPhoneNumberArgumentCaptor.capture(),
                        fromPhoneNumberArgumentCaptor.capture(), listArgumentCaptor.capture()))
                    .thenReturn(mockedMessageCreator);

                when(mockedMessageCreator.create())
                    .thenReturn(mockedMessage);

                assertEquals(
                    mockedMessage, TwilioSendSMSAction.perform(mockedParameters, mockedParameters, mockedContext));
                assertEquals("account_sid", accountSidStringArgumentCaptor.getValue());

                PhoneNumber toPhoneNumber = toPhoneNumberArgumentCaptor.getValue();

                assertEquals("+15592585054", toPhoneNumber.getEndpoint());

                PhoneNumber fromPhoneNumber = fromPhoneNumberArgumentCaptor.getValue();

                assertEquals("+15592582024", fromPhoneNumber.getEndpoint());

                assertEquals(uriList, listArgumentCaptor.getValue());
            }
        }
    }

    @Test
    void testPerformSeventhCase() {
        when(mockedParameters.getString(FROM))
            .thenReturn("+15592582024");
        when(mockedParameters.getString(BODY))
            .thenReturn("body");

        try (MockedStatic<Twilio> twilioMockedStatic = mockStatic(Twilio.class)) {
            twilioMockedStatic.when(
                () -> Twilio.init(
                    mockedParameters.getRequiredString(USERNAME), mockedParameters.getRequiredString(PASSWORD)))
                .thenAnswer(
                    Answers.RETURNS_DEFAULTS);

            try (MockedStatic<Message> messageMockedStatic = mockStatic(Message.class)) {
                messageMockedStatic.when(
                    () -> Message.creator(
                        toPhoneNumberArgumentCaptor.capture(), fromPhoneNumberArgumentCaptor.capture(),
                        bodyStringArgumentCaptor.capture()))
                    .thenReturn(mockedMessageCreator);

                when(mockedMessageCreator.create())
                    .thenReturn(mockedMessage);

                assertEquals(
                    mockedMessage, TwilioSendSMSAction.perform(mockedParameters, mockedParameters, mockedContext));

                PhoneNumber toPhoneNumber = toPhoneNumberArgumentCaptor.getValue();

                assertEquals("+15592585054", toPhoneNumber.getEndpoint());

                PhoneNumber fromPhoneNumber = fromPhoneNumberArgumentCaptor.getValue();

                assertEquals("+15592582024", fromPhoneNumber.getEndpoint());

                assertEquals("body", bodyStringArgumentCaptor.getValue());
            }
        }
    }

    @Test
    void testPerformEighthCase() {
        when(mockedParameters.getString(ACCOUNT_SID))
            .thenReturn("account_sid");
        when(mockedParameters.getString(FROM))
            .thenReturn("+15592582024");
        when(mockedParameters.getString(BODY))
            .thenReturn("body");

        try (MockedStatic<Twilio> twilioMockedStatic = mockStatic(Twilio.class)) {
            twilioMockedStatic.when(
                () -> Twilio.init(
                    mockedParameters.getRequiredString(USERNAME), mockedParameters.getRequiredString(PASSWORD)))
                .thenAnswer(Answers.RETURNS_DEFAULTS);

            try (MockedStatic<Message> messageMockedStatic = mockStatic(Message.class)) {
                messageMockedStatic.when(
                    () -> Message.creator(
                        accountSidStringArgumentCaptor.capture(), toPhoneNumberArgumentCaptor.capture(),
                        fromPhoneNumberArgumentCaptor.capture(), bodyStringArgumentCaptor.capture()))
                    .thenReturn(mockedMessageCreator);

                when(mockedMessageCreator.create())
                    .thenReturn(mockedMessage);

                assertEquals(
                    mockedMessage, TwilioSendSMSAction.perform(mockedParameters, mockedParameters, mockedContext));

                assertEquals("account_sid", accountSidStringArgumentCaptor.getValue());

                PhoneNumber toPhoneNumber = toPhoneNumberArgumentCaptor.getValue();

                assertEquals("+15592585054", toPhoneNumber.getEndpoint());

                PhoneNumber fromPhoneNumber = fromPhoneNumberArgumentCaptor.getValue();

                assertEquals("+15592582024", fromPhoneNumber.getEndpoint());

                assertEquals("body", bodyStringArgumentCaptor.getValue());
            }
        }
    }
}

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
import static com.bytechef.component.twilio.constant.TwilioConstants.ADDRESS_RETENTION;
import static com.bytechef.component.twilio.constant.TwilioConstants.APPLICATION_SID;
import static com.bytechef.component.twilio.constant.TwilioConstants.ATTEMPT;
import static com.bytechef.component.twilio.constant.TwilioConstants.BODY;
import static com.bytechef.component.twilio.constant.TwilioConstants.CONTENT_RETENTION;
import static com.bytechef.component.twilio.constant.TwilioConstants.CONTENT_SID;
import static com.bytechef.component.twilio.constant.TwilioConstants.CONTENT_VARIABLES;
import static com.bytechef.component.twilio.constant.TwilioConstants.DATE_TIME;
import static com.bytechef.component.twilio.constant.TwilioConstants.FORCE_DELIVERY;
import static com.bytechef.component.twilio.constant.TwilioConstants.FROM;
import static com.bytechef.component.twilio.constant.TwilioConstants.MAX_PRICE;
import static com.bytechef.component.twilio.constant.TwilioConstants.MEDIA_URL;
import static com.bytechef.component.twilio.constant.TwilioConstants.MESSAGING_SERVICE_SID;
import static com.bytechef.component.twilio.constant.TwilioConstants.PERSISTENT_ACTION;
import static com.bytechef.component.twilio.constant.TwilioConstants.PROVIDE_FEEDBACK;
import static com.bytechef.component.twilio.constant.TwilioConstants.RISK_CHECK;
import static com.bytechef.component.twilio.constant.TwilioConstants.SCHEDULE_TYPE;
import static com.bytechef.component.twilio.constant.TwilioConstants.SEND_AS_MMS;
import static com.bytechef.component.twilio.constant.TwilioConstants.SHORTEN_URLS;
import static com.bytechef.component.twilio.constant.TwilioConstants.SMART_ENCODED;
import static com.bytechef.component.twilio.constant.TwilioConstants.STATUS_CALLBACK;
import static com.bytechef.component.twilio.constant.TwilioConstants.TO;
import static com.bytechef.component.twilio.constant.TwilioConstants.VALIDITY_PERIOD;
import static com.bytechef.component.twilio.constant.TwilioConstants.ZONE_ID;
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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * @author Monika Domiter
 */
@SuppressFBWarnings("EC")
class TwilioSendSMSActionTest {

    private final ArgumentCaptor<String> accountSidStringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Message.AddressRetention> addressRetentionArgumentCaptor =
        ArgumentCaptor.forClass(Message.AddressRetention.class);
    private final ArgumentCaptor<Integer> attemptIntegerArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    private final ArgumentCaptor<String> applicationSidStringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> bodyStringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> contentSidStringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<String> contentVariablesStringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Message.ContentRetention> contentRetentionArgumentCaptor =
        ArgumentCaptor.forClass(Message.ContentRetention.class);
    private final ArgumentCaptor<Boolean> forceDeliveryBooleanArgumentCaptor = ArgumentCaptor.forClass(Boolean.class);
    private final ArgumentCaptor<PhoneNumber> fromPhoneNumberArgumentCaptor = ArgumentCaptor.forClass(
        PhoneNumber.class);
    @SuppressWarnings("rawtypes")
    private final ArgumentCaptor<List> listArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private final ArgumentCaptor<BigDecimal> maxPriceBigDecimalArgumentCaptor =
        ArgumentCaptor.forClass(BigDecimal.class);
    private final ArgumentCaptor<String> messagingServidSIDStringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final Parameters mockedParameters = mock(Parameters.class);
    private final ActionContext mockedContext = mock(ActionContext.class);
    private final Message mockedMessage = mock(Message.class);
    private final MessageCreator mockedMessageCreator = mock(MessageCreator.class);
    private final List<String> persistentActionList = List.of("a", "b");
    @SuppressWarnings("rawtypes")

    private final ArgumentCaptor<List> persistentActionListArgumentCaptor = ArgumentCaptor.forClass(List.class);
    private final ArgumentCaptor<Boolean> provideFeedbackBooleanArgumentCaptor = ArgumentCaptor.forClass(Boolean.class);
    private final ArgumentCaptor<Message.RiskCheck> riskCheckArgumentCaptor =
        ArgumentCaptor.forClass(Message.RiskCheck.class);
    private final ArgumentCaptor<Message.ScheduleType> scheduleTypeArgumentCaptor =
        ArgumentCaptor.forClass(Message.ScheduleType.class);
    private final ArgumentCaptor<Boolean> sendAsMMSBooleanArgumentCaptor = ArgumentCaptor.forClass(Boolean.class);
    private final ArgumentCaptor<Boolean> shortenUrlsBooleanArgumentCaptor = ArgumentCaptor.forClass(Boolean.class);
    private final ArgumentCaptor<Boolean> smartEncodedBooleanArgumentCaptor = ArgumentCaptor.forClass(Boolean.class);
    private final ArgumentCaptor<String> statusCallbackStringArgumentCaptor = ArgumentCaptor.forClass(String.class);
    private final ArgumentCaptor<Integer> validityPeriodIntegerArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
    private final ArgumentCaptor<PhoneNumber> toPhoneNumberArgumentCaptor = ArgumentCaptor.forClass(PhoneNumber.class);
    private final ArgumentCaptor<ZonedDateTime> zonedDateTimeArgumentCaptor =
        ArgumentCaptor.forClass(ZonedDateTime.class);
    private MockedStatic<Twilio> twilioMockedStatic;

    @BeforeEach
    public void beforeEach() {
        twilioMockedStatic = mockStatic(Twilio.class);

        twilioMockedStatic.when(
            () -> Twilio.init(
                mockedParameters.getRequiredString(USERNAME), mockedParameters.getRequiredString(PASSWORD)))
            .thenAnswer(Answers.RETURNS_DEFAULTS);

        when(mockedParameters.getRequiredString(USERNAME))
            .thenReturn("username");
        when(mockedParameters.getRequiredString(PASSWORD))
            .thenReturn("password");
        when(mockedParameters.getString(TO))
            .thenReturn("+15592585054");
        when(mockedParameters.getString(STATUS_CALLBACK))
            .thenReturn("statusCallback");
        when(mockedParameters.getString(APPLICATION_SID))
            .thenReturn("applicationSid");
        when(mockedParameters.get(MAX_PRICE, BigDecimal.class))
            .thenReturn(BigDecimal.valueOf(1.111));
        when(mockedParameters.getBoolean(PROVIDE_FEEDBACK))
            .thenReturn(true);
        when(mockedParameters.getInteger(ATTEMPT))
            .thenReturn(2);
        when(mockedParameters.getInteger(VALIDITY_PERIOD))
            .thenReturn(5);
        when(mockedParameters.getBoolean(FORCE_DELIVERY))
            .thenReturn(true);
        when(mockedParameters.getString(CONTENT_RETENTION))
            .thenReturn("discard");
        when(mockedParameters.getString(ADDRESS_RETENTION))
            .thenReturn("retain");
        when(mockedParameters.getBoolean(SMART_ENCODED))
            .thenReturn(true);
        when(mockedParameters.getList(PERSISTENT_ACTION, String.class, List.of()))
            .thenReturn(persistentActionList);
        when(mockedParameters.getBoolean(SHORTEN_URLS))
            .thenReturn(true);
        when(mockedParameters.getString(SCHEDULE_TYPE))
            .thenReturn("fixed");
        when(mockedParameters.getBoolean(SEND_AS_MMS))
            .thenReturn(true);
        when(mockedParameters.getString(CONTENT_VARIABLES))
            .thenReturn("contentVariables");
        when(mockedParameters.getString(RISK_CHECK))
            .thenReturn("disable");
        when(mockedParameters.getString(CONTENT_SID))
            .thenReturn("contentSid");
    }

    @AfterEach
    public void afterEach() {
        twilioMockedStatic.close();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testPerformFirstCaseForZoneIdNull() throws URISyntaxException {
        List<URI> uriList = List.of(new URI("fd"));

        when(mockedParameters.getString(MESSAGING_SERVICE_SID))
            .thenReturn("messaging_service_sid");
        when(mockedParameters.getList(MEDIA_URL, URI.class))
            .thenReturn(uriList);

        try (MockedStatic<Message> messageMockedStatic = mockStatic(Message.class)) {
            messageMockedStatic.when(
                () -> Message.creator(
                    toPhoneNumberArgumentCaptor.capture(), messagingServidSIDStringArgumentCaptor.capture(),
                    listArgumentCaptor.capture()))
                .thenReturn(mockedMessageCreator);

            mockMessageCreator();

            Map<String, Message> handleMap = TwilioSendSMSAction.perform(
                mockedParameters, mockedParameters, mockedContext);

            Message result = handleMap.get("message");

            assertEquals(mockedMessage, result);

            PhoneNumber toPhoneNumber = toPhoneNumberArgumentCaptor.getValue();

            assertEquals("+15592585054", toPhoneNumber.getEndpoint());

            assertEquals("messaging_service_sid", messagingServidSIDStringArgumentCaptor.getValue());
            assertEquals(uriList, listArgumentCaptor.getValue());

            testMessageCreatorFields();
        }
    }

    @Test
    void testPerformFirstCaseForZoneIdNotNull() throws URISyntaxException {
        when(mockedParameters.getString(ZONE_ID))
            .thenReturn("Asia/Tokyo");
        when(mockedParameters.getLocalDateTime(DATE_TIME))
            .thenReturn(LocalDateTime.of(2015, Month.APRIL, 1, 1, 1));

        testPerformFirstCaseForZoneIdNull();

        testMessageCreatorSendAtField();
    }

    @Disabled
    @Test
    @SuppressWarnings("unchecked")
    void testPerformSecondCaseForZoneIdNull() {
        List<URI> uriList = List.of();

        when(mockedParameters.getString(ACCOUNT_SID))
            .thenReturn("account_sid");
        when(mockedParameters.getString(MESSAGING_SERVICE_SID))
            .thenReturn("messaging_service_sid");
        when(mockedParameters.getList(MEDIA_URL, URI.class))
            .thenReturn(uriList);

        try (MockedStatic<Message> messageMockedStatic = mockStatic(Message.class)) {
            messageMockedStatic.when(
                () -> Message.creator(
                    accountSidStringArgumentCaptor.capture(), toPhoneNumberArgumentCaptor.capture(),
                    messagingServidSIDStringArgumentCaptor.capture(), listArgumentCaptor.capture()))
                .thenReturn(mockedMessageCreator);

            mockMessageCreator();

            assertEquals(
                mockedMessage, TwilioSendSMSAction.perform(mockedParameters, mockedParameters, mockedContext));

            String accountSid = accountSidStringArgumentCaptor.getValue();

            assertEquals("account_sid", accountSid);

            PhoneNumber toPhoneNumber = toPhoneNumberArgumentCaptor.getValue();

            assertEquals("+15592585054", toPhoneNumber.getEndpoint());

            assertEquals("messaging_service_sid", messagingServidSIDStringArgumentCaptor.getValue());
            assertEquals(uriList, listArgumentCaptor.getValue());

            testMessageCreatorFields();
        }
    }

    @Disabled
    @Test
    void testPerformSecondCaseForZoneIdNotNull() {
        when(mockedParameters.getString(ZONE_ID))
            .thenReturn("Asia/Tokyo");
        when(mockedParameters.getLocalDateTime(DATE_TIME))
            .thenReturn(LocalDateTime.of(2015, Month.APRIL, 1, 1, 1));

        testPerformSecondCaseForZoneIdNull();

        testMessageCreatorSendAtField();
    }

    @Disabled
    @Test
    void testPerformThirdCaseForZoneIdNull() {
        when(mockedParameters.getString(MESSAGING_SERVICE_SID))
            .thenReturn("messaging_service_sid");
        when(mockedParameters.getString(BODY))
            .thenReturn("body");

        try (MockedStatic<Message> messageMockedStatic = mockStatic(Message.class)) {
            messageMockedStatic.when(
                    () -> Message.creator(
                        toPhoneNumberArgumentCaptor.capture(), messagingServidSIDStringArgumentCaptor.capture(),
                        bodyStringArgumentCaptor.capture()))
                .thenReturn(mockedMessageCreator);

            mockMessageCreator();

            assertEquals(
                mockedMessage, TwilioSendSMSAction.perform(mockedParameters, mockedParameters, mockedContext));

            PhoneNumber toPhoneNumber = toPhoneNumberArgumentCaptor.getValue();

            assertEquals("+15592585054", toPhoneNumber.getEndpoint());

            assertEquals("messaging_service_sid", messagingServidSIDStringArgumentCaptor.getValue());
            assertEquals("body", bodyStringArgumentCaptor.getValue());

            testMessageCreatorFields();
        }
    }

    @Disabled
    @Test
    void testPerformThirdCaseForZoneIdNotNull() {
        when(mockedParameters.getString(ZONE_ID))
            .thenReturn("Asia/Tokyo");
        when(mockedParameters.getLocalDateTime(DATE_TIME))
            .thenReturn(LocalDateTime.of(2015, Month.APRIL, 1, 1, 1));

        testPerformThirdCaseForZoneIdNull();

        testMessageCreatorSendAtField();
    }

    @Disabled
    @Test
    void testPerformFourthCaseForZoneIdNull() {
        when(mockedParameters.getString(ACCOUNT_SID))
            .thenReturn("account_sid");
        when(mockedParameters.getString(MESSAGING_SERVICE_SID))
            .thenReturn("messaging_service_sid");
        when(mockedParameters.getString(BODY))
            .thenReturn("body");

        try (MockedStatic<Message> messageMockedStatic = mockStatic(Message.class)) {
            messageMockedStatic.when(
                    () -> Message.creator(
                        accountSidStringArgumentCaptor.capture(), toPhoneNumberArgumentCaptor.capture(),
                        messagingServidSIDStringArgumentCaptor.capture(), bodyStringArgumentCaptor.capture()))
                .thenReturn(mockedMessageCreator);

            mockMessageCreator();

            assertEquals(
                mockedMessage, TwilioSendSMSAction.perform(mockedParameters, mockedParameters, mockedContext));

            assertEquals("account_sid", accountSidStringArgumentCaptor.getValue());

            PhoneNumber toPhoneNumber = toPhoneNumberArgumentCaptor.getValue();

            assertEquals("+15592585054", toPhoneNumber.getEndpoint());

            assertEquals("messaging_service_sid", messagingServidSIDStringArgumentCaptor.getValue());
            assertEquals("body", bodyStringArgumentCaptor.getValue());

            testMessageCreatorFields();
        }
    }

    @Disabled
    @Test
    void testPerformFourthCaseForZoneIdNotNull() {
        when(mockedParameters.getString(ZONE_ID))
            .thenReturn("Asia/Tokyo");
        when(mockedParameters.getLocalDateTime(DATE_TIME))
            .thenReturn(LocalDateTime.of(2015, Month.APRIL, 1, 1, 1));

        testPerformFourthCaseForZoneIdNull();

        testMessageCreatorSendAtField();
    }

    @Disabled
    @Test
    @SuppressWarnings("unchecked")
    void testPerformFifthCaseForZoneIdNull() {
        List<URI> uriList = List.of();

        when(mockedParameters.getString(FROM))
            .thenReturn("+15592582024");
        when(mockedParameters.getList(MEDIA_URL, URI.class))
            .thenReturn(uriList);

        try (MockedStatic<Message> messageMockedStatic = mockStatic(Message.class)) {
            messageMockedStatic.when(
                () -> Message.creator(
                    toPhoneNumberArgumentCaptor.capture(), fromPhoneNumberArgumentCaptor.capture(),
                    listArgumentCaptor.capture()))
                .thenReturn(mockedMessageCreator);

            mockMessageCreator();

            assertEquals(
                mockedMessage, TwilioSendSMSAction.perform(mockedParameters, mockedParameters, mockedContext));

            PhoneNumber toPhoneNumber = toPhoneNumberArgumentCaptor.getValue();

            assertEquals("+15592585054", toPhoneNumber.getEndpoint());

            PhoneNumber fromPhoneNumber = fromPhoneNumberArgumentCaptor.getValue();

            assertEquals("+15592582024", fromPhoneNumber.getEndpoint());

            assertEquals(uriList, listArgumentCaptor.getValue());

            testMessageCreatorFields();
        }

    }

    @Disabled
    @Test
    void testPerformFifthCaseForZoneIdNotNull() {
        when(mockedParameters.getString(ZONE_ID))
            .thenReturn("Asia/Tokyo");
        when(mockedParameters.getLocalDateTime(DATE_TIME))
            .thenReturn(LocalDateTime.of(2015, Month.APRIL, 1, 1, 1));

        testPerformFifthCaseForZoneIdNull();

        testMessageCreatorSendAtField();
    }

    @Disabled
    @Test
    @SuppressWarnings("unchecked")
    void testPerformSixthCaseForZoneIdNull() {
        List<URI> uriList = List.of();

        when(mockedParameters.getString(ACCOUNT_SID))
            .thenReturn("account_sid");
        when(mockedParameters.getString(FROM))
            .thenReturn("+15592582024");
        when(mockedParameters.getList(MEDIA_URL, URI.class))
            .thenReturn(uriList);

        try (MockedStatic<Message> messageMockedStatic = mockStatic(Message.class)) {
            messageMockedStatic.when(
                () -> Message.creator(
                    accountSidStringArgumentCaptor.capture(), toPhoneNumberArgumentCaptor.capture(),
                    fromPhoneNumberArgumentCaptor.capture(), listArgumentCaptor.capture()))
                .thenReturn(mockedMessageCreator);

            mockMessageCreator();

            assertEquals(
                mockedMessage, TwilioSendSMSAction.perform(mockedParameters, mockedParameters, mockedContext));
            assertEquals("account_sid", accountSidStringArgumentCaptor.getValue());

            PhoneNumber toPhoneNumber = toPhoneNumberArgumentCaptor.getValue();

            assertEquals("+15592585054", toPhoneNumber.getEndpoint());

            PhoneNumber fromPhoneNumber = fromPhoneNumberArgumentCaptor.getValue();

            assertEquals("+15592582024", fromPhoneNumber.getEndpoint());

            assertEquals(uriList, listArgumentCaptor.getValue());

            testMessageCreatorFields();

        }
    }

    @Disabled
    @Test
    void testPerformSixthCaseForZoneIdNotNull() {
        when(mockedParameters.getString(ZONE_ID))
            .thenReturn("Asia/Tokyo");
        when(mockedParameters.getLocalDateTime(DATE_TIME))
            .thenReturn(LocalDateTime.of(2015, Month.APRIL, 1, 1, 1));

        testPerformSixthCaseForZoneIdNull();

        testMessageCreatorSendAtField();
    }

    @Disabled
    @Test
    void testPerformSeventhCaseForZoneIdNull() {
        when(mockedParameters.getString(FROM))
            .thenReturn("+15592582024");
        when(mockedParameters.getString(BODY))
            .thenReturn("body");

        try (MockedStatic<Message> messageMockedStatic = mockStatic(Message.class)) {
            messageMockedStatic.when(
                    () -> Message.creator(
                        toPhoneNumberArgumentCaptor.capture(), fromPhoneNumberArgumentCaptor.capture(),
                        bodyStringArgumentCaptor.capture()))
                .thenReturn(mockedMessageCreator);

            mockMessageCreator();

            assertEquals(
                mockedMessage, TwilioSendSMSAction.perform(mockedParameters, mockedParameters, mockedContext));

            PhoneNumber toPhoneNumber = toPhoneNumberArgumentCaptor.getValue();

            assertEquals("+15592585054", toPhoneNumber.getEndpoint());

            PhoneNumber fromPhoneNumber = fromPhoneNumberArgumentCaptor.getValue();

            assertEquals("+15592582024", fromPhoneNumber.getEndpoint());

            assertEquals("body", bodyStringArgumentCaptor.getValue());

            testMessageCreatorFields();
        }
    }

    @Disabled
    @Test
    void testPerformSeventhCaseForZoneIdNotNull() {
        when(mockedParameters.getString(ZONE_ID))
            .thenReturn("Asia/Tokyo");
        when(mockedParameters.getLocalDateTime(DATE_TIME))
            .thenReturn(LocalDateTime.of(2015, Month.APRIL, 1, 1, 1));

        testPerformSeventhCaseForZoneIdNull();

        testMessageCreatorSendAtField();
    }

    @Disabled
    @Test
    void testPerformEighthCaseForZoneIdNull() {
        when(mockedParameters.getString(ACCOUNT_SID))
            .thenReturn("account_sid");
        when(mockedParameters.getString(FROM))
            .thenReturn("+15592582024");
        when(mockedParameters.getString(BODY))
            .thenReturn("body");

        try (MockedStatic<Message> messageMockedStatic = mockStatic(Message.class)) {
            messageMockedStatic.when(
                    () -> Message.creator(
                        accountSidStringArgumentCaptor.capture(), toPhoneNumberArgumentCaptor.capture(),
                        fromPhoneNumberArgumentCaptor.capture(), bodyStringArgumentCaptor.capture()))
                .thenReturn(mockedMessageCreator);

            mockMessageCreator();

            assertEquals(
                mockedMessage, TwilioSendSMSAction.perform(mockedParameters, mockedParameters, mockedContext));

            assertEquals("account_sid", accountSidStringArgumentCaptor.getValue());

            PhoneNumber toPhoneNumber = toPhoneNumberArgumentCaptor.getValue();

            assertEquals("+15592585054", toPhoneNumber.getEndpoint());

            PhoneNumber fromPhoneNumber = fromPhoneNumberArgumentCaptor.getValue();

            assertEquals("+15592582024", fromPhoneNumber.getEndpoint());

            assertEquals("body", bodyStringArgumentCaptor.getValue());

            testMessageCreatorFields();
        }
    }

    @Disabled
    @Test
    void testPerformEighthCaseForZoneIdNotNull() {
        when(mockedParameters.getString(ZONE_ID))
            .thenReturn("Asia/Tokyo");
        when(mockedParameters.getLocalDateTime(DATE_TIME))
            .thenReturn(LocalDateTime.of(2015, Month.APRIL, 1, 1, 1));

        testPerformEighthCaseForZoneIdNull();

        assertEquals(ZoneId.of("Asia/Tokyo"), zonedDateTimeArgumentCaptor.getValue().getZone());
    }

    @SuppressWarnings("unchecked")
    private void mockMessageCreator() {
        when(mockedMessageCreator.setStatusCallback(statusCallbackStringArgumentCaptor.capture()))
            .thenReturn(mockedMessageCreator);
        when(mockedMessageCreator.setApplicationSid(applicationSidStringArgumentCaptor.capture()))
            .thenReturn(mockedMessageCreator);
        when(mockedMessageCreator.setMaxPrice(maxPriceBigDecimalArgumentCaptor.capture()))
            .thenReturn(mockedMessageCreator);
        when(mockedMessageCreator.setProvideFeedback(provideFeedbackBooleanArgumentCaptor.capture()))
            .thenReturn(mockedMessageCreator);
        when(mockedMessageCreator.setAttempt(attemptIntegerArgumentCaptor.capture()))
            .thenReturn(mockedMessageCreator);
        when(mockedMessageCreator.setValidityPeriod(validityPeriodIntegerArgumentCaptor.capture()))
            .thenReturn(mockedMessageCreator);
        when(mockedMessageCreator.setForceDelivery(forceDeliveryBooleanArgumentCaptor.capture()))
            .thenReturn(mockedMessageCreator);
        when(mockedMessageCreator.setContentRetention(contentRetentionArgumentCaptor.capture()))
            .thenReturn(mockedMessageCreator);
        when(mockedMessageCreator.setAddressRetention(addressRetentionArgumentCaptor.capture()))
            .thenReturn(mockedMessageCreator);
        when(mockedMessageCreator.setSmartEncoded(smartEncodedBooleanArgumentCaptor.capture()))
            .thenReturn(mockedMessageCreator);
        when(mockedMessageCreator.setPersistentAction(persistentActionListArgumentCaptor.capture()))
            .thenReturn(mockedMessageCreator);
        when(mockedMessageCreator.setShortenUrls(shortenUrlsBooleanArgumentCaptor.capture()))
            .thenReturn(mockedMessageCreator);
        when(mockedMessageCreator.setScheduleType(scheduleTypeArgumentCaptor.capture()))
            .thenReturn(mockedMessageCreator);
        when(mockedMessageCreator.setSendAsMms(sendAsMMSBooleanArgumentCaptor.capture()))
            .thenReturn(mockedMessageCreator);
        when(mockedMessageCreator.setContentVariables(contentVariablesStringArgumentCaptor.capture()))
            .thenReturn(mockedMessageCreator);
        when(mockedMessageCreator.setSendAt(zonedDateTimeArgumentCaptor.capture()))
            .thenReturn(mockedMessageCreator);
        when(mockedMessageCreator.setRiskCheck(riskCheckArgumentCaptor.capture()))
            .thenReturn(mockedMessageCreator);
        when(mockedMessageCreator.setContentSid(contentSidStringArgumentCaptor.capture()))
            .thenReturn(mockedMessageCreator);
        when(mockedMessageCreator.create())
            .thenReturn(mockedMessage);
    }

    private void testMessageCreatorFields() {
        assertEquals("statusCallback", statusCallbackStringArgumentCaptor.getValue());
        assertEquals("applicationSid", applicationSidStringArgumentCaptor.getValue());
        assertEquals(BigDecimal.valueOf(1.111), maxPriceBigDecimalArgumentCaptor.getValue());
        assertEquals(true, provideFeedbackBooleanArgumentCaptor.getValue());
        assertEquals(2, attemptIntegerArgumentCaptor.getValue());
        assertEquals(5, validityPeriodIntegerArgumentCaptor.getValue());
        assertEquals(true, forceDeliveryBooleanArgumentCaptor.getValue());
        assertEquals(Message.ContentRetention.forValue("discard"), contentRetentionArgumentCaptor.getValue());
        assertEquals(Message.AddressRetention.forValue("retain"), addressRetentionArgumentCaptor.getValue());
        assertEquals(true, smartEncodedBooleanArgumentCaptor.getValue());
        assertEquals(persistentActionList, persistentActionListArgumentCaptor.getValue());
        assertEquals(true, shortenUrlsBooleanArgumentCaptor.getValue());
        assertEquals(Message.ScheduleType.forValue("fixed"), scheduleTypeArgumentCaptor.getValue());
        assertEquals(true, sendAsMMSBooleanArgumentCaptor.getValue());
        assertEquals("contentVariables", contentVariablesStringArgumentCaptor.getValue());
        assertEquals(Message.RiskCheck.forValue("disable"), riskCheckArgumentCaptor.getValue());
        assertEquals("contentSid", contentSidStringArgumentCaptor.getValue());
    }

    private void testMessageCreatorSendAtField() {
        assertEquals(ZoneId.of("Asia/Tokyo"), zonedDateTimeArgumentCaptor.getValue()
            .getZone());
        assertEquals(2015, zonedDateTimeArgumentCaptor.getValue()
            .getYear());
        assertEquals(Month.APRIL, zonedDateTimeArgumentCaptor.getValue()
            .getMonth());
        assertEquals(1, zonedDateTimeArgumentCaptor.getValue()
            .getDayOfMonth());
        assertEquals(1, zonedDateTimeArgumentCaptor.getValue()
            .getHour());
        assertEquals(1, zonedDateTimeArgumentCaptor.getValue()
            .getMinute());
    }

}

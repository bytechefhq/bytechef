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

package com.bytechef.component.infobip.util;

import static com.bytechef.component.infobip.util.InfobipUtils.OffsetDateTimeCustom;
import static com.bytechef.component.infobip.util.InfobipUtils.SmsDeliveryTimeFromCustom;
import static com.bytechef.component.infobip.util.InfobipUtils.SmsDeliveryTimeToCustom;
import static com.bytechef.component.infobip.util.InfobipUtils.SmsDeliveryTimeWindowCustom;
import static com.bytechef.component.infobip.util.InfobipUtils.SmsDestinationCustom;
import static com.bytechef.component.infobip.util.InfobipUtils.SmsIndiaDltOptionsCustom;
import static com.bytechef.component.infobip.util.InfobipUtils.SmsLanguageCustom;
import static com.bytechef.component.infobip.util.InfobipUtils.SmsRegionalOptionsCustom;
import static com.bytechef.component.infobip.util.InfobipUtils.SmsSouthKoreaOptionsCustom;
import static com.bytechef.component.infobip.util.InfobipUtils.SmsTextualMessageCustom;
import static com.bytechef.component.infobip.util.InfobipUtils.SmsTurkeyIysOptionsCustom;
import static com.bytechef.component.infobip.util.InfobipUtils.createSmsTextualMessageList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.infobip.model.SmsDeliveryDay;
import com.infobip.model.SmsDeliveryTimeFrom;
import com.infobip.model.SmsDeliveryTimeTo;
import com.infobip.model.SmsDeliveryTimeWindow;
import com.infobip.model.SmsDestination;
import com.infobip.model.SmsIndiaDltOptions;
import com.infobip.model.SmsLanguage;
import com.infobip.model.SmsRegionalOptions;
import com.infobip.model.SmsTextualMessage;
import com.infobip.model.SmsTurkeyIysOptions;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class InfobipUtilsTest {

    @Test
    void testCreateSmsRegionalOptions() {
        List<SmsTextualMessageCustom> smsTextualMessageCustoms = List.of(
            new SmsTextualMessageCustom(
                "callbackData",
                new SmsDeliveryTimeWindowCustom(
                    List.of("MONDAY", "TUESDAY"),
                    new SmsDeliveryTimeFromCustom(1, 1),
                    new SmsDeliveryTimeToCustom(1, 1)),
                List.of(new SmsDestinationCustom("id", "to")),
                true,
                "from",
                true,
                new SmsLanguageCustom("TR"),
                "application/json",
                "notifyUrl",
                new SmsRegionalOptionsCustom(
                    new SmsIndiaDltOptionsCustom("contentTemplateId",
                        "principalEntityID"),
                    new SmsTurkeyIysOptionsCustom(1, "TACIR"),
                    new SmsSouthKoreaOptionsCustom(1)),
                new OffsetDateTimeCustom(LocalDateTime.of(2015, Month.APRIL, 1, 1, 1), "Asia/Tokyo"),
                "text",
                "TURKISH",
                123L,
                "entityId",
                "applicationId"));

        List<SmsTextualMessage> smsTextualMessages = createSmsTextualMessageList(smsTextualMessageCustoms);

        assertEquals(smsTextualMessageCustoms.size(), smsTextualMessages.size());

        SmsTextualMessageCustom smsTextualMessageCustom = smsTextualMessageCustoms.getFirst();
        SmsTextualMessage smsTextualMessage = smsTextualMessages.getFirst();

        assertEquals(smsTextualMessageCustom.callback(), smsTextualMessage.getCallbackData());

        testCreateSmsDeliveryTimeWindow(
            smsTextualMessageCustom.deliveryTimeWindowCustom(), smsTextualMessage.getDeliveryTimeWindow());

        testCreateSmsDestinationList(smsTextualMessageCustom.destinations(), smsTextualMessage.getDestinations());

        assertEquals(smsTextualMessageCustom.flash(), smsTextualMessage.getFlash());
        assertEquals(smsTextualMessageCustom.from(), smsTextualMessage.getFrom());
        assertEquals(smsTextualMessageCustom.intermediateReport(), smsTextualMessage.getIntermediateReport());

        testCreateSmsLanguage(smsTextualMessageCustom.language(), smsTextualMessage.getLanguage());

        assertEquals(smsTextualMessageCustom.notifyContentType(), smsTextualMessage.getNotifyContentType());
        assertEquals(smsTextualMessageCustom.notifyUrl(), smsTextualMessage.getNotifyUrl());

        testCreateSmsRegionalOptions(smsTextualMessageCustom.regionalOptionsCustom(), smsTextualMessage.getRegional());

        createOffsetDateTime(smsTextualMessageCustom.sendAtCustom(), smsTextualMessage.getSendAt());

        assertEquals(smsTextualMessageCustom.text(), smsTextualMessage.getText());
        assertEquals(smsTextualMessageCustom.transliteration(), smsTextualMessage.getTransliteration());
        assertEquals(smsTextualMessageCustom.validityPeriod(), smsTextualMessage.getValidityPeriod());
        assertEquals(smsTextualMessageCustom.entityId(), smsTextualMessage.getEntityId());
        assertEquals(smsTextualMessageCustom.applicationId(), smsTextualMessage.getApplicationId());
    }

    private static void testCreateSmsDeliveryTimeWindow(
        SmsDeliveryTimeWindowCustom smsDeliveryTimeWindowCustom,
        SmsDeliveryTimeWindow smsDeliveryTimeWindow) {

        List<String> daysString = smsDeliveryTimeWindowCustom.days();
        List<SmsDeliveryDay> smsDeliveryDays = smsDeliveryTimeWindow.getDays();

        assertEquals(daysString.size(), smsDeliveryDays.size());

        for (int i = 0; i < daysString.size(); i++) {
            assertEquals(daysString.get(i), smsDeliveryDays.get(i)
                .getValue());
        }

        SmsDeliveryTimeToCustom deliveryTimeToCustom = smsDeliveryTimeWindowCustom.to();
        SmsDeliveryTimeTo deliveryTimeTo = smsDeliveryTimeWindow.getTo();

        assertEquals(deliveryTimeToCustom.hour(), deliveryTimeTo.getHour());
        assertEquals(deliveryTimeToCustom.minute(), deliveryTimeTo.getMinute());

        SmsDeliveryTimeFromCustom deliveryTimeFromCustom = smsDeliveryTimeWindowCustom.from();

        SmsDeliveryTimeFrom deliveryTimeFrom = smsDeliveryTimeWindow.getFrom();
        assertEquals(deliveryTimeFromCustom.hour(), deliveryTimeFrom.getHour());
        assertEquals(deliveryTimeFromCustom.minute(), deliveryTimeFrom.getMinute());
    }

    private static void testCreateSmsDestinationList(
        List<SmsDestinationCustom> smsDestinationCustoms, List<SmsDestination> smsDestinations) {

        assertEquals(smsDestinationCustoms.size(), smsDestinations.size());
        for (int i = 0; i < smsDestinationCustoms.size(); i++) {
            SmsDestinationCustom smsDestinationCustom = smsDestinationCustoms.get(i);
            SmsDestination smsDestination = smsDestinations.get(i);

            assertEquals(smsDestinationCustom.messageId(), smsDestination.getMessageId());
            assertEquals(smsDestinationCustom.to(), smsDestination.getTo());
        }
    }

    private static void
        testCreateSmsLanguage(SmsLanguageCustom smsLanguageCustom, SmsLanguage smsLanguage) {
        assertEquals(smsLanguageCustom.languageCode(), smsLanguage.getLanguageCode());
    }

    private static void testCreateSmsRegionalOptions(
        SmsRegionalOptionsCustom smsRegionalOptionsCustom, SmsRegionalOptions smsRegionalOptions) {

        SmsIndiaDltOptionsCustom smsIndiaDltOptionsCustom = smsRegionalOptionsCustom.indiaDlt();
        SmsIndiaDltOptions indiaDlt = smsRegionalOptions.getIndiaDlt();

        assertEquals(smsIndiaDltOptionsCustom.contentTemplateId(), indiaDlt.getContentTemplateId());
        assertEquals(smsIndiaDltOptionsCustom.principalEntityId(), indiaDlt.getPrincipalEntityId());

        SmsTurkeyIysOptionsCustom smsTurkeyIysOptionsCustom = smsRegionalOptionsCustom.turkeyIys();
        SmsTurkeyIysOptions turkeyIys = smsRegionalOptions.getTurkeyIys();

        assertEquals(smsTurkeyIysOptionsCustom.brandCode(), turkeyIys.getBrandCode());
        assertEquals(smsTurkeyIysOptionsCustom.recipientType(), turkeyIys.getRecipientType()
            .getValue());
        assertEquals(smsRegionalOptionsCustom.southKorea()
            .resellerCode(),
            smsRegionalOptions.getSouthKorea()
                .getResellerCode());
    }

    private static void createOffsetDateTime(
        OffsetDateTimeCustom offsetDateTimeCustom, OffsetDateTime offsetDateTime) {
        assertEquals(offsetDateTimeCustom.dateTime(), offsetDateTime.toLocalDateTime());
    }
}

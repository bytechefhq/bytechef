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

import com.infobip.model.SmsDeliveryDay;
import com.infobip.model.SmsDeliveryTimeFrom;
import com.infobip.model.SmsDeliveryTimeTo;
import com.infobip.model.SmsDeliveryTimeWindow;
import com.infobip.model.SmsDestination;
import com.infobip.model.SmsIndiaDltOptions;
import com.infobip.model.SmsLanguage;
import com.infobip.model.SmsRegionalOptions;
import com.infobip.model.SmsSouthKoreaOptions;
import com.infobip.model.SmsTextualMessage;
import com.infobip.model.SmsTurkeyIysOptions;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Monika Domiter
 */
public class InfobipUtils {

    public static List<SmsTextualMessage> createSmsTextualMessageList(
        List<SmsTextualMessageCustom> smsTextualMessageCustoms) {

        List<SmsTextualMessage> smsTextualMessages = new ArrayList<>();

        for (InfobipUtils.SmsTextualMessageCustom smsTextualMessageCustom : smsTextualMessageCustoms) {

            SmsTextualMessage smsTextualMessage = new SmsTextualMessage()
                .callbackData(smsTextualMessageCustom.callback())
                .deliveryTimeWindow(createSmsDeliveryTimeWindow(smsTextualMessageCustom.deliveryTimeWindowCustom()))
                .destinations(createSmsDestinationList(smsTextualMessageCustom.destinations()))
                .flash(smsTextualMessageCustom.flash())
                .from(smsTextualMessageCustom.from())
                .intermediateReport(smsTextualMessageCustom.intermediateReport())
                .language(createSmsLanguage(smsTextualMessageCustom.language()))
                .notifyContentType(smsTextualMessageCustom.notifyContentType())
                .notifyUrl(smsTextualMessageCustom.notifyUrl())
                .regional(createSmsRegionalOptions(smsTextualMessageCustom.regionalOptionsCustom()))
                .sendAt(createOffsetDateTime(smsTextualMessageCustom.sendAtCustom()))
                .text(smsTextualMessageCustom.text())
                .transliteration(smsTextualMessageCustom.transliteration())
                .validityPeriod(smsTextualMessageCustom.validityPeriod())
                .entityId(smsTextualMessageCustom.entityId())
                .applicationId(smsTextualMessageCustom.applicationId());

            smsTextualMessages.add(smsTextualMessage);
        }

        return smsTextualMessages;
    }

    private static SmsDeliveryTimeWindow createSmsDeliveryTimeWindow(
        SmsDeliveryTimeWindowCustom smsDeliveryTimeWindowCustom) {

        SmsDeliveryTimeFromCustom deliveryTimeFromCustom = smsDeliveryTimeWindowCustom.from();

        SmsDeliveryTimeToCustom smsDeliveryTimeToCustom = smsDeliveryTimeWindowCustom.to();

        return new SmsDeliveryTimeWindow()
            .days(createSmsDeliveryDayList(smsDeliveryTimeWindowCustom.days()))
            .from(new SmsDeliveryTimeFrom()
                .hour(deliveryTimeFromCustom.hour())
                .minute(deliveryTimeFromCustom.minute()))
            .to(new SmsDeliveryTimeTo()
                .hour(smsDeliveryTimeToCustom.hour())
                .minute(smsDeliveryTimeToCustom.minute()));
    }

    private static List<SmsDestination> createSmsDestinationList(List<SmsDestinationCustom> smsDestinationCustoms) {
        List<SmsDestination> smsDestinations = new ArrayList<>();

        for (SmsDestinationCustom smsDestinationCustom : smsDestinationCustoms) {
            SmsDestination smsDestination =
                new SmsDestination()
                    .messageId(smsDestinationCustom.messageId())
                    .to(smsDestinationCustom.to());

            smsDestinations.add(smsDestination);

        }
        return smsDestinations;
    }

    private static SmsLanguage createSmsLanguage(SmsLanguageCustom smsLanguageCustom) {
        return new SmsLanguage().languageCode(smsLanguageCustom.languageCode());
    }

    private static SmsRegionalOptions createSmsRegionalOptions(SmsRegionalOptionsCustom smsRegionalOptionsCustom) {
        SmsRegionalOptions smsRegionalOptions = new SmsRegionalOptions();

        SmsIndiaDltOptionsCustom smsIndiaDltOptionsCustom = smsRegionalOptionsCustom.indiaDlt();

        smsRegionalOptions.indiaDlt(
            new SmsIndiaDltOptions()
                .principalEntityId(smsIndiaDltOptionsCustom.principalEntityId())
                .contentTemplateId(smsIndiaDltOptionsCustom.contentTemplateId()));

        SmsTurkeyIysOptionsCustom smsTurkeyIysOptionsCustom = smsRegionalOptionsCustom.turkeyIys();

        smsRegionalOptions.turkeyIys(
            new SmsTurkeyIysOptions()
                .brandCode(smsTurkeyIysOptionsCustom.brandCode())
                .recipientType(
                    SmsTurkeyIysOptions.RecipientTypeEnum.fromValue(smsTurkeyIysOptionsCustom.recipientType())));

        smsRegionalOptions.southKorea(
            new SmsSouthKoreaOptions()
                .resellerCode(
                    smsRegionalOptionsCustom.southKorea()
                        .resellerCode()));

        return smsRegionalOptions;
    }

    private static OffsetDateTime createOffsetDateTime(OffsetDateTimeCustom offsetDateTimeCustom) {
        LocalDateTime localDateTime = offsetDateTimeCustom.dateTime();

        ZoneOffset zoneOffSet = ZoneId.of(offsetDateTimeCustom.zoneId())
            .getRules()
            .getOffset(localDateTime);

        return localDateTime.atOffset(zoneOffSet);
    }

    private static List<SmsDeliveryDay> createSmsDeliveryDayList(List<String> days) {
        List<SmsDeliveryDay> smsDeliveryDays = new ArrayList<>();

        for (String day : days) {
            smsDeliveryDays.add(SmsDeliveryDay.fromValue(day));
        }

        return smsDeliveryDays;
    }

    public record SmsTextualMessageCustom(String callback, SmsDeliveryTimeWindowCustom deliveryTimeWindowCustom,
        List<SmsDestinationCustom> destinations, Boolean flash, String from,
        Boolean intermediateReport, SmsLanguageCustom language,
        String notifyContentType, String notifyUrl,
        SmsRegionalOptionsCustom regionalOptionsCustom,
        OffsetDateTimeCustom sendAtCustom, String text, String transliteration,
        Long validityPeriod, String entityId, String applicationId) {

        public SmsTextualMessageCustom(String callback,
            SmsDeliveryTimeWindowCustom deliveryTimeWindowCustom,
            List<SmsDestinationCustom> destinations,
            Boolean flash,
            String from,
            Boolean intermediateReport,
            SmsLanguageCustom language,
            String notifyContentType,
            String notifyUrl,
            SmsRegionalOptionsCustom regionalOptionsCustom,
            OffsetDateTimeCustom sendAtCustom,
            String text,
            String transliteration,
            Long validityPeriod,
            String entityId,
            String applicationId) {

            this.callback = callback;
            this.deliveryTimeWindowCustom = deliveryTimeWindowCustom;
            this.destinations = Collections.unmodifiableList(destinations);
            this.flash = flash;
            this.from = from;
            this.intermediateReport = intermediateReport;
            this.language = language;
            this.notifyContentType = notifyContentType;
            this.notifyUrl = notifyUrl;
            this.regionalOptionsCustom = regionalOptionsCustom;
            this.sendAtCustom = sendAtCustom;
            this.text = text;
            this.transliteration = transliteration;
            this.validityPeriod = validityPeriod;
            this.entityId = entityId;
            this.applicationId = applicationId;
        }

        @Override
        public List<SmsDestinationCustom> destinations() {
            return Collections.unmodifiableList(destinations);
        }
    }

    protected record SmsDeliveryTimeWindowCustom(
        List<String> days, SmsDeliveryTimeFromCustom from, SmsDeliveryTimeToCustom to) {
        protected SmsDeliveryTimeWindowCustom(List<String> days, SmsDeliveryTimeFromCustom from,
            SmsDeliveryTimeToCustom to) {
            this.days = Collections.unmodifiableList(days);
            this.from = from;
            this.to = to;
        }
    }

    protected record SmsDeliveryTimeFromCustom(Integer hour, Integer minute) {
    }

    protected record SmsDeliveryTimeToCustom(Integer hour, Integer minute) {
    }

    protected record SmsRegionalOptionsCustom(
        SmsIndiaDltOptionsCustom indiaDlt, SmsTurkeyIysOptionsCustom turkeyIys, SmsSouthKoreaOptionsCustom southKorea) {
    }

    protected record SmsIndiaDltOptionsCustom(String contentTemplateId, String principalEntityId) {
    }

    protected record SmsSouthKoreaOptionsCustom(Integer resellerCode) {
    }

    protected record SmsTurkeyIysOptionsCustom(Integer brandCode, String recipientType) {
    }

    protected record OffsetDateTimeCustom(LocalDateTime dateTime, String zoneId) {
    }

    protected record SmsLanguageCustom(String languageCode) {
    }

    protected record SmsDestinationCustom(String messageId, String to) {
    }
}

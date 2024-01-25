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

package com.bytechef.component.microsoft.outlook.util;

import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.BCC_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.BODY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.BODY_PREVIEW;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CC_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.COMPLETED_DATE_TIME;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONVERSATION_ID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.DUE_DATE_TIME;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FLAG_STATUS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FROM;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.HAS_ATTACHMENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.IMPORTANCE;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.INFERENCE_CLASSIFICATION;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.INTERNET_MESSAGE_HEADERS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.INTERNET_MESSAGE_ID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.IS_DELIVERY_RECEIPT_REQUESTED;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.IS_DRAFT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.IS_READ;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.IS_READ_RECEIPT_REQUESTED;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.PARENT_FOLDER_ID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.RECEIVED_DATE_TIME;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.REPLY_TO;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SENDER;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SENT_DATE_TIME;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.START_DATE_TIME;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SUBJECT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.TO_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.UNIQUE_BODY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.WEB_LINK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Parameters;
import com.microsoft.graph.models.DateTimeTimeZone;
import com.microsoft.graph.models.EmailAddress;
import com.microsoft.graph.models.InternetMessageHeader;
import com.microsoft.graph.models.ItemBody;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.Recipient;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Monika Domiter
 */
class MicrosoftOutlook365UtilsTest {

    private final Parameters mockedParameters = mock(Parameters.class);

    private static final MicrosoftOutlook365Utils.DateTimeCustom dateTimeCustom =
        new MicrosoftOutlook365Utils.DateTimeCustom(LocalDateTime.of(2015, 1, 1, 1, 1), "Pacific/Midway");
    private static final MicrosoftOutlook365Utils.ItemBodyCustom itemBodyCustom =
        new MicrosoftOutlook365Utils.ItemBodyCustom("content", "TEXT");

    private static final MicrosoftOutlook365Utils.OffsetDateTimeCustom offsetDateTimeCustom =
        new MicrosoftOutlook365Utils.OffsetDateTimeCustom(LocalDateTime.of(2015, 1, 1, 1, 1), "Pacific/Midway");

    @Test
    void testCreateMessage() {

        EmailAddress emailAddress = new EmailAddress();

        emailAddress.address = "email@email.com";
        emailAddress.name = "name";

        Recipient recipient = new Recipient();
        recipient.emailAddress = emailAddress;

        List<Recipient> recipients = List.of(recipient);

        InternetMessageHeader internetMessageHeader = new InternetMessageHeader();

        internetMessageHeader.name = "name";
        internetMessageHeader.value = "value";

        List<InternetMessageHeader> internetMessageHeaders = List.of(internetMessageHeader);

        when(mockedParameters.getList(BCC_RECIPIENTS, Recipient.class, List.of()))
            .thenReturn(recipients);
        when(mockedParameters.get(BODY, MicrosoftOutlook365Utils.ItemBodyCustom.class))
            .thenReturn(itemBodyCustom);
        when(mockedParameters.getString(BODY_PREVIEW))
            .thenReturn("bodyPreview");
        when(mockedParameters.getList(CC_RECIPIENTS, Recipient.class, List.of()))
            .thenReturn(recipients);
        when(mockedParameters.getString(CONVERSATION_ID))
            .thenReturn("conversationId");
        when(mockedParameters.get(COMPLETED_DATE_TIME, MicrosoftOutlook365Utils.DateTimeCustom.class))
            .thenReturn(dateTimeCustom);
        when(mockedParameters.get(DUE_DATE_TIME, MicrosoftOutlook365Utils.DateTimeCustom.class))
            .thenReturn(dateTimeCustom);
        when(mockedParameters.getString(FLAG_STATUS))
            .thenReturn("COMPLETE");
        when(mockedParameters.get(START_DATE_TIME, MicrosoftOutlook365Utils.DateTimeCustom.class))
            .thenReturn(dateTimeCustom);
        when(mockedParameters.get(FROM, Recipient.class))
            .thenReturn(recipient);
        when(mockedParameters.getBoolean(HAS_ATTACHMENTS))
            .thenReturn(true);
        when(mockedParameters.getString(IMPORTANCE))
            .thenReturn("HIGH");
        when(mockedParameters.getString(INFERENCE_CLASSIFICATION))
            .thenReturn("FOCUSED");
        when(mockedParameters.getList(INTERNET_MESSAGE_HEADERS, InternetMessageHeader.class, List.of()))
            .thenReturn(internetMessageHeaders);
        when(mockedParameters.getString(INTERNET_MESSAGE_ID))
            .thenReturn("internetMessageId");
        when(mockedParameters.getBoolean(IS_DELIVERY_RECEIPT_REQUESTED))
            .thenReturn(false);
        when(mockedParameters.getBoolean(IS_DRAFT))
            .thenReturn(false);
        when(mockedParameters.getBoolean(IS_READ))
            .thenReturn(false);
        when(mockedParameters.getBoolean(IS_READ_RECEIPT_REQUESTED))
            .thenReturn(false);
        when(mockedParameters.getString(PARENT_FOLDER_ID))
            .thenReturn("parentFolderId");
        when(mockedParameters.get(RECEIVED_DATE_TIME, MicrosoftOutlook365Utils.OffsetDateTimeCustom.class))
            .thenReturn(offsetDateTimeCustom);
        when(mockedParameters.getList(REPLY_TO, Recipient.class, List.of()))
            .thenReturn(recipients);
        when(mockedParameters.get(SENDER, Recipient.class))
            .thenReturn(recipient);
        when(mockedParameters.get(SENT_DATE_TIME, MicrosoftOutlook365Utils.OffsetDateTimeCustom.class))
            .thenReturn(offsetDateTimeCustom);
        when(mockedParameters.getString(SUBJECT))
            .thenReturn("subject");
        when(mockedParameters.getList(TO_RECIPIENTS, Recipient.class, List.of()))
            .thenReturn(recipients);
        when(mockedParameters.get(UNIQUE_BODY, MicrosoftOutlook365Utils.ItemBodyCustom.class))
            .thenReturn(itemBodyCustom);
        when(mockedParameters.getString(WEB_LINK))
            .thenReturn("webLink");

        Message message = MicrosoftOutlook365Utils.createMessage(mockedParameters);

        assertEquals(recipients, message.bccRecipients);
        assertEquals("bodyPreview", message.bodyPreview);
        assertEquals(recipients, message.ccRecipients);
        assertEquals("conversationId", message.conversationId);
        assertEquals("COMPLETE", message.flag.flagStatus.toString());
        assertEquals(recipient, message.from);
        assertEquals(true, message.hasAttachments);
        assertEquals("HIGH", message.importance.toString());
        assertEquals("FOCUSED", message.inferenceClassification.toString());
        assertEquals(internetMessageHeaders, message.internetMessageHeaders);
        assertEquals("internetMessageId", message.internetMessageId);
        assertEquals(false, message.isDeliveryReceiptRequested);
        assertEquals(false, message.isDraft);
        assertEquals(false, message.isRead);
        assertEquals(false, message.isReadReceiptRequested);
        assertEquals("parentFolderId", message.parentFolderId);
        assertEquals(recipients, message.replyTo);
        assertEquals(recipient, message.sender);
        assertEquals("subject", message.subject);
        assertEquals(recipients, message.toRecipients);
        assertEquals("webLink", message.webLink);

        testItemBody(message.body);
        testItemBody(message.uniqueBody);

        testDateTime(message.flag.completedDateTime);
        testDateTime(message.flag.dueDateTime);
        testDateTime(message.flag.startDateTime);

        testOffsetDateTime(message.receivedDateTime);
        testOffsetDateTime(message.sentDateTime);
    }

    private static void testOffsetDateTime(OffsetDateTime offsetDateTime) {
        LocalDateTime dateTime = offsetDateTimeCustom.dateTime();

        assertEquals(dateTime, offsetDateTime.toLocalDateTime());
        assertEquals(ZoneId.of(offsetDateTimeCustom.zoneId())
            .getRules()
            .getOffset(dateTime), offsetDateTime.getOffset());
    }

    private static void testDateTime(DateTimeTimeZone dateTimeTimeZone) {
        assertEquals(dateTimeCustom.dateTime()
            .toString(), dateTimeTimeZone.dateTime);
        assertEquals(dateTimeCustom.timeZone(), dateTimeTimeZone.timeZone);
    }

    private static void testItemBody(ItemBody itemBody) {
        assertEquals(itemBodyCustom.content(), itemBody.content);
        assertEquals(itemBodyCustom.contentType(), itemBody.contentType.toString());
    }
}

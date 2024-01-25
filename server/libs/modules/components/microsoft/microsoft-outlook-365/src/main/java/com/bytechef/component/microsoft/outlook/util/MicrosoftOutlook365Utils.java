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

import com.azure.identity.AuthorizationCodeCredential;
import com.azure.identity.AuthorizationCodeCredentialBuilder;
import com.bytechef.component.definition.Parameters;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.BodyType;
import com.microsoft.graph.models.DateTimeTimeZone;
import com.microsoft.graph.models.FollowupFlag;
import com.microsoft.graph.models.FollowupFlagStatus;
import com.microsoft.graph.models.Importance;
import com.microsoft.graph.models.InferenceClassificationType;
import com.microsoft.graph.models.InternetMessageHeader;
import com.microsoft.graph.models.ItemBody;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.Recipient;
import com.microsoft.graph.requests.GraphServiceClient;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

/**
 * @author Monika Domiter
 */
public class MicrosoftOutlook365Utils {

    @SuppressWarnings("rawtypes")
    public static GraphServiceClient getGraphServiceClient() {

        // TODO

        final String clientId = "YOUR_CLIENT_ID";
        final String tenantId = "YOUR_TENANT_ID"; // or "common" for multi-tenant apps
        final String clientSecret = "YOUR_CLIENT_SECRET";
        final String authorizationCode = "AUTH_CODE_FROM_REDIRECT";
        final String redirectUrl = "YOUR_REDIRECT_URI";
        final List<String> scopes = Arrays.asList("User.Read");

        final AuthorizationCodeCredential credential = new AuthorizationCodeCredentialBuilder()
            .clientId(clientId)
            .tenantId(tenantId)
            .clientSecret(clientSecret)
            .authorizationCode(authorizationCode)
            .redirectUrl(redirectUrl)
            .build();

        TokenCredentialAuthProvider authProvider = new TokenCredentialAuthProvider(scopes, credential);

        return GraphServiceClient.builder()
            .authenticationProvider(authProvider)
            .buildClient();
    }

    public static Message createMessage(Parameters inputParameters) {
        Message message = new Message();

        message.bccRecipients = inputParameters.getList(BCC_RECIPIENTS, Recipient.class, List.of());
        message.body = createItemBody(inputParameters.get(BODY, ItemBodyCustom.class));
        message.bodyPreview = inputParameters.getString(BODY_PREVIEW);
        message.ccRecipients = inputParameters.getList(CC_RECIPIENTS, Recipient.class, List.of());
        message.conversationId = inputParameters.getString(CONVERSATION_ID);
        message.flag = createFollowupFlag(inputParameters);
        message.from = inputParameters.get(FROM, Recipient.class);
        message.hasAttachments = inputParameters.getBoolean(HAS_ATTACHMENTS);

        if (inputParameters.getString(IMPORTANCE) != null) {
            message.importance = Importance.valueOf(inputParameters.getString(IMPORTANCE));
        }

        if (inputParameters.getString(INFERENCE_CLASSIFICATION) != null) {
            message.inferenceClassification =
                InferenceClassificationType.valueOf(inputParameters.getString(INFERENCE_CLASSIFICATION));
        }

        message.internetMessageHeaders =
            inputParameters.getList(INTERNET_MESSAGE_HEADERS, InternetMessageHeader.class, List.of());
        message.internetMessageId = inputParameters.getString(INTERNET_MESSAGE_ID);
        message.isDeliveryReceiptRequested = inputParameters.getBoolean(IS_DELIVERY_RECEIPT_REQUESTED);
        message.isDraft = inputParameters.getBoolean(IS_DRAFT);
        message.isRead = inputParameters.getBoolean(IS_READ);
        message.isReadReceiptRequested = inputParameters.getBoolean(IS_READ_RECEIPT_REQUESTED);
        message.parentFolderId = inputParameters.getString(PARENT_FOLDER_ID);
        message.receivedDateTime =
            createOffsetDateTime(inputParameters.get(RECEIVED_DATE_TIME, OffsetDateTimeCustom.class));
        message.replyTo = inputParameters.getList(REPLY_TO, Recipient.class, List.of());
        message.sender = inputParameters.get(SENDER, Recipient.class);
        message.sentDateTime = createOffsetDateTime(inputParameters.get(SENT_DATE_TIME, OffsetDateTimeCustom.class));
        message.subject = inputParameters.getString(SUBJECT);
        message.toRecipients = inputParameters.getList(TO_RECIPIENTS, Recipient.class, List.of());
        message.uniqueBody = createItemBody(inputParameters.get(UNIQUE_BODY, ItemBodyCustom.class));
        message.webLink = inputParameters.getString(WEB_LINK);

        return message;
    }

    private static FollowupFlag createFollowupFlag(Parameters inputParameters) {
        FollowupFlag followupFlag = new FollowupFlag();

        followupFlag.completedDateTime =
            createDateTimeTimeZone(inputParameters.get(COMPLETED_DATE_TIME, DateTimeCustom.class));
        followupFlag.dueDateTime = createDateTimeTimeZone(inputParameters.get(DUE_DATE_TIME, DateTimeCustom.class));

        if (inputParameters.getString(FLAG_STATUS) != null) {
            followupFlag.flagStatus = FollowupFlagStatus.valueOf(inputParameters.getString(FLAG_STATUS));
        }

        followupFlag.startDateTime = createDateTimeTimeZone(inputParameters.get(START_DATE_TIME, DateTimeCustom.class));

        return followupFlag;
    }

    private static ItemBody createItemBody(ItemBodyCustom itemBodyCustom) {

        if (itemBodyCustom == null) {
            return null;
        }

        ItemBody itemBody = new ItemBody();

        itemBody.content = itemBodyCustom.content();
        itemBody.contentType = BodyType.valueOf(itemBodyCustom.contentType);

        return itemBody;
    }

    private static DateTimeTimeZone createDateTimeTimeZone(DateTimeCustom dateTimeCustom) {

        if (dateTimeCustom == null) {
            return null;
        }

        DateTimeTimeZone dateTimeTimeZone = new DateTimeTimeZone();

        dateTimeTimeZone.dateTime = dateTimeCustom.dateTime()
            .toString();
        dateTimeTimeZone.timeZone = dateTimeCustom.timeZone();

        return dateTimeTimeZone;
    }

    private static OffsetDateTime createOffsetDateTime(OffsetDateTimeCustom offsetDateTimeCustom) {
        LocalDateTime localDateTime = offsetDateTimeCustom.dateTime();

        ZoneOffset zoneOffSet = ZoneId.of(offsetDateTimeCustom.zoneId())
            .getRules()
            .getOffset(localDateTime);

        return localDateTime.atOffset(zoneOffSet);
    }

    protected record DateTimeCustom(LocalDateTime dateTime, String timeZone) {
    }

    protected record ItemBodyCustom(String content, String contentType) {
    }

    protected record OffsetDateTimeCustom(LocalDateTime dateTime, String zoneId) {
    }
}

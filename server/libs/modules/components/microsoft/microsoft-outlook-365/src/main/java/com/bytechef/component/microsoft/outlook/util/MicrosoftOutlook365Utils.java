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

package com.bytechef.component.microsoft.outlook.util;

import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ADDRESS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.BCC_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.BODY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CC_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONTENT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CONTENT_BYTES;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.EMAIL_ADDRESS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FROM;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.NAME;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ODATA_NEXT_LINK;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SUBJECT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.TO_RECIPIENTS;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.VALUE;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.TypeReference;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftOutlook365Utils {

    private MicrosoftOutlook365Utils() {
    }

    public static SimpleMessage createSimpleMessage(Context context, Map<?, ?> messageBody, String id) {
        String from = null;
        if (messageBody.get(FROM) instanceof Map<?, ?> fromMap &&
            fromMap.get(EMAIL_ADDRESS) instanceof Map<?, ?> emailAddressMap) {

            from = (String) emailAddressMap.get(ADDRESS);
        }

        String bodyHtml = null;
        if (messageBody.get(BODY) instanceof Map<?, ?> map) {
            bodyHtml = (String) map.get(CONTENT);
        }

        return new SimpleMessage(
            (String) messageBody.get(ID),
            (String) messageBody.get(SUBJECT),
            from,
            getRecipients(messageBody, TO_RECIPIENTS),
            getRecipients(messageBody, CC_RECIPIENTS),
            getRecipients(messageBody, BCC_RECIPIENTS),
            (String) messageBody.get("bodyPreview"),
            bodyHtml,
            getFileEntries(context, messageBody, id));
    }

    public static List<Map<?, ?>> getItemsFromNextPage(String link, Context context) {
        List<Map<?, ?>> otherItems = new ArrayList<>();

        while (link != null && !link.isEmpty()) {
            String finalLink = link;

            Map<String, Object> body = context.http(http -> http.get(finalLink))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (body.get(VALUE) instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> map) {
                        otherItems.add(map);
                    }
                }
            }

            link = (String) body.get(ODATA_NEXT_LINK);
        }

        return otherItems;
    }

    public static String getMailboxTimeZone(Context context) {
        Map<String, String> body = context.http(http -> http.get("/me/mailboxSettings/timeZone"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return body.get(VALUE);
    }

    private static List<FileEntry> getFileEntries(Context context, Map<?, ?> messageBody, String id) {
        List<FileEntry> fileEntries = new ArrayList<>();

        if ((boolean) messageBody.get("hasAttachments")) {
            Map<String, Object> attachmentsBody = context
                .http(http -> http.get("/me/messages/%s/attachments".formatted(id)))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            if (attachmentsBody.get(VALUE) instanceof List<?> attachments) {
                for (Object attachment : attachments) {
                    if (attachment instanceof Map<?, ?> map) {
                        String contentBytes = (String) map.get(CONTENT_BYTES);
                        byte[] decodedBytes = context.encoder(encoder -> encoder.base64Decode(contentBytes));

                        FileEntry fileEntry = context.file(
                            file -> file.storeContent((String) map.get(NAME),
                                new ByteArrayInputStream(decodedBytes)));

                        fileEntries.add(fileEntry);
                    }
                }
            }
        }

        return fileEntries;
    }

    private static List<String> getRecipients(Map<?, ?> body, String recipientType) {
        List<String> recipients = new ArrayList<>();

        if (body.get(recipientType) instanceof List<?> list) {
            for (Object recipient : list) {
                if (recipient instanceof Map<?, ?> recipientMap &&
                    recipientMap.get(EMAIL_ADDRESS) instanceof Map<?, ?> emailAddressMap) {

                    recipients.add((String) emailAddressMap.get(ADDRESS));
                }
            }
        }

        return recipients;
    }

    @SuppressFBWarnings("EI")
    public record SimpleMessage(
        String id, String subject, String from, List<String> to, List<String> cc, List<String> bcc, String bodyPlain,
        String bodyHtml, List<FileEntry> attachments) {
    }
}

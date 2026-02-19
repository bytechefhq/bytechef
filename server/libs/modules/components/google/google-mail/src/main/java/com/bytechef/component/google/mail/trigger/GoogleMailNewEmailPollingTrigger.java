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

import static com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.FORMAT_PROPERTY;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static com.bytechef.component.google.mail.definition.Format.FULL;
import static com.bytechef.component.google.mail.definition.Format.SIMPLE;
import static com.bytechef.component.google.mail.util.GoogleMailUtils.getSimpleMessage;
import static com.bytechef.google.commons.GoogleUtils.getCalendarTimezone;
import static com.bytechef.google.commons.GoogleUtils.translateGoogleIOException;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.google.mail.definition.Format;
import com.bytechef.component.google.mail.util.GoogleMailUtils;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class GoogleMailNewEmailPollingTrigger {

    protected static final String LAST_TIME_CHECKED = "lastTimeChecked";

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newEmailPolling")
        .title("New Email Polling")
        .description("Periodically checks your Gmail inbox for any new incoming emails.")
        .type(TriggerType.POLLING)
        .help("", "https://docs.bytechef.io/reference/components/google-mail_v1#new-email-polling")
        .properties(FORMAT_PROPERTY)
        .output(GoogleMailUtils::getMessageOutput)
        .poll(GoogleMailNewEmailPollingTrigger::poll);

    private GoogleMailNewEmailPollingTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext context) {

        try {
            Calendar calendar = GoogleServices.getCalendar(connectionParameters);

            String timezone = getCalendarTimezone(calendar);

            ZoneId zoneId = ZoneId.of(timezone);

            LocalDateTime now = LocalDateTime.now(zoneId);

            boolean editorEnvironment = context.isEditorEnvironment();

            LocalDateTime startDate = closureParameters.getLocalDateTime(
                LAST_TIME_CHECKED, editorEnvironment ? now.minusHours(3) : now);

            Gmail gmail = GoogleServices.getMail(connectionParameters);

            ZonedDateTime zonedDateTime = startDate.atZone(ZoneId.of(timezone));

            String query = "is:unread after:" + zonedDateTime.toEpochSecond();

            List<Object> messages = fetchUnreadMessages(
                gmail, query, inputParameters.get(FORMAT, Format.class, SIMPLE), context, editorEnvironment);

            return new PollOutput(messages, Map.of(LAST_TIME_CHECKED, now), false);
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }
    }

    private static List<Object> fetchUnreadMessages(
        Gmail gmail, String query, Format format, TriggerContext context, boolean editorEnvironment)
        throws IOException {

        List<Object> messages = new ArrayList<>();

        String nextPageToken = null;
        long pageSize = editorEnvironment ? 1L : 500L;

        do {
            ListMessagesResponse listMessagesResponse = gmail.users()
                .messages()
                .list(ME)
                .setQ(query)
                .setMaxResults(pageSize)
                .setPageToken(nextPageToken)
                .execute();

            if (listMessagesResponse.getMessages() != null) {
                for (Message message : listMessagesResponse.getMessages()) {
                    Message fetchedMessage = gmail.users()
                        .messages()
                        .get(ME, message.getId())
                        .setFormat(format == SIMPLE ? FULL.getMapping() : format.getMapping())
                        .execute();

                    if (format.equals(SIMPLE)) {
                        messages.add(getSimpleMessage(fetchedMessage, context, gmail));
                    } else {
                        messages.add(fetchedMessage);
                    }
                }
            }

            nextPageToken = listMessagesResponse.getNextPageToken();

            if (editorEnvironment) {
                break;
            }
        } while (nextPageToken != null);

        return messages;
    }
}

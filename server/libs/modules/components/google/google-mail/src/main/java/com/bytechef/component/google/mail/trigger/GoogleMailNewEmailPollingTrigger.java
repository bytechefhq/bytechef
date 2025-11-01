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
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ID;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.ME;
import static com.bytechef.component.google.mail.constant.GoogleMailConstants.THREAD_ID;
import static com.bytechef.google.commons.GoogleUtils.getCalendarTimezone;
import static com.bytechef.google.commons.GoogleUtils.translateGoogleIOException;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
        .output(
            outputSchema(
                array()
                    .items(
                        object()
                            .properties(
                                string(ID)
                                    .description("ID of the message."),
                                string(THREAD_ID)
                                    .description("The ID of the thread the message belongs to.")))))
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

            LocalDateTime startDate = closureParameters.getLocalDateTime(
                LAST_TIME_CHECKED, context.isEditorEnvironment() ? now.minusHours(3) : now);

            Gmail gmail = GoogleServices.getMail(connectionParameters);

            ZonedDateTime zonedDateTime = startDate.atZone(ZoneId.of(timezone));

            ListMessagesResponse listMessagesResponse = gmail.users()
                .messages()
                .list(ME)
                .setQ("is:unread after:" + zonedDateTime.toEpochSecond())
                .execute();

            return new PollOutput(listMessagesResponse.getMessages(), Map.of(LAST_TIME_CHECKED, now), false);
        } catch (IOException e) {
            throw translateGoogleIOException(e);
        }
    }
}

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

package com.bytechef.component.microsoft.outlook.trigger;

import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FORMAT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FORMAT_PROPERTY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ODATA_NEXT_LINK;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.VALUE;
import static com.bytechef.component.microsoft.outlook.definition.Format.SIMPLE;
import static com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils.createSimpleMessage;
import static com.bytechef.microsoft.commons.MicrosoftUtils.getItemsFromNextPage;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.microsoft.outlook.definition.Format;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils;
import com.bytechef.microsoft.commons.MicrosoftUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftOutlook365NewEmailTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newEmail")
        .title("New Email")
        .description("Triggers when new mail is received.")
        .type(TriggerType.POLLING)
        .properties(FORMAT_PROPERTY)
        .output(MicrosoftOutlook365Utils::getMessageOutput)
        .poll(MicrosoftOutlook365NewEmailTrigger::poll)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    protected static final String LAST_TIME_CHECKED = "lastTimeChecked";

    private MicrosoftOutlook365NewEmailTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext context) {

        ZoneId zoneId = ZoneId.systemDefault();

        LocalDateTime now = LocalDateTime.now(zoneId);

        LocalDateTime startDate = closureParameters.getLocalDateTime(
            LAST_TIME_CHECKED, context.isEditorEnvironment() ? now.minusHours(3) : now);

        List<Map<?, ?>> emails = new ArrayList<>();

        String formattedStartDate = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
            .withZone(zoneId));

        Map<String, Object> body = context
            .http(http -> http.get("/me/mailFolders/Inbox/messages"))
            .queryParameters(
                "$filter", "isRead eq false and receivedDateTime ge " + formattedStartDate,
                "$orderby", "receivedDateTime desc")
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get(VALUE) instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    emails.add(map);
                }
            }
        }

        emails.addAll(getItemsFromNextPage((String) body.get(ODATA_NEXT_LINK), context));

        Format format = inputParameters.getRequired(FORMAT, Format.class);

        if (format.equals(SIMPLE)) {
            List<MicrosoftOutlook365Utils.SimpleMessage> simpleMessages = new ArrayList<>();

            for (Map<?, ?> email : emails) {
                simpleMessages.add(createSimpleMessage(context, email));
            }

            return new PollOutput(simpleMessages, Map.of(LAST_TIME_CHECKED, now), false);
        } else {
            return new PollOutput(emails, Map.of(LAST_TIME_CHECKED, now), false);
        }
    }
}

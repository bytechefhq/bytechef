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
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ID;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ODATA_NEXT_LINK;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.VALUE;
import static com.bytechef.component.microsoft.outlook.definition.Format.SIMPLE;
import static com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils.createSimpleMessage;
import static com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils.getItemsFromNextPage;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.microsoft.outlook.definition.Format;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
        .output()
        .poll(MicrosoftOutlook365NewEmailTrigger::poll);

    protected static final String LAST_TIME_CHECKED = "lastTimeChecked";

    private MicrosoftOutlook365NewEmailTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext context) {

        ZoneId zoneId = ZoneId.systemDefault();

        LocalDateTime now = LocalDateTime.now(zoneId);

        LocalDateTime startDate = closureParameters.getLocalDateTime(LAST_TIME_CHECKED, now.minusHours(3));

        List<Map<?, ?>> maps = new ArrayList<>();

        Map<String, Object> body = context
            .http(http -> http.get("/me/mailFolders/Inbox/messages"))
            .queryParameters("$filter", "isRead eq false", "$orderby", "receivedDateTime desc")
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get(VALUE) instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    addValidEmail(map, startDate, now, maps, zoneId);
                }
            }
        }

        List<Map<?, ?>> otherItems = getItemsFromNextPage((String) body.get(ODATA_NEXT_LINK), context);

        for (Map<?, ?> map : otherItems) {
            addValidEmail(map, startDate, now, maps, zoneId);
        }

        Format format = inputParameters.getRequired(FORMAT, Format.class);

        if (format.equals(SIMPLE)) {
            List<MicrosoftOutlook365Utils.SimpleMessage> simpleMessages = new ArrayList<>();

            for (Map<?, ?> map : maps) {
                simpleMessages.add(createSimpleMessage(context, map, (String) map.get(ID)));
            }

            return new PollOutput(simpleMessages, Map.of(LAST_TIME_CHECKED, now), false);
        } else {
            return new PollOutput(maps, Map.of(LAST_TIME_CHECKED, now), false);
        }
    }

    private static void addValidEmail(
        Map<?, ?> map, LocalDateTime startDate, LocalDateTime endDate, List<Map<?, ?>> maps, ZoneId zoneId) {

        ZonedDateTime receivedDateTime = ZonedDateTime.parse((String) map.get("receivedDateTime"));

        LocalDateTime createdDateTime = LocalDateTime.ofInstant(receivedDateTime.toInstant(), zoneId);

        if (createdDateTime.isAfter(startDate) && createdDateTime.isBefore(endDate)) {
            maps.add(map);
        }
    }
}

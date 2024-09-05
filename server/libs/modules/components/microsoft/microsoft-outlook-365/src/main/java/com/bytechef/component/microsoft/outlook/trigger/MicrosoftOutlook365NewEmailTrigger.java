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

package com.bytechef.component.microsoft.outlook.trigger;

import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.outputSchema;
import static com.bytechef.component.definition.ComponentDSL.trigger;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ODATA_NEXT_LINK;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.VALUE;
import static com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils.getItemsFromNextPage;

import com.bytechef.component.definition.ComponentDSL.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
        .output(outputSchema(array().items(MESSAGE_OUTPUT_PROPERTY)))
        .poll(MicrosoftOutlook365NewEmailTrigger::poll);

    protected static final String LAST_TIME_CHECKED = "lastTimeChecked";

    private MicrosoftOutlook365NewEmailTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext context) {

        LocalDateTime startDate = closureParameters.getLocalDateTime(LAST_TIME_CHECKED, LocalDateTime.now());
        LocalDateTime endDate = LocalDateTime.now();

        List<Map<?, ?>> maps = new ArrayList<>();

        Map<String, Object> body = context
            .http(http -> http.get("/mailFolders/Inbox/messages"))
            .queryParameters("$filter", "isRead eq false", "$orderby", "receivedDateTime desc")
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get(VALUE) instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    addValidEmail(map, startDate, endDate, maps);
                }
            }
        }

        List<Map<?, ?>> otherItems = getItemsFromNextPage(context, (String) body.get(ODATA_NEXT_LINK));

        for (Map<?, ?> map : otherItems) {
            addValidEmail(map, startDate, endDate, maps);
        }

        return new PollOutput(maps, Map.of(LAST_TIME_CHECKED, endDate), false);
    }

    private static void addValidEmail(
        Map<?, ?> map, LocalDateTime startDate, LocalDateTime endDate, List<Map<?, ?>> maps) {

        LocalDateTime createdDateTime =
            LocalDateTime.ofInstant(ZonedDateTime.parse((String) map.get("receivedDateTime"))
                .toInstant(), ZoneOffset.systemDefault());

        if (createdDateTime.isAfter(startDate) && createdDateTime.isBefore(endDate)) {
            maps.add(map);
        }
    }
}

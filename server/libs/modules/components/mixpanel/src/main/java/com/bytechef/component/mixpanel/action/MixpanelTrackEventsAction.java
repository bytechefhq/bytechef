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

package com.bytechef.component.mixpanel.action;

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.mixpanel.constant.MixpanelConstants.DISTINCT_ID;
import static com.bytechef.component.mixpanel.constant.MixpanelConstants.EVENT;
import static com.bytechef.component.mixpanel.constant.MixpanelConstants.EVENTS;
import static com.bytechef.component.mixpanel.constant.MixpanelConstants.INSERT_ID;
import static com.bytechef.component.mixpanel.constant.MixpanelConstants.TIME;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Marija Horvat
 */
public class MixpanelTrackEventsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("trackEvents")
        .title("Track Events")
        .description("Send batches of events from your servers to Mixpanel.")
        .properties(
            array(EVENTS)
                .label("Events")
                .description("A list of events to be sent to Mixpanel.")
                .items(
                    object()
                        .properties(
                            string(EVENT)
                                .label("Event")
                                .description("The name of the event.")
                                .required(true),
                            dateTime(TIME)
                                .label("Time")
                                .description("The time at which the event occurred.")
                                .required(true),
                            string(DISTINCT_ID)
                                .label("Distinct ID")
                                .description("The unique identifier of the user who performed the event.")
                                .required(false),
                            string(INSERT_ID)
                                .label("Insert ID")
                                .description(
                                    "A unique identifier for the event, used for deduplication. Events with " +
                                        "identical values for (event, time, distinct_id, $insert_id) are considered " +
                                        "duplicates; only the latest ingested one will be considered in queries.")
                                .required(true)))
                .minItems(1)
                .required(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        integer("code"),
                        integer("num_records_imported"),
                        string("status"))))
        .perform(MixpanelTrackEventsAction::perform);

    private MixpanelTrackEventsAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        List<Map<String, Object>> events = inputParameters.getRequiredList(EVENTS, new TypeReference<>() {});

        List<Map<String, Object>> body = events.stream()
            .map(MixpanelTrackEventsAction::transformEvent)
            .collect(Collectors.toList());

        return context.http(http -> http.post("/import"))
            .queryParameter("strict", "1")
            .body(Http.Body.of(body))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }

    private static Map<String, Object> transformEvent(Map<String, Object> map) {
        Map<String, Object> propertiesMap = new HashMap<>();

        LocalDateTime date = LocalDateTime.parse((String) map.get(TIME), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        long time = date.toInstant(ZoneOffset.UTC)
            .getEpochSecond();

        propertiesMap.put(TIME, time);

        if (map.get(DISTINCT_ID) != null) {
            propertiesMap.put(DISTINCT_ID, map.get(DISTINCT_ID));
        }

        propertiesMap.put(INSERT_ID, map.get(INSERT_ID));

        return Map.of(EVENT, map.get(EVENT), "properties", propertiesMap);
    }
}

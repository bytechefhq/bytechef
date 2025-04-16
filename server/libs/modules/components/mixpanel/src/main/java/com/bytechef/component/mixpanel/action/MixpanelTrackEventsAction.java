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
import static com.bytechef.component.mixpanel.constant.MixpanelConstants.INSERT_ID;
import static com.bytechef.component.mixpanel.constant.MixpanelConstants.TIME;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class MixpanelTrackEventsAction {
    public static final ModifiableActionDefinition ACTION_DEFINITION = action("trackEvents")
        .title("Track Events")
        .description("Send batches of events from your servers to Mixpanel.")
        .properties(
            array("Events")
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
                                .label("Distinct id")
                                .description("The unique identifier of the user who performed the event.")
                                .required(true),
                            string(INSERT_ID)
                                .label("Insert id")
                                .description(
                                    "A unique identifier for the event, used for deduplication. Events with " +
                                        "identical values for (event, time, distinct_id, $insert_id) are considered " +
                                        "duplicates; only the latest ingested one will be considered in queries.")
                                .required(true))))
        .output(
            outputSchema(
                object()
                    .properties(
                        object("response")
                            .properties(
                                integer("code"),
                                integer("num_records_imported"),
                                string("status")))))
        .perform(MixpanelTrackEventsAction::perform);

    private MixpanelTrackEventsAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        List<Map<?, ?>> body = new ArrayList<>();

        for (int i = 0; i < inputParameters.getList("Events")
            .size(); i++) {
            Map<String, Object> map = (Map<String, Object>) inputParameters.getList("Events")
                .get(i);

            LocalDateTime date = LocalDateTime.parse((String) map.get(TIME), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            long time = date.toInstant(ZoneOffset.UTC)
                .getEpochSecond();

            Map<String, Object> properties = Map.of(
                TIME, time, DISTINCT_ID, map.get(DISTINCT_ID), INSERT_ID, map.get(INSERT_ID));

            body.add(Map.of(EVENT, map.get(EVENT), "properties", properties));
        }

        return context.http(http -> http.post("/import"))
            .queryParameter("strict", "1")
            .body(Context.Http.Body.of(body))
            .configuration(Context.Http.responseType(Context.Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}

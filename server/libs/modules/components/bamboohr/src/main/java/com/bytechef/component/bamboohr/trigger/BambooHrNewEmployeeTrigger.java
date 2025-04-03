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

package com.bytechef.component.bamboohr.trigger;

import static com.bytechef.component.bamboohr.constant.BambooHrConstants.FIELDS;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.ID;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.LAST_TIME_CHECKED;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.bamboohr.util.BambooHrUtils;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OptionsDataSource;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition;
import com.bytechef.component.definition.TypeReference;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 */
public class BambooHrNewEmployeeTrigger {

    static final List<String> EMPLOYEE_LIST = new ArrayList<>();

    public static final ComponentDsl.ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newEmployee")
        .title("New Employee")
        .description("Triggers when a new employee is created.")
        .type(TriggerDefinition.TriggerType.POLLING)
        .properties(
            array(FIELDS)
                .description("Fields you want to get from employee. See documentation for available fields.")
                .items(string())
                .options((OptionsDataSource.TriggerOptionsFunction<String>) BambooHrUtils::getFieldOptions)
                .required(true))
        .output()
        .poll(BambooHrNewEmployeeTrigger::poll);

    private BambooHrNewEmployeeTrigger() {
    }

    protected static TriggerDefinition.PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext triggerContext) {

        List<String> fields = inputParameters.getRequiredList(FIELDS, String.class);
        String queryParameter = String.join(",", fields);

        ZoneId zoneId = ZoneId.of("GMT");

        LocalDateTime now = LocalDateTime.now(zoneId);

        Map<String, Object> body = triggerContext
            .http(http -> http.get("/employees/directory"))
            .header("accept", "application/json")
            .configuration(responseType(Context.Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Map<?, ?>> maps = new ArrayList<>();

        if (body.get("employees") instanceof List<?> list) {
            if (EMPLOYEE_LIST.isEmpty()) {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> map) {
                        String id = (String) map.get(ID);
                        EMPLOYEE_LIST.add(id);
                    }
                }
            } else {
                for (Object o : list) {
                    if (o instanceof Map<?, ?> map) {
                        String id = (String) map.get(ID);

                        if (!EMPLOYEE_LIST.contains(id)) {
                            maps.add(
                                triggerContext
                                    .http(http -> http.get("/employees/" + id))
                                    .queryParameter(FIELDS, queryParameter)
                                    .header("accept", "application/json")
                                    .configuration(responseType(Http.ResponseType.JSON))
                                    .execute()
                                    .getBody(new TypeReference<>() {}));
                        }
                    }
                }
            }
        }
        return new TriggerDefinition.PollOutput(maps, Map.of(LAST_TIME_CHECKED, now), false);
    }
}

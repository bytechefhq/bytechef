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

package com.bytechef.component.bamboohr.trigger;

import static com.bytechef.component.bamboohr.constant.BambooHrConstants.ALL_EMPLOYEES;
import static com.bytechef.component.bamboohr.constant.BambooHrConstants.ID;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Marija Horvat
 * @author Monika Kušter
 */
public class BambooHrNewEmployeeTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newEmployee")
        .title("New Employee")
        .description("Triggers when a new employee is created.")
        .type(TriggerType.POLLING)
        .output(outputSchema(string()))
        .poll(BambooHrNewEmployeeTrigger::poll);

    private BambooHrNewEmployeeTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext triggerContext) {

        List<String> allEmployees = closureParameters.getList(ALL_EMPLOYEES, String.class, List.of());
        List<String> allEmployeesUpdate = new ArrayList<>();

        Map<String, Object> body = triggerContext
            .http(http -> http.get("/employees/directory"))
            .header("accept", "application/json")
            .configuration(responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<String> newEmployees = new ArrayList<>();

        if (body.get("employees") instanceof List<?> employees) {
            for (Object employee : employees) {
                if (employee instanceof Map<?, ?> map) {
                    String id = (String) map.get(ID);

                    allEmployeesUpdate.add(id);

                    if (!allEmployees.contains(id)) {
                        newEmployees.add(id);
                    }
                }
            }
        }

        return new PollOutput(newEmployees, Map.of(ALL_EMPLOYEES, allEmployeesUpdate), false);
    }
}

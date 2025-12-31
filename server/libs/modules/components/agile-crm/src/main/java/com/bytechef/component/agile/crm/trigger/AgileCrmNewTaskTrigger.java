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

package com.bytechef.component.agile.crm.trigger;

import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.CREATED_TIME;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.LAST_TIME_CHECKED;
import static com.bytechef.component.agile.crm.constant.AgileCrmConstants.TASK_OUTPUT_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TypeReference;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class AgileCrmNewTaskTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newTask")
        .title("New Task")
        .description("Triggers when a new task is added.")
        .type(TriggerType.POLLING)
        .output(outputSchema(TASK_OUTPUT_PROPERTY))
        .poll(AgileCrmNewTaskTrigger::poll);

    private AgileCrmNewTaskTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext triggerContext) {

        Instant now = Instant.now();

        Instant lastTimeCheckedEpoch = closureParameters.get(
            LAST_TIME_CHECKED,
            Instant.class,
            triggerContext.isEditorEnvironment() ? now.minus(3, ChronoUnit.HOURS) : now);

        List<Map<String, Object>> tasks = triggerContext.http(http -> http.get("/tasks/based"))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Map<String, Object>> newTasks = new ArrayList<>();

        for (Map<String, Object> task : tasks) {
            if ((Integer) task.get(CREATED_TIME) >= lastTimeCheckedEpoch.getEpochSecond()) {
                newTasks.add(task);
            }
        }

        newTasks.sort(Comparator.comparing((Map<String, Object> t) -> (Integer) t.get(CREATED_TIME))
            .reversed());

        return new PollOutput(newTasks, Map.of(LAST_TIME_CHECKED, now), false);
    }
}

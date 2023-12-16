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

package com.bytechef.task.dispatcher.each;

import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.array;
import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.bool;
import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.date;
import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.dateTime;
import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.integer;
import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.number;
import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.object;
import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.string;
import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.task;
import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.taskDispatcher;
import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.time;
import static com.bytechef.task.dispatcher.each.constant.EachTaskDispatcherConstants.EACH;
import static com.bytechef.task.dispatcher.each.constant.EachTaskDispatcherConstants.INDEX;
import static com.bytechef.task.dispatcher.each.constant.EachTaskDispatcherConstants.ITEM;
import static com.bytechef.task.dispatcher.each.constant.EachTaskDispatcherConstants.ITERATEE;
import static com.bytechef.task.dispatcher.each.constant.EachTaskDispatcherConstants.LIST;

import com.bytechef.hermes.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class EachTaskDispatcherDefinitionFactory implements TaskDispatcherDefinitionFactory {

    private static final TaskDispatcherDefinition TASK_DISPATCHER_DEFINITION = taskDispatcher(EACH)
        .title("Each")
        .description(
            "Iterates over each item in list, in parallel. Note, that since it iterates over each item in parallel, there is no guarantee of completion order.")
        .icon("path:assets/each.svg")
        .properties(
            array(LIST)
                .label("List of items")
                .description("List of items to iterate over.")
                .items(array(), bool(), date(), dateTime(), integer(), number(), object(), string(), time()))
        .taskProperties(task(ITERATEE))
        .variableProperties(string(ITEM), string(INDEX));

    @Override
    public TaskDispatcherDefinition getDefinition() {
        return TASK_DISPATCHER_DEFINITION;
    }
}


/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.task.dispatcher.map;

import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.array;
import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.string;
import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.task;
import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.taskDispatcher;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.ITEM;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.ITEM_INDEX;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.ITEM_VAR;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.ITERATEE;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.LIST;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.MAP;

import com.bytechef.hermes.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class MapTaskDispatcherDefinitionFactory implements TaskDispatcherDefinitionFactory {

    private static final TaskDispatcherDefinition TASK_DISPATCHER_DEFINITION = taskDispatcher(MAP)
        .title("Map")
        .description(
            "Produces a new collection of values by mapping each value in `list` through defined task, in parallel. When execution is finished on all items, the `map` task will return a list of execution results in an order which corresponds to the order of the source `list`.")
        .properties(
            array(LIST).label("List of items")
                .description("List of items to iterate over."),
            string(ITEM_VAR)
                .label("Item Var")
                .description("The name of the item variable.")
                .defaultValue(ITEM),
            string(ITEM_INDEX)
                .label("Item Index")
                .description("The name of the index variable.")
                .defaultValue(ITEM_INDEX))
        .taskProperties(task(ITERATEE));

    @Override
    public TaskDispatcherDefinition getDefinition() {
        return TASK_DISPATCHER_DEFINITION;
    }
}

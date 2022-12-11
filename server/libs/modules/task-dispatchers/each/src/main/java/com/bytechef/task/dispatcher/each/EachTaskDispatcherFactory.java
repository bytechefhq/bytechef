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

package com.bytechef.task.dispatcher.each;

import static com.bytechef.hermes.definition.DefinitionDSL.oneOf;
import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.array;
import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.display;
import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.integer;
import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.string;
import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.task;
import static com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDSL.taskDispatcher;
import static com.bytechef.task.dispatcher.each.constants.EachTaskDispatcherConstants.EACH;
import static com.bytechef.task.dispatcher.each.constants.EachTaskDispatcherConstants.ITEM;
import static com.bytechef.task.dispatcher.each.constants.EachTaskDispatcherConstants.ITEM_INDEX;
import static com.bytechef.task.dispatcher.each.constants.EachTaskDispatcherConstants.ITEM_VAR;
import static com.bytechef.task.dispatcher.each.constants.EachTaskDispatcherConstants.ITERATEE;
import static com.bytechef.task.dispatcher.each.constants.EachTaskDispatcherConstants.LIST;

import com.bytechef.hermes.task.dispatcher.TaskDispatcherFactory;
import com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class EachTaskDispatcherFactory implements TaskDispatcherFactory {

    private static final TaskDispatcherDefinition TASK_DISPATCHER_DEFINITION = taskDispatcher(EACH)
            .display(
                    display("Each")
                            .description(
                                    "Iterates over each item in `list`, in parallel. Note, that since it iterates over each item in parallel, there is no guarantee of completion order."))
            .properties(
                    array(LIST)
                            .label("List of items")
                            .description("List of items to iterate over.")
                            .items(oneOf()),
                    string(ITEM_VAR)
                            .label("Item Var")
                            .description("The name of the item variable.")
                            .defaultValue(ITEM),
                    string(ITEM_INDEX)
                            .label("Item Index")
                            .description("The name of the index variable.")
                            .defaultValue(ITEM_INDEX))
            .output(oneOf("item"), integer("itemIndex"))
            .taskProperties(task(ITERATEE));

    @Override
    public TaskDispatcherDefinition getDefinition() {
        return TASK_DISPATCHER_DEFINITION;
    }
}

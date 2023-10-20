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

package com.bytechef.task.dispatcher.loop;

import static com.bytechef.hermes.task.dispatcher.TaskDispatcherDSL.array;
import static com.bytechef.hermes.task.dispatcher.TaskDispatcherDSL.bool;
import static com.bytechef.hermes.task.dispatcher.TaskDispatcherDSL.create;
import static com.bytechef.hermes.task.dispatcher.TaskDispatcherDSL.display;
import static com.bytechef.hermes.task.dispatcher.TaskDispatcherDSL.string;
import static com.bytechef.task.dispatcher.loop.constants.LoopTaskConstants.ITEM_INDEX;
import static com.bytechef.task.dispatcher.loop.constants.LoopTaskConstants.ITEM_VAR;
import static com.bytechef.task.dispatcher.loop.constants.LoopTaskConstants.LIST;
import static com.bytechef.task.dispatcher.loop.constants.LoopTaskConstants.LOOP;
import static com.bytechef.task.dispatcher.loop.constants.LoopTaskConstants.LOOP_FOREVER;

import com.bytechef.hermes.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.hermes.task.dispatcher.definition.TaskDispatcherDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class LoopTaskDispatcherDefinitionFactory implements TaskDispatcherDefinitionFactory {

    private static final TaskDispatcherDefinition TASK_DISPATCHER_DEFINITION = create(LOOP)
            .display(display("Loop").description("Loops sequentially over list of items."))
            .inputs(
                    array(LIST).label("List of items").description("List of items to iterate over."),
                    string(ITEM_VAR)
                            .label("Item Var")
                            .description("The name of the item variable.")
                            .defaultValue("item"),
                    string(ITEM_INDEX)
                            .label("Item Index")
                            .description("The name of the index variable.")
                            .defaultValue("itemIndex"),
                    bool(LOOP_FOREVER)
                            .label("Loop Forever")
                            .description("Should loop iterate until condition set by \'Loop Break\' statement is met.")
                            .defaultValue(false));

    @Override
    public TaskDispatcherDefinition getDefinition() {
        return TASK_DISPATCHER_DEFINITION;
    }
}

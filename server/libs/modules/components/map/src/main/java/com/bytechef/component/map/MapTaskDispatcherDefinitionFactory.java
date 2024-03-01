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

package com.bytechef.component.map;

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.array;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.integer;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.object;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.task;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.taskDispatcher;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.INDEX;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.ITEM;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.ITERATEE;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.LIST;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.MAP;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.registry.util.SchemaUtils;
import com.bytechef.platform.workflow.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.platform.workflow.task.dispatcher.definition.OutputFunction;
import com.bytechef.platform.workflow.task.dispatcher.definition.Property.ObjectProperty;
import com.bytechef.platform.workflow.task.dispatcher.definition.PropertyFactory;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.ModifiableValueProperty;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
//@Component
public class MapTaskDispatcherDefinitionFactory implements TaskDispatcherDefinitionFactory {

    private static final TaskDispatcherDefinition TASK_DISPATCHER_DEFINITION = taskDispatcher(MAP)
        .title("Map")
        .description(
            "Produces a new collection of values by mapping each value in list through defined task, in parallel. When execution is finished on all items, the `map` task will return a list of execution results in an order which corresponds to the order of the source list.")
        .icon("path:assets/map.svg")
        .properties(
            array(LIST)
                .label("List of items")
                .description("List of items to iterate over."))
        .output(getOutputFunction())
        .taskProperties(task(ITERATEE))
        .variableProperties(MapTaskDispatcherDefinitionFactory::getVariableProperties);

    @Override
    public TaskDispatcherDefinition getDefinition() {
        return TASK_DISPATCHER_DEFINITION;
    }

    protected static OutputFunction getOutputFunction() {
        // TODO
        return (inputParameters) -> null;
    }

    private static ObjectProperty getVariableProperties(Map<String, ?> inputParameters) {
        ObjectProperty variableProperties;

        List<?> list = MapUtils.getRequiredList(inputParameters, LIST);

        if (list.isEmpty()) {
            variableProperties = object();
        } else {
            variableProperties = object()
                .properties(
                    (ModifiableValueProperty<?, ?>) SchemaUtils.getOutputSchema(
                        list.getFirst(), ITEM, new PropertyFactory(list.getFirst())),
                    integer(INDEX));
        }

        return variableProperties;
    }
}

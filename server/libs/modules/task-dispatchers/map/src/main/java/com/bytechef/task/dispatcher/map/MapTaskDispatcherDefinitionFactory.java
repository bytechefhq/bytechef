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

package com.bytechef.task.dispatcher.map;

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.array;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.integer;
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
import com.bytechef.platform.workflow.task.dispatcher.definition.Property;
import com.bytechef.platform.workflow.task.dispatcher.definition.PropertyFactoryFunction;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
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
        .outputSchema(getOutputSchemaFunction())
        .taskProperties(task(ITERATEE))
        .variableProperties(MapTaskDispatcherDefinitionFactory::getVariableProperties);

    @Override
    public TaskDispatcherDefinition getDefinition() {
        return TASK_DISPATCHER_DEFINITION;
    }

    protected static OutputFunction getOutputSchemaFunction() {
        // TODO
        return (inputParameters) -> null;
    }

    private static List<Property> getVariableProperties(Map<String, ?> inputParameters) {
        List<Property> properties;

        List<?> list = MapUtils.getRequiredList(inputParameters, LIST);

        if (list.isEmpty()) {
            properties = List.of();
        } else {
            properties = List.of(
                (Property.ValueProperty<?>) SchemaUtils.getSchemaDefinition(
                    ITEM, new PropertyFactoryFunction(list.getFirst())),
                integer(INDEX));
        }

        return properties;
    }
}

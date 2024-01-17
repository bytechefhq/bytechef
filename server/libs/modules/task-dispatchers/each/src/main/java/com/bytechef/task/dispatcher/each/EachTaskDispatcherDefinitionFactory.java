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

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.array;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.integer;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.task;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDSL.taskDispatcher;
import static com.bytechef.task.dispatcher.each.constant.EachTaskDispatcherConstants.EACH;
import static com.bytechef.task.dispatcher.each.constant.EachTaskDispatcherConstants.INDEX;
import static com.bytechef.task.dispatcher.each.constant.EachTaskDispatcherConstants.ITEM;
import static com.bytechef.task.dispatcher.each.constant.EachTaskDispatcherConstants.ITERATEE;
import static com.bytechef.task.dispatcher.each.constant.EachTaskDispatcherConstants.LIST;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.registry.util.SchemaUtils;
import com.bytechef.platform.workflow.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.platform.workflow.task.dispatcher.definition.Property;
import com.bytechef.platform.workflow.task.dispatcher.definition.Property.ValueProperty;
import com.bytechef.platform.workflow.task.dispatcher.definition.PropertyFactoryFunction;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition;
import java.util.List;
import java.util.Map;
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
                .description("List of items to iterate over."))
        .taskProperties(task(ITERATEE))
        .variableProperties(EachTaskDispatcherDefinitionFactory::getVariableProperties);

    @Override
    public TaskDispatcherDefinition getDefinition() {
        return TASK_DISPATCHER_DEFINITION;
    }

    private static List<Property> getVariableProperties(Map<String, ?> inputParameters) {
        List<Property> properties;

        List<?> list = MapUtils.getRequiredList(inputParameters, LIST);

        if (list.isEmpty()) {
            properties = List.of();
        } else {
            properties = List.of(
                (ValueProperty<?>) SchemaUtils.getSchemaDefinition(
                    ITEM, new PropertyFactoryFunction(list.getFirst())),
                integer(INDEX));
        }

        return properties;
    }
}

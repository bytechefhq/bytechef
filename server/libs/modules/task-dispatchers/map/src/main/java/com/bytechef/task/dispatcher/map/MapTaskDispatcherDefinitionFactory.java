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

package com.bytechef.task.dispatcher.map;

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.array;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.integer;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.object;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.task;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.taskDispatcher;
import static com.bytechef.platform.workflow.task.dispatcher.map.MapDataSource.ENVIRONMENT_ID;
import static com.bytechef.platform.workflow.task.dispatcher.map.MapDataSource.WORKFLOW_ID;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.INDEX;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.ITEM;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.ITEMS;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.ITERATEE;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.MAP;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.util.SchemaUtils;
import com.bytechef.platform.workflow.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.platform.workflow.task.dispatcher.definition.Property.ObjectProperty;
import com.bytechef.platform.workflow.task.dispatcher.definition.PropertyFactory;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.ModifiableValueProperty;
import com.bytechef.platform.workflow.task.dispatcher.map.MapDataSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

/**
 * @author Ivica Cardic
 */
@Component
public class MapTaskDispatcherDefinitionFactory implements TaskDispatcherDefinitionFactory {

    private final TaskDispatcherDefinition taskDispatcherDefinition;

    public MapTaskDispatcherDefinitionFactory(Optional<MapDataSource> mapDataSource) {
        this.taskDispatcherDefinition = taskDispatcher(MAP)
            .title("Map")
            .description(
                "Produces a new collection of values by mapping each value in list through defined task, in parallel. When execution is finished on all items, the `map` task will return a list of execution results in an order which corresponds to the order of the source list.")
            .icon("path:assets/map.svg")
            .properties(
                array(ITEMS)
                    .label("List of items")
                    .description("List of items to iterate over."))
            .output(inputParameters -> mapDataSource
                .map(dataSource -> output(inputParameters, dataSource))
                .orElse(null))
            .taskProperties(task(ITERATEE))
            .variableProperties(MapTaskDispatcherDefinitionFactory::variableProperties);
    }

    @Override
    public TaskDispatcherDefinition getDefinition() {
        return taskDispatcherDefinition;
    }

    protected static OutputResponse output(Map<String, ?> inputParameters, MapDataSource mapDataSource) {
        String workflowId = MapUtils.getString(inputParameters, WORKFLOW_ID);
        long environmentId = MapUtils.getLong(inputParameters, ENVIRONMENT_ID, 0L);

        List<Map<String, ?>> iterateeTasks = MapUtils.getList(
            inputParameters, ITERATEE, new TypeReference<Map<String, ?>>() {}, List.of());

        if (iterateeTasks.isEmpty()) {
            return null;
        }

        Map<String, ?> lastTask = iterateeTasks.getLast();

        String lastTaskName = MapUtils.getString(lastTask, "name");
        String lastTaskType = MapUtils.getString(lastTask, "type");

        if (lastTaskType == null) {
            return null;
        }

        OutputResponse lastTaskOutput = mapDataSource.getLastIterateeTaskOutput(
            workflowId, lastTaskName, lastTaskType, environmentId);

        if (lastTaskOutput == null) {
            return null;
        }

        ModifiableValueProperty<?, ?> lastTaskSchema =
            (ModifiableValueProperty<?, ?>) lastTaskOutput.getOutputSchema();

        Object lastTaskSampleOutput = lastTaskOutput.getSampleOutput();

        if (lastTaskSampleOutput != null) {
            return OutputResponse.of(array().items(lastTaskSchema), List.of(lastTaskSampleOutput));
        }

        return OutputResponse.of(array().items(lastTaskSchema));
    }

    protected static OutputResponse variableProperties(Map<String, ?> inputParameters) {
        ObjectProperty variableProperties;

        List<?> list = MapUtils.getRequiredList(inputParameters, ITEMS);

        if (list.isEmpty()) {
            variableProperties = object();
        } else {
            ModifiableValueProperty<?, ?> outputSchema = (ModifiableValueProperty<?, ?>) SchemaUtils.getOutputSchema(
                ITEM, list.getFirst(), PropertyFactory.PROPERTY_FACTORY);

            variableProperties = object()
                .properties(outputSchema, integer(INDEX));
        }

        return OutputResponse.of(variableProperties);
    }
}

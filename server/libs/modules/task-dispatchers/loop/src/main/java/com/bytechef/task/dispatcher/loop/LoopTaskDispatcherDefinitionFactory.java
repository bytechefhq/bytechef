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

package com.bytechef.task.dispatcher.loop;

import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.array;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.bool;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.integer;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.object;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.task;
import static com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.taskDispatcher;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.INDEX;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.ITEM;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.ITEMS;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.ITERATEE;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.LOOP;
import static com.bytechef.task.dispatcher.loop.constant.LoopTaskDispatcherConstants.LOOP_FOREVER;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.util.SchemaUtils;
import com.bytechef.platform.workflow.task.dispatcher.TaskDispatcherDefinitionFactory;
import com.bytechef.platform.workflow.task.dispatcher.definition.PropertyFactory;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.definition.TaskDispatcherDsl.ModifiableValueProperty;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class LoopTaskDispatcherDefinitionFactory implements TaskDispatcherDefinitionFactory {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(LoopTaskDispatcherDefinitionFactory.class);

    private static final TaskDispatcherDefinition TASK_DISPATCHER_DEFINITION = taskDispatcher(LOOP)
        .title("Loop")
        .description("Loops sequentially over list of items.")
        .icon("path:assets/loop.svg")
        .properties(
            array(ITEMS)
                .label("List of items")
                .description("List of items to iterate over.")
                .displayCondition("%s == false".formatted(LOOP_FOREVER)),
            bool(LOOP_FOREVER)
                .label("Loop Forever")
                .description("Should loop iterate until condition set by 'Loop Break' statement is met.")
                .defaultValue(false))
        .taskProperties(
            array(ITERATEE)
                .items(task()))
        .variableProperties(LoopTaskDispatcherDefinitionFactory::variableProperties);

    @Override
    public TaskDispatcherDefinition getDefinition() {
        return TASK_DISPATCHER_DEFINITION;
    }

    protected static OutputResponse variableProperties(Map<String, ?> inputParameters) {
        OutputResponse outputResponse;
        List<?> list = List.of();

        if (MapUtils.containsKey(inputParameters, ITEMS)) {
            // TODO Remove once UI suppress executing outputs if previous nodes don't have defined output
            try {
                list = MapUtils.getList(inputParameters, ITEMS, List.of());
            } catch (Exception e) {
                if (logger.isDebugEnabled()) {
                    logger.debug(e.getMessage());
                }
            }
        }

        boolean allNull = list.isEmpty() || list.stream()
            .allMatch(Objects::isNull);

        if (allNull) {
            outputResponse = OutputResponse.of(
                object().properties(object(ITEM), integer(INDEX)), Map.of(ITEM, Map.of(), INDEX, 0));
        } else {
            ModifiableValueProperty<?, ?> itemProperty = (ModifiableValueProperty<?, ?>) SchemaUtils.getOutputSchema(
                ITEM, list.getFirst(), PropertyFactory.PROPERTY_FACTORY);

            outputResponse = OutputResponse.of(
                object().properties(itemProperty, integer(INDEX)), Map.of(ITEM, list.getFirst(), INDEX, 0));
        }

        return outputResponse;
    }
}

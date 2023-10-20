
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

package com.bytechef.component.map;

import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.ITEM_INDEX;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.ITEM_VAR;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.LIST;
import static com.bytechef.task.dispatcher.map.constant.MapTaskDispatcherConstants.MAP;

import com.bytechef.hermes.component.ComponentDefinitionFactory;
import com.bytechef.hermes.component.definition.ComponentDefinition;

public class MapComponentDefinitionFactory implements ComponentDefinitionFactory {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(MAP)
        .display(
            display("Map")
                .description(
                    "Produces a new collection of values by mapping each value in `list` through defined task, in parallel. When execution is finished on all items, the `map` task will return a list of execution results in an order which corresponds to the order of the source `list`."))
        .actions(action(MAP)
            .properties(
                array(LIST).label("List of items")
                    .description("List of items to iterate over."),
                string(ITEM_VAR)
                    .label("Item Var")
                    .description("The name of the item variable.")
                    .defaultValue("item"),
                string(ITEM_INDEX)
                    .label("Item Index")
                    .description("The name of the index variable.")
                    .defaultValue("itemIndex")));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

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

package com.bytechef.component.example;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.example.constant.ExampleConstants.EXAMPLE;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.example.action.ExampleDummyAction;
import com.bytechef.component.example.connection.ExampleConnection;
import com.google.auto.service.AutoService;

/**
 * @author Mario Cvjetojevic
 */
@AutoService(ComponentHandler.class)
public class ExampleComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(EXAMPLE)
        .title("Example")
        .description("Component description.")
        .icon("path:assets/example.svg")
        .categories(ComponentCategory.HELPERS)
        .connection(ExampleConnection.CONNECTION_DEFINITION)
        .actions(ExampleDummyAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

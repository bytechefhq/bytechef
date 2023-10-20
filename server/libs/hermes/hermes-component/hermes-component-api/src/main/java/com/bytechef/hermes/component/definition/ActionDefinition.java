
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

package com.bytechef.hermes.component.definition;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Property;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * @author Ivica Cardic
 */
@JsonDeserialize(as = ComponentDSL.ModifiableActionDefinition.class)
public sealed interface ActionDefinition permits ComponentDSL.ModifiableActionDefinition {

    Display getDisplay();

    Object getExampleOutput();

    Map<String, Object> getMetadata();

    String getName();

    List<Property<? extends Property<?>>> getOutput();

    List<Property<?>> getProperties();

    /**
     * The code that should be performed when an action is executed as a task when running inside the workflow engine.
     *
     * @return an optional perform function implementation
     */
    Optional<BiFunction<Context, ExecutionParameters, Object>> getPerformFunction();
}

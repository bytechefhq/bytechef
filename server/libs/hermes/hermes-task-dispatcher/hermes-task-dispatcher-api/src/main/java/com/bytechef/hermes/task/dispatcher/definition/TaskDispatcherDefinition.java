
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

package com.bytechef.hermes.task.dispatcher.definition;

import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.Resources;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Used for specifying a task dispatcher description.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
@Schema(
    name = "TaskDispatcherDefinition",
    description = "A task dispatcher defines a strategy for dispatching tasks to be executed.")
public sealed interface TaskDispatcherDefinition permits TaskDispatcherDSL.ModifiableTaskDispatcherDefinition {

    Display getDisplay();

    @Schema(name = "name", description = "The connection name.")
    String getName();

    @Schema(name = "output", description = "The output schema of a task dispatching result.")
    List<Property<? extends Property<?>>> getOutput();

    @Schema(name = "properties", description = "Properties of the connection.")
    List<Property<?>> getProperties();

    Resources getResources();

    int getVersion();

    @Schema(name = "taskProperties", description = "Properties used to define tasks to be dispatched.")
    List<Property<?>> getTaskProperties();
}

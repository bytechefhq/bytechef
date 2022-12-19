
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

import com.bytechef.hermes.component.Connection;
import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.Resources;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Used for specifying a connection.
 *
 * @author Ivica Cardic
 */
@Schema(name = "ConnectionDefinition", description = "A connection to an outside service.")
public sealed interface ConnectionDefinition permits ComponentDSL.ModifiableConnectionDefinition {

    List<? extends Authorization> getAuthorizations();

    Function<Connection, String> getBaseUriFunction();

    @Schema(name = "componentName", description = "The name of a component this connection can be used for.")
    String getComponentName();

    @Schema(name = "componentVersion", description = "The version of a component this connection can be used for.")
    int getComponentVersion();

    Display getDisplay();

    @Schema(name = "properties", description = "Properties of the connection.")
    List<? extends Property<?>> getProperties();

    Resources getResources();

    @Schema(name = "subtitle", description = "Additional explanation.")
    String getSubtitle();

    Optional<Consumer<Connection>> getTestConsumer();
}

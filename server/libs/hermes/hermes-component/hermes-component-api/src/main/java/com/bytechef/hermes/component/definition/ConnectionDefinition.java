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

import static com.bytechef.hermes.component.constants.ComponentConstants.BASE_URI;

import com.bytechef.hermes.component.Connection;
import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.Resources;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
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
public sealed class ConnectionDefinition permits ComponentDSL.ModifiableConnectionDefinition {

    protected String componentName;
    protected int componentVersion;
    protected List<? extends Authorization> authorizations = Collections.emptyList();

    @JsonIgnore
    protected Function<Connection, String> baseUriFunction = (connectionParameters) ->
            connectionParameters.containsKey(BASE_URI) ? connectionParameters.getParameter(BASE_URI) : null;

    protected Display display;
    protected List<? extends Property<?>> properties;
    protected Resources resources;
    protected String subtitle;

    @JsonIgnore
    protected Consumer<Connection> testConsumer;

    protected ConnectionDefinition() {}

    public List<? extends Authorization> getAuthorizations() {
        return authorizations;
    }

    public Function<Connection, String> getBaseUriFunction() {
        return baseUriFunction;
    }

    @Schema(name = "componentName", description = "The name of a component this connection can be used for.")
    public String getComponentName() {
        return componentName;
    }

    @Schema(name = "componentVersion", description = "The version of a component this connection can be used for.")
    public int getComponentVersion() {
        return componentVersion;
    }

    public Display getDisplay() {
        return display;
    }

    @Schema(name = "properties", description = "Properties of the connection.")
    public List<? extends Property<?>> getProperties() {
        return properties;
    }

    public Resources getResources() {
        return resources;
    }

    @Schema(name = "subtitle", description = "Additional explanation.")
    public String getSubtitle() {
        return subtitle;
    }

    public Optional<Consumer<Connection>> getTestConsumer() {
        return Optional.ofNullable(testConsumer);
    }
}

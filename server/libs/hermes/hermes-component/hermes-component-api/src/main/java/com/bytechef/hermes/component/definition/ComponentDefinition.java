
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

import com.bytechef.hermes.component.constants.Versions;
import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Resources;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Used for specifying a component.
 *
 * @author Ivica Cardic
 */
@Schema(
    name = "ComponentDefinition",
    description = "A component contains a set of reusable code(actions) that accomplish specific tasks, triggers(TODO) and connections if there is a need for a connection to an outside service.")
public sealed class ComponentDefinition permits ComponentDSL.ModifiableComponentDefinition {

    protected List<? extends ActionDefinition> actions;
    protected ConnectionDefinition connection;
    protected Display display;
    protected Map<String, Object> metadata;
    protected String name;
    protected Resources resources;
    protected int version = Versions.VERSION_1;

    protected ComponentDefinition(String name) {
        this.name = Objects.requireNonNull(name);
    }

    @SuppressWarnings("unchecked")
    @Schema(name = "actions", description = "The list of all available actions the component can perform.")
    public List<? extends ActionDefinition> getActions() {
        return actions;
    }

    @SuppressWarnings("unchecked")
    @Schema(name = "connection", description = "Definition of connection to an outside service.")
    public ConnectionDefinition getConnection() {
        return connection;
    }

    public Display getDisplay() {
        return display;
    }

    @Schema(name = "metadata", description = "Additional data that can be used during processing.")
    public Map<String, Object> getMetadata() {
        return metadata == null ? null : new HashMap<>(metadata);
    }

    @Schema(name = "name", description = "The connection name.")
    public String getName() {
        return name;
    }

    public Resources getResources() {
        return resources;
    }

    public int getVersion() {
        return version;
    }
}

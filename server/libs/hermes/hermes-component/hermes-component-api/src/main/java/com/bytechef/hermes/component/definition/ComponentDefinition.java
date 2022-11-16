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
import com.bytechef.hermes.definition.Definition;
import com.bytechef.hermes.definition.Resources;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Used for specifying a component.
 *
 * @author Ivica Cardic
 */
@Schema(
        name = "ComponentDefinition",
        description =
                "A component contains a set of reusable code(actions) that accomplish specific tasks, triggers(TODO) and connections if there is a need for a connection to an outside service.")
public final class ComponentDefinition implements Definition {

    private List<ConnectionDefinition> connections;
    private ComponentDisplay display;
    private String name;
    private List<Action> actions;
    private Resources resources;
    private int version = Versions.VERSION_1;

    private ComponentDefinition() {}

    public ComponentDefinition(String name) {
        this.name = name;
    }

    public ComponentDefinition actions(Action... actions) {
        this.actions = List.of(actions);

        return this;
    }

    public ComponentDefinition connections(ConnectionDefinition... connections) {
        this.connections = List.of(connections);

        return this;
    }

    public ComponentDefinition display(com.bytechef.hermes.component.definition.ComponentDisplay display) {
        this.display = display;

        return this;
    }

    public ComponentDefinition resources(Resources resources) {
        this.resources = resources;

        return this;
    }

    public ComponentDefinition version(int version) {
        this.version = version;

        return this;
    }

    @SuppressWarnings("unchecked")
    @Schema(name = "actions", description = "The list of all available actions the component can perform.")
    public List<Action> getActions() {
        return actions;
    }

    @SuppressWarnings("unchecked")
    @Schema(name = "connections", description = " The list of possible connections to an outside service.")
    public List<ConnectionDefinition> getConnections() {
        return connections;
    }

    @Override
    public ComponentDisplay getDisplay() {
        return display;
    }

    @Override
    @Schema(name = "name", description = "The connection name.")
    public String getName() {
        return name;
    }

    @Override
    public Resources getResources() {
        return resources;
    }

    @Override
    public int getVersion() {
        return version;
    }
}

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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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

    private List<ActionDefinition> actionDefinitions;
    private ConnectionDefinition connectionDefinition;
    private ComponentDisplay display;
    private Map<String, Object> metadata;
    private String name;
    private Resources resources;
    private int version = Versions.VERSION_1;

    private ComponentDefinition() {}

    public ComponentDefinition(String name) {
        this.name = name;
    }

    public ComponentDefinition actions(ActionDefinition... actionDefinitions) {
        this.actionDefinitions = List.of(actionDefinitions);

        return this;
    }

    public ComponentDefinition actions(List<ActionDefinition>... actionsList) {
        this.actionDefinitions =
                Stream.of(actionsList).flatMap(Collection::stream).toList();

        return this;
    }

    public ComponentDefinition connection(ConnectionDefinition connectionDefinition) {
        this.connectionDefinition = connectionDefinition;

        this.connectionDefinition.setComponentName(name);
        this.connectionDefinition.setDisplay(display);

        return this;
    }

    public ComponentDefinition display(ComponentDisplay display) {
        this.display = display;

        return this;
    }

    @SuppressWarnings("unchecked")
    public ComponentDefinition metadata(String key, String value) {
        if (metadata == null) {
            metadata = new HashMap<>();
        }

        this.metadata.put(key, value);

        return this;
    }

    @SuppressFBWarnings("EI2")
    public ComponentDefinition metadata(Map<String, Object> metadata) {
        this.metadata = metadata;

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
    public List<ActionDefinition> getActionDefinitions() {
        return actionDefinitions;
    }

    @SuppressWarnings("unchecked")
    @Schema(name = "connection", description = "Definition of connection to an outside service.")
    public ConnectionDefinition getConnectionDefinition() {
        return connectionDefinition;
    }

    @Override
    public ComponentDisplay getDisplay() {
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

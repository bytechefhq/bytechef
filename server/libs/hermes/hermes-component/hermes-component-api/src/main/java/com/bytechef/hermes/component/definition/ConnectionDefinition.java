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
import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.Resources;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Used for specifying an connection.
 *
 * @author Ivica Cardic
 */
@Schema(name = "ConnectionDefinition", description = "A connection to an outside service.")
public final class ConnectionDefinition implements Definition {

    private Display display;
    private String name;
    private List<Property> properties;
    private Resources resources;
    private String subtitle;
    private int version = Versions.VERSION_1;

    private ConnectionDefinition() {}

    public ConnectionDefinition(String name) {
        this.name = name;
    }

    public ConnectionDefinition display(Display display) {
        this.display = display;

        return this;
    }

    public ConnectionDefinition name(String name) {
        this.name = name;

        return this;
    }

    public ConnectionDefinition properties(Property... properties) {
        this.properties = List.of(properties);

        return this;
    }

    public ConnectionDefinition resources(Resources resources) {
        this.resources = resources;

        return this;
    }

    public ConnectionDefinition subtitle(String subtitle) {
        this.subtitle = subtitle;

        return this;
    }

    public ConnectionDefinition version(int version) {
        this.version = version;

        return this;
    }

    public Display getDisplay() {
        return display;
    }

    @Schema(name = "name", description = "The connection name.")
    public String getName() {
        return name;
    }

    @Override
    public Resources getResources() {
        return resources;
    }

    @Schema(name = "properties", description = "Properties of the connection.")
    public List<Property> getProperties() {
        return properties;
    }

    @Schema(name = "subtitle", description = "Additional explanation.")
    public String getSubtitle() {
        return subtitle;
    }

    @Override
    public int getVersion() {
        return version;
    }
}

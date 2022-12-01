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

import com.bytechef.hermes.component.ConnectionParameters;
import com.bytechef.hermes.definition.Definition;
import com.bytechef.hermes.definition.Display;
import com.bytechef.hermes.definition.Property;
import com.bytechef.hermes.definition.Resources;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Used for specifying an connection.
 *
 * @author Ivica Cardic
 */
@Schema(name = "ConnectionDefinition", description = "A connection to an outside service.")
public final class ConnectionDefinition implements Definition {

    private String componentName;
    private List<Authorization> authorizations = Collections.emptyList();
    private Function<ConnectionParameters, String> baseUriFunction =
            (connectionParameters) -> connectionParameters.getParameter(BASE_URI);
    private Display display;
    private List<Property<?>> properties;
    private Resources resources;
    private String subtitle;

    @JsonIgnore
    private Consumer<ConnectionParameters> testConsumer;

    public ConnectionDefinition() {}

    public ConnectionDefinition authorizations(Authorization... authorizations) {
        if (authorizations != null) {
            this.authorizations = List.of(authorizations);
        }

        return this;
    }

    public ConnectionDefinition baseUri(Function<ConnectionParameters, String> baseUriFunction) {
        this.baseUriFunction = baseUriFunction;

        return this;
    }

    public ConnectionDefinition properties(Property<?>... properties) {
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

    public ConnectionDefinition testConsumer(Consumer<ConnectionParameters> testConsumer) {
        this.testConsumer = testConsumer;

        return this;
    }

    public List<Authorization> getAuthorizations() {
        return authorizations;
    }

    public Function<ConnectionParameters, String> getBaseUriFunction() {
        return baseUriFunction;
    }

    @Schema(name = "componentName", description = "The name of a connection this connection can be used for.")
    public String getComponentName() {
        return componentName;
    }

    public Display getDisplay() {
        return display;
    }

    @Schema(name = "properties", description = "Properties of the connection.")
    public List<Property<?>> getProperties() {
        return properties;
    }

    public Resources getResources() {
        return resources;
    }

    @Schema(name = "subtitle", description = "Additional explanation.")
    public String getSubtitle() {
        return subtitle;
    }

    public Consumer<ConnectionParameters> getTestConsumer() {
        return testConsumer;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public void setDisplay(Display display) {
        this.display = new Display(display.getLabel());
    }
}

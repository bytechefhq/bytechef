/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.component.domain;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class ConnectionDefinition {

    private boolean authorizationRequired;
    private List<Authorization> authorizations;
    private String componentDescription;
    private String componentName;
    private String componentTitle;
    private List<? extends Property> properties;
    private int version;

    private ConnectionDefinition() {
    }

    public ConnectionDefinition(
        com.bytechef.component.definition.ConnectionDefinition connectionDefinition, String componentName,
        String componentTitle, String componentDescription) {

        this.authorizationRequired = OptionalUtils.orElse(connectionDefinition.getAuthorizationRequired(), true);
        this.authorizations = toAuthorizationDTOs(
            OptionalUtils.orElse(connectionDefinition.getAuthorizations(), Collections.emptyList()));
        this.componentDescription = componentDescription;
        this.componentName = componentName;
        this.componentTitle = componentTitle;
        this.properties = CollectionUtils.map(
            OptionalUtils.orElse(connectionDefinition.getProperties(), Collections.emptyList()),
            valueProperty -> (ValueProperty<?>) Property.toProperty(valueProperty));
        this.version = connectionDefinition.getVersion();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ConnectionDefinition that)) {
            return false;
        }

        return authorizationRequired == that.authorizationRequired && version == that.version &&
            Objects.equals(authorizations, that.authorizations) &&
            Objects.equals(componentDescription, that.componentDescription) &&
            Objects.equals(componentName, that.componentName) && Objects.equals(componentTitle, that.componentTitle) &&
            Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            authorizationRequired, authorizations, componentDescription, componentName, componentTitle, properties,
            version);
    }

    public List<Authorization> getAuthorizations() {
        return authorizations;
    }

    @Nullable
    public String getComponentDescription() {
        return componentDescription;
    }

    public String getComponentName() {
        return componentName;
    }

    public String getComponentTitle() {
        return componentTitle;
    }

    public List<? extends Property> getProperties() {
        return properties;
    }

    public int getVersion() {
        return version;
    }

    public boolean isAuthorizationRequired() {
        return authorizationRequired;
    }

    @Override
    public String toString() {
        return "ConnectionDefinition{" +
            "componentName='" + componentName + '\'' +
            ", version=" + version +
            ", componentTitle='" + componentTitle + '\'' +
            ", componentDescription='" + componentDescription + '\'' +
            ", authorizationRequired=" + authorizationRequired +
            ", properties=" + properties +
            ", authorizations=" + authorizations +
            '}';
    }

    private static List<Authorization> toAuthorizationDTOs(
        List<? extends com.bytechef.component.definition.Authorization> authorizations) {

        return authorizations.stream()
            .map(Authorization::new)
            .toList();
    }
}

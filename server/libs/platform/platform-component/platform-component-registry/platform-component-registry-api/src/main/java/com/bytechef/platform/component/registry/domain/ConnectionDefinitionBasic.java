/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.component.registry.domain;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.ConnectionDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class ConnectionDefinitionBasic {

    protected boolean authorizationRequired;
    protected String componentDescription;
    protected String componentName;
    protected String componentTitle;
    protected int version;

    protected ConnectionDefinitionBasic() {
    }

    public ConnectionDefinitionBasic(ConnectionDefinition connectionDefinition) {
        this.authorizationRequired = OptionalUtils.orElse(connectionDefinition.getAuthorizationRequired(), true);
        this.componentDescription =
            OptionalUtils.orElse(connectionDefinition.getComponentDescription(), null);
        this.componentName = connectionDefinition.getComponentName();
        this.componentTitle = OptionalUtils.orElse(
            connectionDefinition.getComponentTitle(), connectionDefinition.getComponentName());
        this.version = connectionDefinition.getVersion();
    }

    public boolean isAuthorizationRequired() {
        return authorizationRequired;
    }

    public Optional<String> getComponentDescription() {
        return Optional.ofNullable(componentDescription);
    }

    public String getComponentName() {
        return componentName;
    }

    public String getComponentTitle() {
        return componentTitle;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ConnectionDefinitionBasic that))
            return false;
        return authorizationRequired == that.authorizationRequired && version == that.version
            && Objects.equals(componentDescription, that.componentDescription)
            && Objects.equals(componentName, that.componentName) && Objects.equals(componentTitle, that.componentTitle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authorizationRequired, componentDescription, componentName, componentTitle, version);
    }

    @Override
    public String toString() {
        return "ConnectionDefinitionBasic{" +
            "authorizationRequired=" + authorizationRequired +
            ", componentDescription='" + componentDescription + '\'' +
            ", componentName='" + componentName + '\'' +
            ", componentTitle='" + componentTitle + '\'' +
            ", version=" + version +
            '}';
    }
}

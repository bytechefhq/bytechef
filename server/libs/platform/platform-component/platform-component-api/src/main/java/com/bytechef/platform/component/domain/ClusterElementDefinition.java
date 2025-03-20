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

package com.bytechef.platform.component.domain;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.platform.component.definition.PropertyFactory;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.util.SchemaUtils;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
public class ClusterElementDefinition {

    private String componentName;
    private int componentVersion;
    private String description;
    private ClusterElementType type;
    private String name;
    private boolean outputDefined;
    private boolean outputFunctionDefined;
    private OutputResponse outputResponse;
    private List<? extends Property> properties;
    private String title;

    private ClusterElementDefinition() {
    }

    public ClusterElementDefinition(
        com.bytechef.component.definition.ClusterElementDefinition<?> clusterElementDefinition,
        String componentName, int componentVersion) {

        this.componentName = componentName;
        this.componentVersion = componentVersion;
        this.description = OptionalUtils.orElse(clusterElementDefinition.getDescription(), null);
        this.type = clusterElementDefinition.getType();
        this.name = clusterElementDefinition.getName();
        this.outputDefined = OptionalUtils.mapOrElse(
            clusterElementDefinition.getOutputDefinition(), outputDefinition -> true, false);
        this.outputFunctionDefined = OptionalUtils.mapOrElse(
            clusterElementDefinition.getOutputDefinition(),
            outputDefinition -> OptionalUtils.mapOrElse(outputDefinition.getOutput(), output -> true, false), false);
        this.outputResponse = OptionalUtils.mapOrElse(
            clusterElementDefinition.getOutputDefinition(), ClusterElementDefinition::toOutputResponse, null);
        this.properties = CollectionUtils.map(
            OptionalUtils.orElse(clusterElementDefinition.getProperties(), List.of()), Property::toProperty);
        this.title = OptionalUtils.orElse(clusterElementDefinition.getTitle(), null);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ClusterElementDefinition that)) {
            return false;
        }

        return componentVersion == that.componentVersion && Objects.equals(description, that.description) &&
            Objects.equals(type, that.type) && Objects.equals(name, that.name) &&
            outputDefined == that.outputDefined && outputFunctionDefined == that.outputFunctionDefined &&
            Objects.equals(componentName, that.componentName) && Objects.equals(outputResponse, that.outputResponse) &&
            Objects.equals(properties, that.properties) && Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            componentName, componentVersion, description, type, name, outputDefined, outputFunctionDefined,
            outputResponse, properties, title);
    }

    public String getComponentName() {
        return componentName;
    }

    public int getComponentVersion() {
        return componentVersion;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public OutputResponse getOutputResponse() {
        return outputResponse;
    }

    public List<? extends Property> getProperties() {
        return Collections.unmodifiableList(properties);
    }

    public ClusterElementType getType() {
        return type;
    }

    public boolean isOutputDefined() {
        return outputDefined;
    }

    @Override
    public String toString() {
        return "ClusterElementDefinition{" +
            "name='" + name + '\'' +
            ", title='" + title + '\'' +
            ", description='" + description + '\'' +
            ", componentName='" + componentName + '\'' +
            ", componentVersion=" + componentVersion +
            ", type=" + type +
            ", properties=" + properties +
            ", outputDefined=" + outputDefined +
            ", outputFunctionDefined=" + outputFunctionDefined +
            ", outputResponse=" + outputResponse +
            '}';
    }

    private static OutputResponse toOutputResponse(
        com.bytechef.component.definition.OutputDefinition outputDefinition) {

        return outputDefinition.getOutputResponse()
            .map(
                outputResponse -> SchemaUtils.toOutput(
                    outputResponse, PropertyFactory.OUTPUT_FACTORY_FUNCTION, PropertyFactory.PROPERTY_FACTORY))
            .orElse(null);
    }
}

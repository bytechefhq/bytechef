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
import com.bytechef.commons.util.IconUtils;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.platform.component.definition.PropertyFactory;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.util.SchemaUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public final class ClusterElementDefinition {

    private String componentName;
    private int componentVersion;
    private String description;
    private Help help;
    private String icon;
    private String name;
    private boolean outputDefined;
    private boolean outputFunctionDefined;
    private OutputResponse outputResponse;
    private boolean outputSchemaDefined;
    private List<? extends Property> properties;
    private String title;
    private ClusterElementType type;

    private ClusterElementDefinition() {
    }

    public ClusterElementDefinition(
        com.bytechef.component.definition.ClusterElementDefinition<?> clusterElementDefinition, String componentName,
        Integer componentVersion, @Nullable String icon) {

        this.componentName = Objects.requireNonNull(componentName, "componentName is required");
        this.componentVersion = Objects.requireNonNull(componentVersion, "componentVersion is required");
        this.description = clusterElementDefinition.getDescription()
            .orElse(null);
        this.help = clusterElementDefinition.getHelp()
            .map(Help::new)
            .orElse(null);
        this.name = Objects.requireNonNull(clusterElementDefinition.getName(), "name is required");
        this.icon = IconUtils.readIcon(icon);
        this.outputDefined = clusterElementDefinition.getOutputDefinition()
            .isPresent();
        this.outputFunctionDefined = clusterElementDefinition.getOutputDefinition()
            .map(outputDefinition -> outputDefinition.getOutput()
                .isPresent())
            .orElse(false);
        this.outputResponse = clusterElementDefinition.getOutputDefinition()
            .map(ClusterElementDefinition::toOutputResponse)
            .orElse(null);
        this.outputSchemaDefined = outputResponse != null && outputResponse.outputSchema() != null;

        List<? extends com.bytechef.component.definition.Property> properties =
            clusterElementDefinition.getProperties();

        this.properties = CollectionUtils.map(properties, Property::toProperty);
        this.title = clusterElementDefinition.getTitle()
            .orElse(null);
        this.type = Objects.requireNonNull(clusterElementDefinition.getType(), "type is required");
    }

    private ClusterElementDefinition(
        ClusterElementDefinition clusterElementDefinition, List<? extends Property> prependedProperties) {

        this.componentName = clusterElementDefinition.componentName;
        this.componentVersion = clusterElementDefinition.componentVersion;
        this.description = clusterElementDefinition.description;
        this.help = clusterElementDefinition.help;
        this.icon = clusterElementDefinition.icon;
        this.name = clusterElementDefinition.name;
        this.outputDefined = clusterElementDefinition.outputDefined;
        this.outputFunctionDefined = clusterElementDefinition.outputFunctionDefined;
        this.outputResponse = clusterElementDefinition.outputResponse;
        this.outputSchemaDefined = clusterElementDefinition.outputSchemaDefined;

        List<Property> mergedProperties = new ArrayList<>(prependedProperties);

        mergedProperties.addAll(clusterElementDefinition.properties);

        this.properties = mergedProperties;
        this.title = clusterElementDefinition.title;
        this.type = clusterElementDefinition.type;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ClusterElementDefinition that)) {
            return false;
        }

        return Objects.equals(componentName, that.componentName) && componentVersion == that.componentVersion &&
            Objects.equals(description, that.description) && Objects.equals(help, that.help) &&
            Objects.equals(icon, that.icon) && Objects.equals(name, that.name) &&
            outputDefined == that.outputDefined && outputFunctionDefined == that.outputFunctionDefined &&
            Objects.equals(outputResponse, that.outputResponse) && outputSchemaDefined == that.outputSchemaDefined &&
            Objects.equals(properties, that.properties) && Objects.equals(title, that.title) &&
            Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            componentName, componentVersion, description, icon, name, outputDefined, outputFunctionDefined,
            outputResponse, outputSchemaDefined, properties, title, type);
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

    @Nullable
    public Help getHelp() {
        return help;
    }

    public String getIcon() {
        return icon;
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

    public ClusterElementDefinition withPrependedProperties(List<? extends Property> prependedProperties) {
        return new ClusterElementDefinition(this, prependedProperties);
    }

    public String getTitle() {
        return title;
    }

    public ClusterElementType getType() {
        return type;
    }

    public boolean isOutputDefined() {
        return outputDefined;
    }

    public boolean isOutputFunctionDefined() {
        return outputFunctionDefined;
    }

    public boolean isOutputSchemaDefined() {
        return outputSchemaDefined;
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
            ", outputSchemaDefined=" + outputSchemaDefined +
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

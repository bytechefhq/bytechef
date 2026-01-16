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
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import com.bytechef.platform.component.definition.PropertyFactory;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.util.SchemaUtils;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
public class ClusterElementDefinition {

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
        com.bytechef.component.definition.ClusterElementDefinition<?> clusterElementDefinition,
        String componentName, int componentVersion, String icon) {

        this.componentName = componentName;
        this.componentVersion = componentVersion;
        this.description = OptionalUtils.orElse(clusterElementDefinition.getDescription(), null);
        this.help = OptionalUtils.mapOrElse(clusterElementDefinition.getHelp(), Help::new, null);
        this.name = clusterElementDefinition.getName();
        this.icon = IconUtils.readIcon(icon);
        this.outputDefined = OptionalUtils.mapOrElse(
            clusterElementDefinition.getOutputDefinition(), outputDefinition -> true, false);
        this.outputFunctionDefined = OptionalUtils.mapOrElse(
            clusterElementDefinition.getOutputDefinition(),
            outputDefinition -> OptionalUtils.mapOrElse(outputDefinition.getOutput(), output -> true, false), false);
        this.outputResponse = OptionalUtils.mapOrElse(
            clusterElementDefinition.getOutputDefinition(), ClusterElementDefinition::toOutputResponse, null);
        this.outputSchemaDefined = outputResponse != null && outputResponse.outputSchema() != null;
        this.properties = CollectionUtils.map(
            OptionalUtils.orElse(clusterElementDefinition.getProperties(), List.of()), Property::toProperty);
        this.title = OptionalUtils.orElse(clusterElementDefinition.getTitle(), null);
        this.type = clusterElementDefinition.getType();
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

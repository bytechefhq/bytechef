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

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.registry.util.SchemaUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class ActionDefinition extends ActionDefinitionBasic {

    private boolean nodeDescriptionDefined;
    private Output output;
    private boolean outputDefined;
    private List<? extends Property> properties;

    private ActionDefinition() {
    }

    public ActionDefinition(com.bytechef.component.definition.ActionDefinition actionDefinition) {
        super(actionDefinition);

        this.nodeDescriptionDefined = OptionalUtils.mapOrElse(
            actionDefinition.getNodeDescriptionFunction(), editorDescriptionDataSource -> true, false);
        this.output = OptionalUtils.mapOrElse(
            actionDefinition.getOutput(),
            outputSchema -> SchemaUtils.toOutputSchema(
                outputSchema,
                (baseProperty, sampleOutput) -> new Output(
                    Property.toProperty((com.bytechef.component.definition.Property) baseProperty), sampleOutput)),
            null);
        this.outputDefined =
            OptionalUtils.mapOrElse(actionDefinition.getOutput(), outputSchema -> true, false) ||
                OptionalUtils.mapOrElse(
                    actionDefinition.getOutputFunction(), outputSchemaDataSource -> true,
                    actionDefinition.isDefaultOutputFunction());
        this.properties = CollectionUtils.map(
            OptionalUtils.orElse(actionDefinition.getProperties(), List.of()), Property::toProperty);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ActionDefinition that)) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        return nodeDescriptionDefined == that.nodeDescriptionDefined
            && outputDefined == that.outputDefined
            && Objects.equals(output, that.output)
            && Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nodeDescriptionDefined, outputDefined, output,
            properties);
    }

    public boolean isNodeDescriptionDefined() {
        return nodeDescriptionDefined;
    }

    public Output getOutput() {
        return output;
    }

    public boolean isOutputDefined() {
        return outputDefined;
    }

    public List<? extends Property> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "Definition{" +
            "nodeDescriptionDefined=" + nodeDescriptionDefined +
            ", outputDefined=" + outputDefined +
            ", outputSchema=" + output +
            ", properties=" + properties +
            ", batch=" + batch +
            ", description='" + description + '\'' +
            ", help=" + help +
            ", name='" + name + '\'' +
            ", title='" + title + '\'' +
            "} ";
    }

}

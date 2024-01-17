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
import com.bytechef.platform.registry.util.OutputSchemaUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class ActionDefinition extends ActionDefinitionBasic {

    private boolean editorDescriptionDataSource;
    private OutputSchema outputSchema;
    private boolean outputSchemaDataSource;
    private List<? extends Property> properties;

    private ActionDefinition() {
    }

    public ActionDefinition(com.bytechef.component.definition.ActionDefinition actionDefinition) {
        super(actionDefinition);

        this.editorDescriptionDataSource = OptionalUtils.mapOrElse(
            actionDefinition.getEditorDescriptionFunction(), editorDescriptionDataSource -> true, false);
        this.outputSchema = OptionalUtils.mapOrElse(
            actionDefinition.getOutputSchema(),
            outputSchema -> OutputSchemaUtils.toOutputSchema(
                outputSchema,
                (baseValueProperty, sampleOutput) -> new OutputSchema(
                    Property.toProperty((com.bytechef.component.definition.Property) baseValueProperty), sampleOutput)),
            null);
        this.outputSchemaDataSource = OptionalUtils.mapOrElse(
            actionDefinition.getOutputSchemaFunction(), outputSchemaDataSource -> true,
            actionDefinition.isOutputSchemaDefaultFunction());
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

        return editorDescriptionDataSource == that.editorDescriptionDataSource
            && outputSchemaDataSource == that.outputSchemaDataSource && Objects.equals(outputSchema, that.outputSchema)
            && Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), editorDescriptionDataSource, outputSchema, outputSchemaDataSource,
            properties);
    }

    public boolean isEditorDescriptionDataSource() {
        return editorDescriptionDataSource;
    }

    public OutputSchema getOutputSchema() {
        return outputSchema;
    }

    public boolean isOutputSchemaDataSource() {
        return outputSchemaDataSource;
    }

    public List<? extends Property> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "Definition{" +
            "editorDescriptionDataSource=" + editorDescriptionDataSource +
            ", outputSchema=" + outputSchema +
            ", outputSchemaDataSource=" + outputSchemaDataSource +
            ", properties=" + properties +
            ", batch=" + batch +
            ", description='" + description + '\'' +
            ", help=" + help +
            ", name='" + name + '\'' +
            ", title='" + title + '\'' +
            "} ";
    }

}

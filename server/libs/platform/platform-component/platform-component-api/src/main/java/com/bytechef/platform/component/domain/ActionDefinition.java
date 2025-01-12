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
import com.bytechef.platform.component.definition.PropertyFactory;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.util.SchemaUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.Nullable;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class ActionDefinition extends ActionDefinitionBasic {

    private boolean outputDefined;
    private boolean outputFunctionDefined;
    private OutputResponse outputResponse;
    private List<? extends Property> properties;
    private boolean workflowNodeDescriptionDefined;

    private ActionDefinition() {
    }

    public ActionDefinition(
        com.bytechef.component.definition.ActionDefinition actionDefinition, String componentName,
        int componentVersion) {

        super(actionDefinition, componentName, componentVersion);

        this.outputDefined = OptionalUtils.mapOrElse(
            actionDefinition.getOutputDefinition(), outputDefinition -> true, false);
        this.outputFunctionDefined = OptionalUtils.mapOrElse(
            actionDefinition.getOutputDefinition(),
            outputDefinition -> OptionalUtils.mapOrElse(outputDefinition.getOutput(), output -> true, false), false);
        this.outputResponse = OptionalUtils.mapOrElse(
            actionDefinition.getOutputDefinition(), ActionDefinition::toOutputResponse, null);
        this.properties = CollectionUtils.map(
            OptionalUtils.orElse(actionDefinition.getProperties(), List.of()), Property::toProperty);
        this.workflowNodeDescriptionDefined = OptionalUtils.mapOrElse(
            actionDefinition.getWorkflowNodeDescription(), workflowNodeDescriptionFunction -> true, false);
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

        return outputDefined == that.outputDefined && Objects.equals(outputResponse, that.outputResponse) &&
            Objects.equals(properties, that.properties) &&
            workflowNodeDescriptionDefined == that.workflowNodeDescriptionDefined;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            super.hashCode(), outputDefined, outputResponse, properties, workflowNodeDescriptionDefined);
    }

    public boolean isOutputDefined() {
        return outputDefined;
    }

    public boolean isOutputFunctionDefined() {
        return outputFunctionDefined;
    }

    public boolean isWorkflowNodeDescriptionDefined() {
        return workflowNodeDescriptionDefined;
    }

    @Nullable
    public OutputResponse getOutputResponse() {
        return outputResponse;
    }

    public List<? extends Property> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "ActionDefinition{" +
            "name='" + name + '\'' +
            ", componentName='" + componentName + '\'' +
            ", componentVersion=" + componentVersion +
            ", batch=" + batch +
            ", title='" + title + '\'' +
            ", description='" + description + '\'' +
            ", outputDefined=" + outputDefined +
            ", outputResponse=" + outputResponse +
            ", properties=" + properties +
            ", help=" + help +
            ", workflowNodeDescriptionDefined=" + workflowNodeDescriptionDefined +
            "} ";
    }

    private static OutputResponse toOutputResponse(
        com.bytechef.component.definition.OutputDefinition outputDefinition) {

        return outputDefinition.getOutputResponse()
            .map(
                outputResponse -> SchemaUtils.toOutput(
                    outputResponse,
                    (property, sampleOutput) -> new OutputResponse(
                        Property.toProperty((com.bytechef.component.definition.Property) property), sampleOutput),
                    PropertyFactory.PROPERTY_FACTORY))
            .orElse(null);
    }
}

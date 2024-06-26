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

    private boolean dynamicOutput;
    private Output output;
    private boolean outputDefined;
    private List<? extends Property> properties;
    private boolean workflowNodeDescriptionDefined;

    private ActionDefinition() {
    }

    public ActionDefinition(
        com.bytechef.component.definition.ActionDefinition actionDefinition, String componentName,
        int componentVersion) {

        super(actionDefinition, componentName, componentVersion);

        this.dynamicOutput = OptionalUtils.mapOrElse(
            actionDefinition.getOutput(), outputFunction -> true, actionDefinition.isDynamicOutput());
        this.output = OptionalUtils.mapOrElse(
            actionDefinition.getOutputResponse(),
            outputResponse -> SchemaUtils.toOutput(
                outputResponse,
                (property, sampleOutput) -> new Output(
                    Property.toProperty((com.bytechef.component.definition.Property) property), sampleOutput)),
            null);
        this.outputDefined = OptionalUtils.mapOrElse(actionDefinition.getOutputResponse(), output -> true, false);
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

        return dynamicOutput == that.dynamicOutput &&
            Objects.equals(output, that.output) && outputDefined == that.outputDefined &&
            Objects.equals(properties, that.properties) &&
            workflowNodeDescriptionDefined == that.workflowNodeDescriptionDefined;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            super.hashCode(), dynamicOutput, output, outputDefined, properties, workflowNodeDescriptionDefined);
    }

    public Output getOutput() {
        return output;
    }

    public List<? extends Property> getProperties() {
        return properties;
    }

    public boolean isDynamicOutput() {
        return dynamicOutput;
    }

    public boolean isOutputDefined() {
        return outputDefined;
    }

    public boolean isWorkflowNodeDescriptionDefined() {
        return workflowNodeDescriptionDefined;
    }

    @Override
    public String toString() {
        return "Definition{" +
            "name='" + name + '\'' +
            ", title='" + title + '\'' +
            ", description='" + description + '\'' +
            ", outputDefined=" + outputDefined +
            ", dynamicOutput=" + dynamicOutput +
            ", output=" + output +
            ", properties=" + properties +
            ", batch=" + batch +
            ", help=" + help +
            ", workflowNodeDescriptionDefined=" + workflowNodeDescriptionDefined +
            "} ";
    }

}

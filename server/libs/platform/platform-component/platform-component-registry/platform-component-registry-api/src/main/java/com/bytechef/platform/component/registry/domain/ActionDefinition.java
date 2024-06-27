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

    private boolean workflowNodeDescriptionDefined;
    private Output output;
    private boolean outputDefined;
    private boolean outputFunctionDefined;
    private List<? extends Property> properties;

    private ActionDefinition() {
    }

    public ActionDefinition(
        com.bytechef.component.definition.ActionDefinition actionDefinition, String componentName,
        int componentVersion) {

        super(actionDefinition, componentName, componentVersion);

        this.output = OptionalUtils.mapOrElse(
            actionDefinition.getOutput(),
            output -> SchemaUtils.toOutput(
                output,
                (property, sampleOutput) -> new Output(
                    Property.toProperty((com.bytechef.component.definition.Property) property), sampleOutput)),
            null);
        this.outputDefined = OptionalUtils.mapOrElse(actionDefinition.getOutput(), output -> true, false);
        this.outputFunctionDefined = OptionalUtils.mapOrElse(
            actionDefinition.getOutputFunction(), outputFunction -> true, actionDefinition.isDefaultOutputFunction());
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

        return workflowNodeDescriptionDefined == that.workflowNodeDescriptionDefined
            && Objects.equals(output, that.output) && outputDefined == that.outputDefined
            && outputFunctionDefined == that.outputFunctionDefined && Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            super.hashCode(), output, outputDefined, outputFunctionDefined, properties, workflowNodeDescriptionDefined);
    }

    public Output getOutput() {
        return output;
    }

    public List<? extends Property> getProperties() {
        return properties;
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

    @Override
    public String toString() {
        return "Definition{" +
            "workflowNodeDescriptionDefined=" + workflowNodeDescriptionDefined +
            ", output=" + output +
            ", outputDefined=" + outputDefined +
            ", outputFunctionDefined=" + outputFunctionDefined +
            ", properties=" + properties +
            ", batch=" + batch +
            ", description='" + description + '\'' +
            ", help=" + help +
            ", name='" + name + '\'' +
            ", title='" + title + '\'' +
            "} ";
    }

}

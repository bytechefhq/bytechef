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
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ActionDefinition.StreamPerformFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsSseStreamResponsePerformFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsStreamPerformFunction;
import com.bytechef.platform.component.definition.PropertyFactory;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.util.SchemaUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class ActionDefinition {

    private boolean batch;
    private String componentName;
    private int componentVersion;
    private String description;
    private Help help;
    private String name;
    private boolean outputDefined;
    private boolean outputFunctionDefined;
    private OutputResponse outputResponse;
    private boolean outputSchemaDefined;
    private List<? extends Property> properties;
    boolean singleConnection;
    private boolean sseStreamResponse;
    private String title;
    private boolean workflowNodeDescriptionDefined;

    private ActionDefinition() {
    }

    public ActionDefinition(
        com.bytechef.component.definition.ActionDefinition actionDefinition, String componentName,
        int componentVersion) {

        this.batch = actionDefinition.getBatch()
            .orElse(false);
        this.componentName = componentName;
        this.componentVersion = componentVersion;
        this.description = Validate.notNull(getDescription(actionDefinition), "description");
        this.help = actionDefinition.getHelp()
            .map(Help::new)
            .orElse(null);
        this.name = Validate.notNull(actionDefinition.getName(), "name");
        this.outputDefined = actionDefinition.getOutputDefinition()
            .isPresent();
        this.outputFunctionDefined = actionDefinition.getOutputDefinition()
            .map(outputDefinition -> outputDefinition.getOutput()
                .isPresent())
            .orElse(false);
        this.outputResponse = actionDefinition.getOutputDefinition()
            .map(ActionDefinition::toOutputResponse)
            .orElse(null);
        this.outputSchemaDefined = outputResponse != null && outputResponse.outputSchema() != null;
        this.properties = CollectionUtils.map(
            actionDefinition.getProperties()
                .orElse(List.of()),
            Property::toProperty);
        this.singleConnection = actionDefinition.getPerform()
            .map(perform -> perform instanceof PerformFunction)
            .orElse(false);
        this.sseStreamResponse = actionDefinition.getPerform()
            .map(perform -> perform instanceof StreamPerformFunction ||
                perform instanceof MultipleConnectionsStreamPerformFunction ||
                perform instanceof MultipleConnectionsSseStreamResponsePerformFunction)
            .orElse(false);
        this.title = Validate.notNull(getTitle(actionDefinition), "title");
        this.workflowNodeDescriptionDefined = actionDefinition.getWorkflowNodeDescription()
            .isPresent();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ActionDefinition that)) {
            return false;
        }

        return batch == that.batch && componentVersion == that.componentVersion &&
            outputDefined == that.outputDefined && outputFunctionDefined == that.outputFunctionDefined &&
            Objects.equals(componentName, that.componentName) && Objects.equals(description, that.description) &&
            Objects.equals(help, that.help) && Objects.equals(name, that.name) &&
            Objects.equals(outputResponse, that.outputResponse) &&
            Objects.equals(outputSchemaDefined, that.outputSchemaDefined) &&
            Objects.equals(properties, that.properties) && sseStreamResponse == that.sseStreamResponse &&
            Objects.equals(title, that.title) && workflowNodeDescriptionDefined == that.workflowNodeDescriptionDefined;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            batch, componentName, componentVersion, description, help, name, outputDefined, outputFunctionDefined,
            outputResponse, outputSchemaDefined, properties, sseStreamResponse, title, workflowNodeDescriptionDefined);
    }

    public String getComponentName() {
        return componentName;
    }

    public int getComponentVersion() {
        return componentVersion;
    }

    public String getDescription() {
        return description;
    }

    public Help getHelp() {
        return help;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public OutputResponse getOutputResponse() {
        return outputResponse;
    }

    public List<? extends Property> getProperties() {
        return properties;
    }

    public String getTitle() {
        return title;
    }

    public boolean isBatch() {
        return batch;
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

    public boolean isSingleConnection() {
        return singleConnection;
    }

    public boolean isSseStreamResponse() {
        return sseStreamResponse;
    }

    public boolean isWorkflowNodeDescriptionDefined() {
        return workflowNodeDescriptionDefined;
    }

    @Override
    public String toString() {
        return "ActionDefinition{" +
            "name='" + name + '\'' +
            ", componentName='" + componentName + '\'' +
            ", componentVersion=" + componentVersion +
            ", title='" + title + '\'' +
            ", description='" + description + '\'' +
            ", properties=" + properties +
            ", batch=" + batch +
            ", outputDefined=" + outputDefined +
            ", outputFunctionDefined=" + outputFunctionDefined +
            ", outputResponse=" + outputResponse +
            ", outputResponseDefined=" + outputSchemaDefined +
            ", help=" + help +
            ", sseStreamResponse=" + sseStreamResponse +
            ", workflowNodeDescriptionDefined=" + workflowNodeDescriptionDefined +
            '}';
    }

    private static String getDescription(com.bytechef.component.definition.ActionDefinition actionDefinition) {
        return actionDefinition.getDescription()
            .orElse(
                actionDefinition.getTitle()
                    .orElse(actionDefinition.getName()));
    }

    private static String getTitle(com.bytechef.component.definition.ActionDefinition actionDefinition) {
        return actionDefinition.getTitle()
            .orElse(actionDefinition.getName());
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

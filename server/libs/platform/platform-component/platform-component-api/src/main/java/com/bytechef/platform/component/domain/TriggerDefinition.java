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
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
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
public class TriggerDefinition {

    protected boolean batch;
    protected String description;
    protected String componentName;
    protected int componentVersion;
    protected Help help;
    protected String name;
    protected boolean outputFunctionDefined;
    protected boolean outputDefined;
    private OutputResponse outputResponse;
    private boolean outputSchemaDefined;
    private List<? extends Property> properties;
    protected String title;
    protected TriggerType type;
    private boolean webhookRawBody;
    private boolean workflowNodeDescriptionDefined;
    private boolean workflowSyncExecution;
    private boolean workflowSyncValidation;
    private boolean workflowSyncOnEnableValidation;

    private TriggerDefinition() {
    }

    public TriggerDefinition(
        com.bytechef.component.definition.TriggerDefinition triggerDefinition, String componentName,
        int componentVersion) {

        this.batch = OptionalUtils.orElse(triggerDefinition.getBatch(), false);
        this.componentName = componentName;
        this.componentVersion = componentVersion;
        this.description = Validate.notNull(getDescription(triggerDefinition), "description");
        this.help = OptionalUtils.mapOrElse(triggerDefinition.getHelp(), Help::new, null);
        this.name = Validate.notNull(triggerDefinition.getName(), "name");
        this.outputDefined = OptionalUtils.mapOrElse(
            triggerDefinition.getOutputDefinition(), outputDefinition -> true, false);
        this.outputFunctionDefined = OptionalUtils.mapOrElse(
            triggerDefinition.getOutputDefinition(),
            outputDefinition -> OptionalUtils.mapOrElse(outputDefinition.getOutput(), output -> true, false), false);
        this.outputResponse = OptionalUtils.mapOrElse(
            triggerDefinition.getOutputDefinition(), TriggerDefinition::toOutputResponse, null);
        this.outputSchemaDefined = outputResponse != null && outputResponse.outputSchema() != null;
        this.properties = CollectionUtils.map(
            OptionalUtils.orElse(triggerDefinition.getProperties(), List.of()), Property::toProperty);
        this.title = Validate.notNull(getTitle(triggerDefinition), "title");
        this.type = Validate.notNull(triggerDefinition.getType(), "type");
        this.webhookRawBody = OptionalUtils.orElse(triggerDefinition.getWebhookRawBody(), false);
        this.workflowNodeDescriptionDefined = OptionalUtils.mapOrElse(
            triggerDefinition.getWorkflowNodeDescription(), nodeDescriptionFunction -> true, false);
        this.workflowSyncExecution = OptionalUtils.orElse(triggerDefinition.getWorkflowSyncExecution(), false);
        this.workflowSyncValidation = OptionalUtils.mapOrElse(triggerDefinition.getWebhookValidate(), v -> true, false);
        this.workflowSyncOnEnableValidation = OptionalUtils.mapOrElse(
            triggerDefinition.getWebhookValidateOnEnable(), v -> true, false);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TriggerDefinition that)) {
            return false;
        }

        return batch == that.batch && componentVersion == that.componentVersion &&
            outputFunctionDefined == that.outputFunctionDefined && outputDefined == that.outputDefined &&
            webhookRawBody == that.webhookRawBody &&
            workflowNodeDescriptionDefined == that.workflowNodeDescriptionDefined &&
            workflowSyncExecution == that.workflowSyncExecution &&
            workflowSyncValidation == that.workflowSyncValidation &&
            workflowSyncOnEnableValidation == that.workflowSyncOnEnableValidation &&
            Objects.equals(description, that.description) && Objects.equals(componentName, that.componentName) &&
            Objects.equals(help, that.help) && Objects.equals(name, that.name) &&
            Objects.equals(outputResponse, that.outputResponse) &&
            Objects.equals(outputSchemaDefined, that.outputSchemaDefined) &&
            Objects.equals(properties, that.properties) && Objects.equals(title, that.title) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            batch, description, componentName, componentVersion, help, name, outputFunctionDefined, outputDefined,
            outputResponse, outputSchemaDefined, properties, title, type, webhookRawBody,
            workflowNodeDescriptionDefined, workflowSyncExecution, workflowSyncValidation,
            workflowSyncOnEnableValidation);
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

    public TriggerType getType() {
        return type;
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

    public boolean isWebhookRawBody() {
        return webhookRawBody;
    }

    public boolean isWorkflowNodeDescriptionDefined() {
        return workflowNodeDescriptionDefined;
    }

    public boolean isWorkflowSyncExecution() {
        return workflowSyncExecution;
    }

    public boolean isWorkflowSyncValidation() {
        return workflowSyncValidation;
    }

    public boolean isWorkflowSyncOnEnableValidation() {
        return workflowSyncOnEnableValidation;
    }

    @Override
    public String toString() {
        return "TriggerDefinition{" +
            "name='" + name + '\'' +
            ", componentName='" + componentName + '\'' +
            ", componentVersion=" + componentVersion +
            ", type=" + type +
            ", title='" + title + '\'' +
            ", description='" + description + '\'' +
            ", properties=" + properties +
            ", batch=" + batch +
            ", outputDefined=" + outputDefined +
            ", outputFunctionDefined=" + outputFunctionDefined +
            ", outputResponse=" + outputResponse +
            ", outputResponseDefined=" + outputSchemaDefined +
            ", webhookRawBody=" + webhookRawBody +
            ", workflowSyncExecution=" + workflowSyncExecution +
            ", workflowSyncValidation=" + workflowSyncValidation +
            ", workflowSyncOnEnableValidation=" + workflowSyncOnEnableValidation +
            ", workflowNodeDescriptionDefined=" + workflowNodeDescriptionDefined +
            ", help=" + help +
            '}';
    }

    private static String getDescription(com.bytechef.component.definition.TriggerDefinition triggerDefinition) {
        return OptionalUtils.orElse(triggerDefinition.getDescription(), getTitle(triggerDefinition));
    }

    private static String getTitle(com.bytechef.component.definition.TriggerDefinition triggerDefinition) {
        return OptionalUtils.orElse(triggerDefinition.getTitle(), triggerDefinition.getName());
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

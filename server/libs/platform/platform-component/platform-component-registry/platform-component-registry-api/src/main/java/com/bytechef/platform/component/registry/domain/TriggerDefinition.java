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
public class TriggerDefinition extends TriggerDefinitionBasic {

    private Output output;
    private boolean outputDefined;
    private boolean dynamicOutput;
    private List<? extends Property> properties;
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

        super(triggerDefinition, componentName, componentVersion);

        this.dynamicOutput = OptionalUtils.mapOrElse(
            triggerDefinition.getOutput(), outputFunction -> true, triggerDefinition.isDynamicOutput());
        this.output = OptionalUtils.mapOrElse(
            triggerDefinition.getOutputResponse(),
            outputResponse -> SchemaUtils.toOutput(
                outputResponse,
                (property, sampleOutput) -> new Output(
                    Property.toProperty((com.bytechef.component.definition.Property) property), sampleOutput)),
            null);
        this.outputDefined = OptionalUtils.mapOrElse(triggerDefinition.getOutputResponse(), output -> true, false);
        this.properties = CollectionUtils.map(
            OptionalUtils.orElse(triggerDefinition.getProperties(), List.of()), Property::toProperty);
        this.webhookRawBody = OptionalUtils.orElse(triggerDefinition.getWebhookRawBody(), false);
        this.workflowNodeDescriptionDefined = OptionalUtils.mapOrElse(
            triggerDefinition.getWorkflowNodeDescription(), nodeDescriptionFunction -> true, false);
        this.workflowSyncExecution = OptionalUtils.orElse(triggerDefinition.getWorkflowSyncExecution(), false);
        this.workflowSyncValidation = OptionalUtils.orElse(triggerDefinition.getWorkflowSyncValidation(), false);
        this.workflowSyncOnEnableValidation = OptionalUtils.orElse(
            triggerDefinition.getWorkflowSyncOnEnableValidation(), false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof TriggerDefinition that)) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        return dynamicOutput == that.dynamicOutput && output == that.output && outputDefined == that.outputDefined
            && Objects.equals(properties, that.properties) && webhookRawBody == that.webhookRawBody
            && workflowNodeDescriptionDefined == that.workflowNodeDescriptionDefined
            && workflowSyncExecution == that.workflowSyncExecution
            && workflowSyncValidation == that.workflowSyncValidation
            && workflowSyncOnEnableValidation == that.workflowSyncOnEnableValidation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            super.hashCode(), dynamicOutput, output, outputDefined, properties,
            webhookRawBody, workflowNodeDescriptionDefined, workflowSyncExecution, workflowSyncValidation,
            workflowSyncOnEnableValidation);
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

    public boolean isDynamicOutput() {
        return dynamicOutput;
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
            ", title='" + title + '\'' +
            ", type=" + type +
            ", description='" + description + '\'' +
            ", output=" + output +
            ", outputDefined=" + outputDefined +
            ", outputFunctionDefined=" + dynamicOutput +
            ", properties=" + properties +
            ", webhookRawBody=" + webhookRawBody +
            ", workflowSyncExecution=" + workflowSyncExecution +
            ", workflowSyncValidation=" + workflowSyncValidation +
            ", batch=" + batch +
            ", help=" + help +
            ", workflowNodeDescriptionDefined=" + workflowNodeDescriptionDefined +
            "} ";
    }
}

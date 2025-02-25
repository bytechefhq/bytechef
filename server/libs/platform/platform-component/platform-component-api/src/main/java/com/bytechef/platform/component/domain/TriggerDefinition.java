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
public class TriggerDefinition extends TriggerDefinitionBasic {

    private OutputResponse outputResponse;
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

        this.outputResponse = OptionalUtils.mapOrElse(
            triggerDefinition.getOutputDefinition(), TriggerDefinition::toOutputResponse, null);
        this.properties = CollectionUtils.map(
            OptionalUtils.orElse(triggerDefinition.getProperties(), List.of()), Property::toProperty);
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
        if (this == o) {
            return true;
        }

        if (!(o instanceof TriggerDefinition that)) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        return outputDefined == that.outputDefined && outputFunctionDefined == that.outputFunctionDefined &&
            Objects.equals(outputResponse, that.outputResponse) && Objects.equals(properties, that.properties) &&
            webhookRawBody == that.webhookRawBody &&
            workflowNodeDescriptionDefined == that.workflowNodeDescriptionDefined &&
            workflowSyncExecution == that.workflowSyncExecution &&
            workflowSyncValidation == that.workflowSyncValidation &&
            workflowSyncOnEnableValidation == that.workflowSyncOnEnableValidation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            super.hashCode(), outputDefined, outputFunctionDefined, outputResponse, properties, webhookRawBody,
            workflowNodeDescriptionDefined, workflowSyncExecution, workflowSyncValidation,
            workflowSyncOnEnableValidation);
    }

    @Nullable
    public OutputResponse getOutputResponse() {
        return outputResponse;
    }

    public List<? extends Property> getProperties() {
        return properties;
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
            ", batch=" + batch +
            ", title='" + title + '\'' +
            ", description='" + description + '\'' +
            ", outputDefined=" + outputDefined +
            ", outputDefinition=" + outputResponse +
            ", properties=" + properties +
            ", webhookRawBody=" + webhookRawBody +
            ", workflowSyncExecution=" + workflowSyncExecution +
            ", workflowSyncValidation=" + workflowSyncValidation +
            ", help=" + help +
            ", workflowNodeDescriptionDefined=" + workflowNodeDescriptionDefined +
            "} ";
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

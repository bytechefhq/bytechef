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

    private boolean editorDescriptionDefined;
    private Output output;
    private boolean outputDefined;
    private List<? extends Property> properties;
    private boolean webhookRawBody;
    private boolean workflowSyncExecution;
    private boolean workflowSyncValidation;

    private TriggerDefinition() {
    }

    public TriggerDefinition(com.bytechef.component.definition.TriggerDefinition triggerDefinition) {
        super(triggerDefinition);

        this.editorDescriptionDefined = OptionalUtils.mapOrElse(
            triggerDefinition.getEditorDescriptionFunction(), editorDescriptionDataSource -> true, false);
        this.output = OptionalUtils.mapOrElse(
            triggerDefinition.getOutput(),
            outputSchema -> SchemaUtils.toOutputSchema(
                outputSchema,
                (baseValueProperty, sampleOutput) -> new Output(
                    Property.toProperty((com.bytechef.component.definition.Property) baseValueProperty), sampleOutput)),
            null);
        this.outputDefined = OptionalUtils.mapOrElse(
            triggerDefinition.getOutput(), outputSchema -> true, false) ||
            OptionalUtils.mapOrElse(
                triggerDefinition.getOutputFunction(), outputSchemaDataSource -> true,
                triggerDefinition.isDefaultOutputFunction());
        this.properties = CollectionUtils.map(
            OptionalUtils.orElse(triggerDefinition.getProperties(), List.of()), Property::toProperty);
        this.webhookRawBody = OptionalUtils.orElse(triggerDefinition.getWebhookRawBody(), false);
        this.workflowSyncExecution = OptionalUtils.orElse(triggerDefinition.getWorkflowSyncExecution(), false);
        this.workflowSyncValidation = OptionalUtils.orElse(triggerDefinition.getWorkflowSyncValidation(), false);
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

        return editorDescriptionDefined == that.editorDescriptionDefined
            && outputDefined == that.outputDefined && webhookRawBody == that.webhookRawBody
            && workflowSyncExecution == that.workflowSyncExecution
            && workflowSyncValidation == that.workflowSyncValidation && Objects.equals(output, that.output)
            && Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), editorDescriptionDefined, output, outputDefined,
            properties, webhookRawBody, workflowSyncExecution,
            workflowSyncValidation);
    }

    public Output getOutput() {
        return output;
    }

    public List<? extends Property> getProperties() {
        return properties;
    }

    public boolean isEditorDescriptionDefined() {
        return editorDescriptionDefined;
    }

    public boolean isOutputDefined() {
        return outputDefined;
    }

    public boolean isWebhookRawBody() {
        return webhookRawBody;
    }

    public boolean isWorkflowSyncExecution() {
        return workflowSyncExecution;
    }

    public boolean isWorkflowSyncValidation() {
        return workflowSyncValidation;
    }

    @Override
    public String toString() {
        return "TriggerDefinition{" +
            "editorDescriptionDefined=" + editorDescriptionDefined +
            ", outputDefined=" + outputDefined +
            ", outputSchema=" + output +
            ", properties=" + properties +
            ", webhookRawBody=" + webhookRawBody +
            ", workflowSyncExecution=" + workflowSyncExecution +
            ", workflowSyncValidation=" + workflowSyncValidation +
            ", batch=" + batch +
            ", description='" + description + '\'' +
            ", help=" + help +
            ", name='" + name + '\'' +
            ", title='" + title + '\'' +
            ", type=" + type +
            "} ";
    }
}


/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.definition.registry.dto;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.hermes.component.definition.TriggerDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public class TriggerDefinitionDTO extends TriggerDefinitionBasicDTO {

    private final boolean editorDescriptionDataSource;
    private final PropertyDTO outputSchema;
    private final boolean outputSchemaDataSource;
    private final List<? extends PropertyDTO> properties;
    private final Object sampleOutput;
    private final boolean sampleOutputDataSource;
    private final boolean webhookRawBody;
    private final boolean workflowSyncExecution;
    private final boolean workflowSyncValidation;

    public TriggerDefinitionDTO(TriggerDefinition triggerDefinition) {
        super(triggerDefinition);
        this.editorDescriptionDataSource = OptionalUtils.mapOrElse(
            triggerDefinition.getEditorDescriptionDataSource(), editorDescriptionDataSource -> true, false);
        this.outputSchema = OptionalUtils.mapOrElse(
            triggerDefinition.getOutputSchema(), PropertyDTO::toPropertyDTO, null);
        this.outputSchemaDataSource = OptionalUtils.mapOrElse(
            triggerDefinition.getOutputSchemaDataSource(), outputSchemaDataSource -> true, false);
        this.properties = CollectionUtils.map(
            OptionalUtils.orElse(triggerDefinition.getProperties(), List.of()), PropertyDTO::toPropertyDTO);
        this.sampleOutput = triggerDefinition.getSampleOutput();
        this.sampleOutputDataSource = OptionalUtils.mapOrElse(
            triggerDefinition.getSampleOutputDataSource(), sampleOutputDataSource -> true, false);
        this.webhookRawBody = OptionalUtils.orElse(triggerDefinition.getWebhookRawBody(), false);
        this.workflowSyncExecution = OptionalUtils.orElse(triggerDefinition.getWorkflowSyncExecution(), false);
        this.workflowSyncValidation = OptionalUtils.orElse(triggerDefinition.getWorkflowSyncValidation(), false);
    }

    public boolean isEditorDescriptionDataSource() {
        return editorDescriptionDataSource;
    }

    public Optional<PropertyDTO> getOutputSchema() {
        return Optional.ofNullable(outputSchema);
    }

    public boolean isOutputSchemaDataSource() {
        return outputSchemaDataSource;
    }

    public List<? extends PropertyDTO> getProperties() {
        return properties;
    }

    public Object getSampleOutput() {
        return sampleOutput;
    }

    public boolean isSampleOutputDataSource() {
        return sampleOutputDataSource;
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
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TriggerDefinitionDTO that))
            return false;
        if (!super.equals(o))
            return false;
        return editorDescriptionDataSource == that.editorDescriptionDataSource
            && outputSchemaDataSource == that.outputSchemaDataSource
            && sampleOutputDataSource == that.sampleOutputDataSource && webhookRawBody == that.webhookRawBody
            && workflowSyncExecution == that.workflowSyncExecution
            && workflowSyncValidation == that.workflowSyncValidation && Objects.equals(outputSchema, that.outputSchema)
            && Objects.equals(properties, that.properties) && Objects.equals(sampleOutput, that.sampleOutput);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), editorDescriptionDataSource, outputSchema, outputSchemaDataSource,
            properties, sampleOutput, sampleOutputDataSource, webhookRawBody, workflowSyncExecution,
            workflowSyncValidation);
    }

    @Override
    public String toString() {
        return "TriggerDefinitionDTO{" +
            "editorDescriptionDataSource=" + editorDescriptionDataSource +
            ", outputSchema=" + outputSchema +
            ", outputSchemaDataSource=" + outputSchemaDataSource +
            ", properties=" + properties +
            ", sampleOutput=" + sampleOutput +
            ", sampleOutputDataSource=" + sampleOutputDataSource +
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

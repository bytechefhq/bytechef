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

package com.bytechef.component.datatable.trigger;

import static com.bytechef.component.datatable.constant.DataTableConstants.TABLE;
import static com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.definition.TriggerDefinition.TriggerType.DYNAMIC_WEBHOOK;

import com.bytechef.automation.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.automation.data.table.configuration.service.DataTableService;
import com.bytechef.automation.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.automation.data.table.execution.service.DataTableRowService;
import com.bytechef.component.datatable.util.DataTableUtils;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.OptionsFunction;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.platform.component.definition.TriggerContextAware;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Objects;

/**
 * Record Deleted trigger for Data Tables.
 *
 * @author Ivica Cardic
 */
public class DataTableRecordDeletedTrigger {

    private final DataTableRowService dataTableRowService;
    private final DataTableService dataTableService;
    private final DataTableWebhookService dataTableWebhookService;

    @SuppressFBWarnings("EI")
    public static ModifiableTriggerDefinition of(
        DataTableRowService dataTableRowService, DataTableService dataTableService,
        DataTableWebhookService dataTableWebhookService) {

        return new DataTableRecordDeletedTrigger(
            dataTableRowService, dataTableService, dataTableWebhookService).build();
    }

    private DataTableRecordDeletedTrigger(
        DataTableRowService dataTableRowService, DataTableService dataTableService,
        DataTableWebhookService dataTableWebhookService) {

        this.dataTableRowService = dataTableRowService;
        this.dataTableService = dataTableService;
        this.dataTableWebhookService = dataTableWebhookService;
    }

    private ModifiableTriggerDefinition build() {
        return trigger("recordDeleted")
            .title("Record Deleted")
            .description("Triggers when a record is deleted from the selected table.")
            .type(DYNAMIC_WEBHOOK)
            .properties(
                string(TABLE)
                    .label("Table")
                    .description("Select a Data Table.")
                    .required(true)
                    .options(
                        (OptionsFunction<String>) (
                            inputParameters, connectionParameters, depends, searchText,
                            context) -> DataTableUtils.getTableOptions(searchText, dataTableService)))
            .output((inputParameters, connectionParameters, context) -> {
                var baseName = inputParameters.getRequiredString(TABLE);

                return DataTableUtils.createTriggerOutputResponse(dataTableRowService, dataTableService, baseName);
            })
            .webhookEnable((
                inputParameters, connectionParameters, webhookUrl, workflowExecutionId,
                context) -> webhookEnable(inputParameters, webhookUrl, context))
            .webhookDisable((
                inputParameters, connectionParameters, webhookEnableOutputParameters, workflowExecutionId,
                context) -> webhookDisable(webhookEnableOutputParameters))
            .webhookRequest((
                inputParameters, connectionParameters, headers, parameters, body, method, webhookEnableOutputParameters,
                context) -> webhookRequest(body));
    }

    private WebhookEnableOutput webhookEnable(
        Parameters inputParameters, String webhookUrl, TriggerContext triggerContext) {

        TriggerContextAware triggerContextAware = (TriggerContextAware) triggerContext;

        String baseName = inputParameters.getRequiredString(TABLE);

        long webhookId = dataTableWebhookService.addWebhook(
            baseName, webhookUrl, DataTableWebhookType.RECORD_DELETED,
            Objects.requireNonNull(triggerContextAware.getEnvironmentId()));

        return new WebhookEnableOutput(Map.of("webhookId", webhookId), null);
    }

    private void webhookDisable(Parameters webhookEnableOutputParameters) {
        long webhookId = webhookEnableOutputParameters.getRequiredLong("webhookId");

        dataTableWebhookService.removeWebhook(webhookId);
    }

    private Object webhookRequest(WebhookBody body) {
        Map<String, Object> content = body.getContent(new TypeReference<>() {});

        Object payload = content.get("payload");

        return payload != null ? payload : content;
    }
}

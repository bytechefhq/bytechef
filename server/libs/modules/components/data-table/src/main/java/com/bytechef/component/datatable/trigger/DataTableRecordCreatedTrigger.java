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
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.definition.BaseOutputDefinition.OutputResponse;
import com.bytechef.platform.component.definition.TriggerContextAware;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Objects;

/**
 * Record Created trigger for Data Tables.
 *
 * @author Ivica Cardic
 */
public class DataTableRecordCreatedTrigger {

    private final DataTableRowService dataTableRowService;
    private final DataTableService dataTableService;
    private final DataTableWebhookService dataTableWebhookService;

    @SuppressFBWarnings("EI")
    public static ModifiableTriggerDefinition of(
        DataTableRowService dataTableRowService, DataTableService dataTableService,
        DataTableWebhookService dataTableWebhookService) {

        return new DataTableRecordCreatedTrigger(
            dataTableRowService, dataTableService, dataTableWebhookService).build();
    }

    private DataTableRecordCreatedTrigger(
        DataTableRowService dataTableRowService, DataTableService dataTableService,
        DataTableWebhookService dataTableWebhookService) {

        this.dataTableRowService = dataTableRowService;
        this.dataTableService = dataTableService;
        this.dataTableWebhookService = dataTableWebhookService;
    }

    private ModifiableTriggerDefinition build() {
        return trigger("recordCreated")
            .title("Record Created")
            .description("Triggers when a new record is inserted into the selected table.")
            .type(DYNAMIC_WEBHOOK)
            .properties(
                string(TABLE)
                    .label("Table")
                    .description("Select a Data Table.")
                    .required(true)
                    .options(DataTableUtils.getTriggerTableOptions(dataTableService)))
            .output(this::output)
            .webhookEnable(this::webhookEnable)
            .webhookDisable(this::webhookDisable)
            .webhookRequest(this::webhookRequest);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private OutputResponse output(
        Parameters inputParameters, Parameters connectionParameters, TriggerContext triggerContext) {

        var baseName = inputParameters.getRequiredString(TABLE);

        return DataTableUtils.createTriggerOutputResponse(dataTableRowService, dataTableService, baseName);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl, String workflowExecutionId,
        TriggerContext triggerContext) {

        TriggerContextAware triggerContextAware = (TriggerContextAware) triggerContext;

        String baseName = inputParameters.getRequiredString(TABLE);

        long webhookId = dataTableWebhookService.addWebhook(
            baseName, webhookUrl, DataTableWebhookType.RECORD_CREATED,
            Objects.requireNonNull(triggerContextAware.getEnvironmentId()));

        return new WebhookEnableOutput(Map.of("webhookId", webhookId), null);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters webhookEnableOutputParameters,
        String workflowExecutionId, TriggerContext triggerContext) {

        long webhookId = webhookEnableOutputParameters.getRequiredLong("webhookId");

        dataTableWebhookService.removeWebhook(webhookId);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private Object webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters webhookEnableOutputParameters,
        TriggerContext triggerContext) {

        Map<String, Object> content = body.getContent(new TypeReference<>() {});

        Object payload = content.get("payload");

        return payload != null ? payload : content;
    }
}

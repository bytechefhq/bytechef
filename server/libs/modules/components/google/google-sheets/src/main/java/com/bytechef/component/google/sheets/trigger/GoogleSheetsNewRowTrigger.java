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

package com.bytechef.component.google.sheets.trigger;

import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.definition.TriggerContext.Data.Scope.WORKFLOW;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.APPLICATION_VND_GOOGLE_APPS_SPREADSHEET;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.IS_THE_FIRST_ROW_HEADER_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.getMapOfValuesForRowAndColumn;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.OptionsFunction;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.google.sheets.util.GoogleSheetsUtils;
import com.bytechef.google.commons.GoogleServices;
import com.bytechef.google.commons.GoogleUtils;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Channel;
import com.google.api.services.sheets.v4.Sheets;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Marko Kriskovic
 */
public class GoogleSheetsNewRowTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newRow")
        .title("New Row")
        .description("Triggers when a new row is added.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            string(SPREADSHEET_ID)
                .label("Spreadsheet")
                .description("The spreadsheet to apply the updates to.")
                .options(
                    GoogleUtils.getFileOptionsByMimeTypeForTriggers(APPLICATION_VND_GOOGLE_APPS_SPREADSHEET, true))
                .required(true),
            IS_THE_FIRST_ROW_HEADER_PROPERTY,
            string(SHEET_NAME)
                .label("Sheet")
                .description("The name of the sheet")
                .options((OptionsFunction<String>) GoogleSheetsUtils::getSheetNameOptions)
                .optionsLookupDependsOn(SPREADSHEET_ID)
                .required(true))
        .output()
        .webhookEnable(GoogleSheetsNewRowTrigger::webhookEnable)
        .webhookDisable(GoogleSheetsNewRowTrigger::webhookDisable)
        .webhookRequest(GoogleSheetsNewRowTrigger::webhookRequest);

    private GoogleSheetsNewRowTrigger() {
    }

    protected static WebhookEnableOutput webhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, TriggerContext context) {

        Drive drive = GoogleServices.getDrive(connectionParameters);
        String fileId = inputParameters.getRequiredString(SPREADSHEET_ID);

        String uuid = String.valueOf(UUID.randomUUID());

        Channel channelConfig = new Channel()
            .setAddress(webhookUrl)
            .setId(uuid)
            .setPayload(true)
            .setType("web_hook");

        Channel channel;

        try {
            channel = drive.files()
                .watch(fileId, channelConfig)
                .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new WebhookEnableOutput(
            Map.of("id", channel.getId(), "resourceId", channel.getResourceId()), null);
    }

    protected static void webhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, TriggerContext context) {

        Drive drive = GoogleServices.getDrive(connectionParameters);

        Channel channelConfig = new Channel()
            .setId(outputParameters.getRequiredString("id"))
            .setResourceId(outputParameters.getRequiredString("resourceId"));

        try {
            drive.channels()
                .stop(channelConfig)
                .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected static List<Map<String, Object>> webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers,
        HttpParameters parameters, WebhookBody body, WebhookMethod method, WebhookEnableOutput output,
        TriggerContext context) {

        Sheets sheets = GoogleServices.getSheets(connectionParameters);

        Optional<Object> currentRowNumOptional = context.data(data -> data.fetch(WORKFLOW, "currentRow"));

        int currentRowNum = currentRowNumOptional.map(o -> Integer.parseInt(o.toString()))
            .orElse(0);

        List<List<Object>> values = GoogleSheetsUtils.getSpreadsheetValues(
            sheets, inputParameters.getRequiredString(SPREADSHEET_ID), inputParameters.getRequiredString(SHEET_NAME));

        if (values == null) {
            return Collections.emptyList();
        } else {
            context.data(data -> data.put(WORKFLOW, "currentRow", values.size()));

            return getMapOfValuesForRowAndColumn(inputParameters, sheets, values, currentRowNum, values.size());
        }
    }
}

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

package com.bytechef.component.google.sheets.trigger;

import static com.bytechef.component.definition.ActionContext.Data.Scope.WORKFLOW;
import static com.bytechef.component.definition.ComponentDSL.*;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.IS_THE_FIRST_ROW_HEADER_PROPERTY;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SHEET_NAME_PROPERTY_TRIGGER;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID;
import static com.bytechef.component.google.sheets.constant.GoogleSheetsConstants.SPREADSHEET_ID_PROPERTY_TRIGGER;
import static com.bytechef.component.google.sheets.util.GoogleSheetsUtils.getMapOfValuesForRowAndColumn;

import com.bytechef.component.definition.ComponentDSL;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.DynamicWebhookEnableOutput;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.google.sheets.util.GoogleSheetsUtils;
import com.bytechef.google.commons.GoogleServices;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Channel;
import com.google.api.services.sheets.v4.Sheets;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class GoogleSheetsOnRowAdded {
    private static final String ON_ROW_ADDED = "onRowAdded";
    public static final ComponentDSL.ModifiableTriggerDefinition TRIGGER_DEFINITION = ComponentDSL.trigger(ON_ROW_ADDED)
        .title("OnRowAdded")
        .description("Triggers when you add a row in google sheets. Refresh the page when you're done putting input.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(
            SPREADSHEET_ID_PROPERTY_TRIGGER,
            IS_THE_FIRST_ROW_HEADER_PROPERTY,
            SHEET_NAME_PROPERTY_TRIGGER)
        .outputSchema(
            array()
                .items(object()
                    .additionalProperties(string(), array().items(string()))))
        .dynamicWebhookEnable(GoogleSheetsOnRowAdded::dynamicWebhookEnable)
        .dynamicWebhookDisable(GoogleSheetsOnRowAdded::dynamicWebhookDisable)
        .dynamicWebhookRequest(GoogleSheetsOnRowAdded::dynamicWebhookRequest);

    protected static DynamicWebhookEnableOutput dynamicWebhookEnable(
        Parameters inputParameters, Parameters connectionParameters, String webhookUrl,
        String workflowExecutionId, Context context) {

        Drive drive = GoogleServices.getDrive(connectionParameters);
        String fileId = inputParameters.getRequiredString(SPREADSHEET_ID);

        String uuid = UUID.randomUUID()
            .toString();

        Channel channelConfig = new Channel()
            .setAddress(webhookUrl)
            .setId(uuid)
            .setPayload(true)
            .setType("web_hook");

        Channel channel = null;
        try {
            channel = drive.files()
                .watch(fileId, channelConfig)
                .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new DynamicWebhookEnableOutput(Map.of("id", channel.getId(), "resourceId", channel.getResourceId()),
            null);
    }

    protected static void dynamicWebhookDisable(
        Parameters inputParameters, Parameters connectionParameters, Parameters outputParameters,
        String workflowExecutionId, Context context) {

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

    protected static List<Map<String, Object>> dynamicWebhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers,
        HttpParameters parameters, WebhookBody body, WebhookMethod method, DynamicWebhookEnableOutput output,
        TriggerContext context) throws IOException {

        Sheets sheets = GoogleServices.getSheets(connectionParameters);

        Optional<Object> currentRowNumOptional =
            context.data(data -> data.fetchValue(WORKFLOW, "currentRow"));
        int currentRowNum = currentRowNumOptional.map(o -> Integer.parseInt(o.toString()))
            .orElse(0);

        List<List<Object>> values = GoogleSheetsUtils.getAll(sheets, inputParameters.getRequiredString(SPREADSHEET_ID),
            inputParameters.getRequiredString(SHEET_NAME));

        if (values == null)
            return Collections.emptyList();

        context.data(data -> data.setValue(WORKFLOW, "currentRow", values.size()));

        return getMapOfValuesForRowAndColumn(inputParameters, sheets, values, currentRowNum, values.size());
    }
}

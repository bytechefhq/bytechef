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

import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.google.sheets.util.GoogleSheetsRowDiffUtils;
import com.bytechef.component.google.sheets.util.GoogleSheetsRowTriggerUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Marko Kriskovic
 */
public class GoogleSheetsModifiedRowTriggerV2 {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("modifiedRow")
        .title("Modified Row")
        .description(
            "Triggers when an existing row is modified. Rows are tracked by their position, so only rows whose " +
                "content changes at the same position are reported; rows that are inserted, removed, or moved " +
                "do not fire the trigger.")
        .type(TriggerType.DYNAMIC_WEBHOOK)
        .properties(GoogleSheetsRowTriggerUtils.getSpreadsheetAndSheetProperties())
        .output()
        .webhookEnable(GoogleSheetsNewRowTrigger::webhookEnable)
        .webhookDisable(GoogleSheetsNewRowTrigger::webhookDisable)
        .webhookRequest(GoogleSheetsModifiedRowTriggerV2::webhookRequest)
        .help("", "https://docs.bytechef.io/reference/components/google-sheets_v2#modified-row");

    private GoogleSheetsModifiedRowTriggerV2() {
    }

    protected static List<Map<String, Object>> webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers,
        HttpParameters parameters, WebhookBody body, WebhookMethod method, Parameters output,
        TriggerContext context) {

        return GoogleSheetsRowTriggerUtils.getChangedRows(
            inputParameters, connectionParameters, context, GoogleSheetsRowDiffUtils::getModifiedRowIndexes);
    }
}

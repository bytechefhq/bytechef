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

package com.bytechef.component.microsoft.excel.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.ID;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.NAME;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKBOOK_ID;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKBOOK_ID_PROPERTY;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.microsoft.commons.MicrosoftUtils;

/**
 * @author Monika Kušter
 */
public class MicrosoftExcelCreateWorksheetAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createWorksheet")
        .title("Create Worksheet")
        .description("Creates a new worksheet in the specified workbook.")
        .help("", "https://docs.bytechef.io/reference/components/microsoft-excel_v1#create-worksheet")
        .properties(
            WORKBOOK_ID_PROPERTY,
            string(NAME)
                .label("Worksheet Name")
                .description("The name of the new worksheet.")
                .required(false))
        .perform(MicrosoftExcelCreateWorksheetAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse)
        .output(
            outputSchema(
                object()
                    .properties(
                        string("@odata.context")
                            .description("The OData context URL."),
                        string("@odata.id")
                            .description("The OData ID."),
                        string(ID)
                            .description("The ID of the new worksheet."),
                        integer("position")
                            .description("The position of the new worksheet."),
                        string(NAME)
                            .description("The name of the new worksheet."),
                        string("visibility")
                            .description("The visibility of the new worksheet."))));

    private MicrosoftExcelCreateWorksheetAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        return context.http(
            http -> http.post(
                "/me/drive/items/%s/workbook/worksheets/add".formatted(inputParameters.getRequiredString(WORKBOOK_ID))))
            .body(Body.of(NAME, inputParameters.get(NAME)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody();
    }
}

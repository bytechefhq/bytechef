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
import static com.bytechef.microsoft.commons.MicrosoftConstants.ODATA_NEXT_LINK;
import static com.bytechef.microsoft.commons.MicrosoftConstants.VALUE;
import static com.bytechef.microsoft.commons.MicrosoftUtils.getItemsFromNextPage;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.microsoft.commons.MicrosoftUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftExcelListWorksheetsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("listWorksheets")
        .title("List Worksheets")
        .description("List all worksheets in the specified workbook.")
        .help("", "https://docs.bytechef.io/reference/components/microsoft-excel_v1#list-worksheets")
        .properties(WORKBOOK_ID_PROPERTY)
        .perform(MicrosoftExcelListWorksheetsAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse)
        .output(
            outputSchema(
                object()
                    .properties(
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

    private MicrosoftExcelListWorksheetsAction() {
    }

    public static List<Map<?, ?>> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Map<String, ?> body = context.http(
            http -> http.get(
                "/me/drive/items/%s/workbook/worksheets".formatted(inputParameters.getRequiredString(WORKBOOK_ID))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Map<?, ?>> worksheets = new ArrayList<>();

        if (body.get(VALUE) instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    worksheets.add(map);
                }
            }
        }

        worksheets.addAll(getItemsFromNextPage((String) body.get(ODATA_NEXT_LINK), context));

        return worksheets;
    }
}

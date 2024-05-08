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

package com.bytechef.component.microsoft.excel.util;

import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.BASE_URL;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.VALUES;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKBOOK_ID;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKBOOK_WORKSHEETS_PATH;
import static com.bytechef.component.microsoft.excel.constant.MicrosoftExcelConstants.WORKSHEET_NAME;
import static com.bytechef.component.microsoft.excel.util.MicrosoftExcelUtils.columnToLabel;
import static com.bytechef.component.microsoft.excel.util.MicrosoftExcelUtils.getMapOfValuesForRow;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class MicrosoftExcelUpdateWorksheetUtils {

    private MicrosoftExcelUpdateWorksheetUtils() {
    }

    public static Map<String, Object> updateRange(
        Parameters inputParameters, ActionContext context, int rowNumber, List<Object> rowValues) {

        String range = "A" + rowNumber + ":" + columnToLabel(rowValues.size(), false) + rowNumber;

        context
            .http(http -> http.patch(BASE_URL + "/" + inputParameters.getRequiredString(WORKBOOK_ID) +
                WORKBOOK_WORKSHEETS_PATH + inputParameters.getRequiredString(WORKSHEET_NAME) +
                "/range(address='" + range + "')"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .body(Http.Body.of(VALUES, List.of(rowValues)))
            .execute();

        return getMapOfValuesForRow(inputParameters, context, rowValues);
    }
}

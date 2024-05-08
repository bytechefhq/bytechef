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

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class MicrosoftExcelRowUtils {

    private MicrosoftExcelRowUtils() {
    }

    public static List<Object>
        getRowFromWorksheet(Parameters inputParameters, ActionContext context, Integer rowNumber) {
        String range =
            "A" + rowNumber + ":" + MicrosoftExcelUtils.getLastUsedColumnLabel(inputParameters, context) + rowNumber;

        Map<String, Object> body = context
            .http(http -> http.get(BASE_URL + "/" +
                inputParameters.getRequiredString(WORKBOOK_ID) + WORKBOOK_WORKSHEETS_PATH +
                inputParameters.getRequiredString(WORKSHEET_NAME) + "/range(address='" + range + "')"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Object> row = new ArrayList<>();

        if (body.get(VALUES) instanceof List<?> list && list.getFirst() instanceof List<?> firstRow) {
            row.addAll(firstRow);
        }

        return row;
    }

}

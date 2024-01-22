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

package com.bytechef.component.google.sheets.util;

import com.google.api.services.sheets.v4.Sheets;
import java.io.IOException;
import java.util.List;

/**
 * @author Monika Domiter
 */
public class GoogleSheetsRowUtils {

    private GoogleSheetsRowUtils() {
    }

    public static List<Object> getRow(Sheets sheets, String spreadSheetId, Integer sheetId, Integer rowNumber)
        throws IOException {

        return sheets.spreadsheets()
            .values()
            .batchGet(spreadSheetId)
            .setRanges(List.of(GoogleSheetsUtils.createRange(sheetId, rowNumber)))
            .setValueRenderOption("UNFORMATTED_VALUE")
            .setDateTimeRenderOption("FORMATTED_STRING")
            .setMajorDimension("ROWS")
            .execute()
            .getValueRanges()
            .getFirst()
            .getValues()
            .getFirst();
    }
}

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

package com.bytechef.component.xlsx.file.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.nullable;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.time;
import static com.bytechef.component.xlsx.file.constant.XlsxFileConstants.FILENAME;
import static com.bytechef.component.xlsx.file.constant.XlsxFileConstants.ROWS;
import static com.bytechef.component.xlsx.file.constant.XlsxFileConstants.SHEET_NAME;
import static com.bytechef.component.xlsx.file.constant.XlsxFileConstants.WRITE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.xlsx.file.constant.XlsxFileConstants;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * @author Ivica Cardic
 */
public class XlsxFileWriteAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(WRITE)
        .title("Write to file")
        .description("Writes the data to a XLS/XLSX file.")
        .properties(
            string(SHEET_NAME)
                .label("Sheet Name")
                .description("The name of the sheet to create in the spreadsheet.")
                .defaultValue("Sheet")
                .advancedOption(true),
            array(ROWS)
                .label("Rows")
                .description("The array of objects to write to the file.")
                .required(true)
                .items(
                    object()
                        .additionalProperties(
                            bool(), date(), dateTime(), integer(), nullable(), number(), string(), time())),
            string(FILENAME)
                .label("Filename")
                .description(
                    "Filename to set for binary data. By default, \"file.xlsx\" will be used.")
                .required(true)
                .defaultValue("file.xlsx")
                .advancedOption(true))
        .output()
        .perform(XlsxFileWriteAction::perform);

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    protected static FileEntry perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String fileName = inputParameters.getString(FILENAME, getaDefaultFileName());
        List<Map<String, ?>> rows = (List) inputParameters.getList(ROWS, List.of());
        String sheetName = inputParameters.getString(SHEET_NAME, "Sheet");

        return context.file(file -> file.storeContent(
            fileName, new ByteArrayInputStream(write(rows, new WriteConfiguration(fileName, sheetName)))));
    }

    private static String getaDefaultFileName() {
        String xlsxName = XlsxFileConstants.FileFormat.XLSX.name();

        return "file." + xlsxName.toLowerCase();
    }

    private static Workbook getWorkbook() {
        return new XSSFWorkbook();
    }

    private static byte[] write(List<Map<String, ?>> rows, WriteConfiguration configuration) throws IOException {
        boolean headerRow = false;
        Workbook workbook = getWorkbook();

        Sheet sheet = workbook.createSheet(configuration.sheetName());

        for (int i = 0; i < rows.size(); i++) {
            Map<String, ?> item = rows.get(i);

            if (!headerRow) {
                headerRow = true;

                int columnCount = 0;
                Row row = sheet.createRow(0);

                for (String fieldName : item.keySet()) {
                    Cell cell = row.createCell(columnCount++);

                    cell.setCellValue(fieldName);
                }
            }

            int columnCount = 0;
            Row row = sheet.createRow(i + 1);

            for (Object value : item.values()) {
                Cell cell = row.createCell(columnCount++);

                if (value instanceof Boolean) {
                    cell.setCellValue((Boolean) value);
                } else if (value instanceof Integer) {
                    cell.setCellValue((Integer) value);
                } else if (value instanceof Long) {
                    cell.setCellValue((Long) value);
                } else if (value instanceof Double) {
                    cell.setCellValue((Double) value);
                } else if (value instanceof BigDecimal) {
                    cell.setCellValue(((BigDecimal) value).doubleValue());
                } else {
                    cell.setCellValue((String) value);
                }
            }
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        workbook.write(byteArrayOutputStream);

        workbook.close();

        return byteArrayOutputStream.toByteArray();
    }

    private record WriteConfiguration(String fileName, String sheetName) {
    }
}

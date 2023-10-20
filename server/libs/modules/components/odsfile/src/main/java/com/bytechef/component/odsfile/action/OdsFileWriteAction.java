
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.component.odsfile.action;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.Parameters;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.github.miachm.sods.Range;
import com.github.miachm.sods.Sheet;
import com.github.miachm.sods.SpreadSheet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.bytechef.component.odsfile.constant.OdsFileConstants.FILENAME;
import static com.bytechef.component.odsfile.constant.OdsFileConstants.ROWS;
import static com.bytechef.component.odsfile.constant.OdsFileConstants.SHEET_NAME;
import static com.bytechef.component.odsfile.constant.OdsFileConstants.WRITE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.definition.DefinitionDSL.array;
import static com.bytechef.hermes.definition.DefinitionDSL.display;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.oneOf;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
public class OdsFileWriteAction {

    public static final ActionDefinition ACTION_DEFINITION = action(WRITE)
        .display(display("Write to file").description("Writes the data to a ODS file."))
        .properties(
            array(ROWS)
                .label("Rows")
                .description("The array of objects to write to the file.")
                .required(true)
                .items(object().additionalProperties(oneOf())),
            string(FILENAME)
                .label("Filename")
                .description(
                    "Filename to set for binary data. By default, \"file.ods\" will be used.")
                .required(true)
                .defaultValue("file.ods")
                .advancedOption(true),
            string(SHEET_NAME)
                .label("Sheet Name")
                .description("The name of the sheet to create in the spreadsheet.")
                .defaultValue("Sheet")
                .advancedOption(true))
        .outputSchema(fileEntry())
        .perform(OdsFileWriteAction::performWrite);

    public static Context.FileEntry performWrite(Context context, Parameters parameters) {
        String fileName = parameters.getString(FILENAME, "file.ods");
        @SuppressWarnings("unchecked")
        List<Map<String, ?>> rows = (List) parameters.getList(ROWS, Map.class, List.of());

        String sheetName = parameters.getString(SHEET_NAME, "Sheet");

        try {
            return context.storeFileContent(
                fileName, new ByteArrayInputStream(write(rows, new WriteConfiguration(fileName, sheetName))));
        } catch (IOException ioException) {
            throw new ComponentExecutionException("Unable to handle action " + parameters, ioException);
        }
    }

    private static Object[] getHeaderValues(Set<String> names) {
        Objects.requireNonNull(names);

        if (names.isEmpty()) {
            throw new ComponentExecutionException("Unable to create header values with empty names collection");
        }

        Object[] values = new Object[names.size()];

        int idx = 0;

        for (Object value : names) {
            values[idx++] = value;
        }

        return values;
    }

    private static byte[] write(List<Map<String, ?>> rows, WriteConfiguration configuration) throws IOException {
        Map<String, ?> rowMap = rows.get(0);

        Object[] headerValues = getHeaderValues(rowMap.keySet());
        Object[][] values = new Object[rows.size() + 1][headerValues.length];

        values[0] = headerValues;

        for (int i = 0; i < rows.size(); i++) {
            Map<String, ?> row = rows.get(i);

            for (int j = 0; j < headerValues.length; j++) {
                values[i + 1][j] = row.get(headerValues[j]);
            }
        }

        Sheet sheet = new Sheet(configuration.sheetName(), rows.size() + 1, headerValues.length);

        SpreadSheet spreadSheet = new SpreadSheet();

        spreadSheet.appendSheet(sheet);

        Range range = sheet.getDataRange();

        range.setValues(values);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        spreadSheet.save(byteArrayOutputStream);

        return byteArrayOutputStream.toByteArray();
    }

    private record WriteConfiguration(String fileName, String sheetName) {
    }
}

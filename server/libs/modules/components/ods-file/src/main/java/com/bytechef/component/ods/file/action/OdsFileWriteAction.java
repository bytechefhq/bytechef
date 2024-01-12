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

package com.bytechef.component.ods.file.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.nullable;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.time;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.SampleOutputDataSource.ActionSampleOutputFunction;
import com.bytechef.component.definition.SampleOutputDataSource.SampleOutputResponse;
import com.bytechef.component.ods.file.constant.OdsFileConstants;
import com.github.miachm.sods.Range;
import com.github.miachm.sods.Sheet;
import com.github.miachm.sods.SpreadSheet;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.Validate;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
public class OdsFileWriteAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(OdsFileConstants.WRITE)
        .title("Write to file")
        .description("Writes the data to a ODS file.")
        .properties(
            array(OdsFileConstants.ROWS)
                .label("Rows")
                .description("The array of objects to write to the file.")
                .required(true)
                .items(object().additionalProperties(
                    array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time())),
            string(OdsFileConstants.FILENAME)
                .label("Filename")
                .description(
                    "Filename to set for binary data. By default, \"file.ods\" will be used.")
                .required(true)
                .defaultValue("file.ods")
                .advancedOption(true),
            string(OdsFileConstants.SHEET_NAME)
                .label("Sheet Name")
                .description("The name of the sheet to create in the spreadsheet.")
                .defaultValue("Sheet")
                .advancedOption(true))
        .outputSchema(fileEntry())
        .sampleOutput(getSampleOutputFunction())
        .perform(OdsFileWriteAction::perform);

    private static Object[] getHeaderValues(Set<String> names) {
        Validate.notNull(names, "'names' must not be null");

        if (names.isEmpty()) {
            throw new IllegalArgumentException("Unable to create header values with empty names collection");
        }

        Object[] values = new Object[names.size()];

        int idx = 0;

        for (Object value : names) {
            values[idx++] = value;
        }

        return values;
    }

    protected static ActionSampleOutputFunction getSampleOutputFunction() {
        return (inputParameters, connectionParameters, context) -> new SampleOutputResponse(
            perform(inputParameters, connectionParameters, context));
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        String fileName = inputParameters.getString(OdsFileConstants.FILENAME, "file.ods");
        List<Map<String, ?>> rows = (List) inputParameters.getList(OdsFileConstants.ROWS, List.of());
        String sheetName = inputParameters.getString(OdsFileConstants.SHEET_NAME, "Sheet");

        return context.file(
            file -> file.storeContent(
                fileName, new ByteArrayInputStream(write(rows, new WriteConfiguration(fileName, sheetName)))));
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

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

package com.bytechef.component.csv.file.action;

import static com.bytechef.component.csv.file.constant.CsvFileConstants.CSV_MAPPER;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.FILENAME;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.ROWS;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.ROWS_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class CsvFileWriteAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("write")
        .title("Write to CSV File")
        .description(
            "Writes the data records into a CSV file. Record values are assembled into line and separated with arbitrary character, mostly comma. CSV may or may not define header line.")
        .properties(
            ROWS_PROPERTY,
            string(FILENAME)
                .label("Filename")
                .description(
                    "Filename to set for binary data. By default, \"file.csv\" will be used.")
                .defaultValue("file.csv")
                .advancedOption(true))
        .output(outputSchema(fileEntry().description("File entry representing new csv file.")))
        .perform(CsvFileWriteAction::perform);

    protected static FileEntry perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) throws IOException {

        List<Map<String, ?>> rows = inputParameters.getList(ROWS, new TypeReference<>() {}, List.of());

        try (InputStream inputStream = new ByteArrayInputStream(write(rows))) {
            return context.file(
                file -> file.storeContent(inputParameters.getString(FILENAME, "file.csv"), inputStream));
        }
    }

    private static byte[] write(List<Map<String, ?>> rows) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        boolean headerRow = false;

        ObjectWriter writer = CSV_MAPPER.writer();

        try (PrintWriter printWriter = new PrintWriter(byteArrayOutputStream, false, StandardCharsets.UTF_8);
            SequenceWriter sequenceWriter = writer.writeValues(printWriter)) {

            for (Map<String, ?> row : rows) {
                if (!headerRow) {
                    headerRow = true;

                    sequenceWriter.write(row.keySet());
                }

                sequenceWriter.write(row.values());
            }
        }

        return byteArrayOutputStream.toByteArray();
    }
}

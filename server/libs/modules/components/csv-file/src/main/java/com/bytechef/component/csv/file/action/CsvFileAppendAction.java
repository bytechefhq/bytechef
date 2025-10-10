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
import static com.bytechef.component.csv.file.constant.CsvFileConstants.FILE_ENTRY;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.ROWS;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.ROWS_PROPERTY;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Appends rows to an existing CSV file referenced by a FileEntry.
 *
 * @author Ivica Cardic
 */
public class CsvFileAppendAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("append")
        .title("Append to CSV File")
        .description(
            "Appends the data records into an existing CSV file. Record values are assembled into a line and " +
                "separated with a delimiter (comma by default). The existing header (if any) is preserved.")
        .properties(
            fileEntry(FILE_ENTRY)
                .label("File Entry")
                .description("The object property which contains a reference to the csv file to append to.")
                .required(true),
            ROWS_PROPERTY)
        .output(outputSchema(fileEntry().description("File entry representing updated csv file.")))
        .perform(CsvFileAppendAction::perform);

    protected static FileEntry perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) throws IOException {

        FileEntry fileEntry = inputParameters.getRequiredFileEntry(FILE_ENTRY);
        List<Map<String, ?>> rows = inputParameters.getList(ROWS, new TypeReference<>() {}, List.of());

        OutputStream outputStream = context.file(f -> f.getOutputStream(fileEntry));

        ObjectWriter writer = CSV_MAPPER.writer();

        try (PrintWriter printWriter = new PrintWriter(outputStream, false, StandardCharsets.UTF_8);
            SequenceWriter sequenceWriter = writer.writeValues(printWriter)) {

            boolean headerWritten = context.file(file -> file.getContentLength(fileEntry)) > 0;

            for (Map<String, ?> row : rows) {
                if (!headerWritten) {
                    headerWritten = true;

                    sequenceWriter.write(row.keySet());
                }

                sequenceWriter.write(row.values());
            }
        }

        return fileEntry;
    }
}

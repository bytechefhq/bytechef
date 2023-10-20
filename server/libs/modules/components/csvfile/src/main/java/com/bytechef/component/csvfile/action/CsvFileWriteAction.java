
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

package com.bytechef.component.csvfile.action;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.Context.FileEntry;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.definition.ComponentDSL;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.component.util.MapValueUtils;
import com.fasterxml.jackson.databind.SequenceWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.bytechef.component.csvfile.constant.CsvFileConstants.CSV_MAPPER;
import static com.bytechef.component.csvfile.constant.CsvFileConstants.FILENAME;
import static com.bytechef.component.csvfile.constant.CsvFileConstants.ROWS;
import static com.bytechef.component.csvfile.constant.CsvFileConstants.WRITE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.definition.DefinitionDSL.array;
import static com.bytechef.hermes.definition.DefinitionDSL.bool;
import static com.bytechef.hermes.definition.DefinitionDSL.dateTime;

import static com.bytechef.hermes.definition.DefinitionDSL.number;
import static com.bytechef.hermes.definition.DefinitionDSL.oneOf;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class CsvFileWriteAction {

    public static final ActionDefinition ACTION_DEFINITION = action(WRITE)
        .title("Write to file")
        .description("Writes the data to a csv file.")
        .properties(
            array(ROWS)
                .label("Rows")
                .description("The array of objects to write to the file.")
                .required(true)
                .items(ComponentDSL.object()
                    .additionalProperties(
                        oneOf().types(bool(), dateTime(), number(), string()))),
            string(FILENAME)
                .label("Filename")
                .description(
                    "Filename to set for binary data. By default, \"file.csv\" will be used.")
                .defaultValue("file.csv")
                .advancedOption(true))
        .outputSchema(fileEntry())
        .execute(CsvFileWriteAction::executeWrite);

    protected static FileEntry executeWrite(Context context, Map<String, ?> inputParameters) {
        List<Map<String, ?>> rows = (List<Map<String, ?>>) MapValueUtils.getList(inputParameters, ROWS, List.of());

        try (InputStream inputStream = new ByteArrayInputStream(write(rows))) {
            return context.storeFileContent("file.csv", inputStream);
        } catch (IOException e) {
            throw new ComponentExecutionException(e.getMessage(), e);
        }
    }

    private static byte[] write(List<Map<String, ?>> rows) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        boolean headerRow = false;

        try (PrintWriter printWriter = new PrintWriter(byteArrayOutputStream, false, StandardCharsets.UTF_8)) {
            SequenceWriter sequenceWriter = CSV_MAPPER.writer()
                .writeValues(printWriter);

            for (Map<String, ?> row : rows) {
                if (!headerRow) {
                    headerRow = true;

                    sequenceWriter.write(row.keySet());
                }

                sequenceWriter.write(row.values());
            }

            sequenceWriter.close();
        }

        return byteArrayOutputStream.toByteArray();
    }
}

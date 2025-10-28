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

package com.bytechef.component.csv.file.datastream;

import static com.bytechef.component.csv.file.constant.CsvFileConstants.CSV_MAPPER;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.FILE_ENTRY;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;

import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.datastream.ExecutionContext;
import com.bytechef.component.definition.datastream.ItemWriter;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class CsvFileItemWriter implements ItemWriter {

    public static final ModifiableClusterElementDefinition<CsvFileItemWriter> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<CsvFileItemWriter>clusterElement("writer")
            .title("Write CSV file rows")
            .description("Writes a list of rows to a CSV file.")
            .type(DESTINATION)
            .object(CsvFileItemWriter.class)
            .properties(
                fileEntry(FILE_ENTRY)
                    .label("File Entry")
                    .description("The object property which contains a reference to the csv file to append to.")
                    .required(true));

    private BufferedWriter bufferedWriter;
    boolean headerWritten;
    private SequenceWriter sequenceWriter;

    @Override
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public void close() {
        if (bufferedWriter != null) {
            try {
                bufferedWriter.close();
            } catch (Exception e) {
                // Ignore
            }
        }

        if (sequenceWriter != null) {
            try {
                sequenceWriter.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @Override
    public void open(
        Parameters inputParameters, Parameters connectionParameters, Context context,
        ExecutionContext executionContext) {

        FileEntry fileEntry = inputParameters.getRequiredFileEntry(FILE_ENTRY);

        bufferedWriter = new BufferedWriter(
            new OutputStreamWriter(
                context.file(file -> file.getOutputStream(fileEntry)),
                StandardCharsets.UTF_8));

        headerWritten = context.file(file -> file.getContentLength(fileEntry)) > 0;

        ObjectWriter writer = CSV_MAPPER.writer();

        try {
            sequenceWriter = writer.writeValues(bufferedWriter);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(List<? extends Map<String, Object>> items) throws Exception {
        for (Map<String, ?> row : items) {
            if (!headerWritten) {
                headerWritten = true;

                sequenceWriter.write(row.keySet());
            }

            sequenceWriter.write(row.values());
        }
    }
}

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

import static com.bytechef.component.csv.file.constant.CsvFileConstants.FILE_ENTRY;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.READ_PROPERTIES;

import com.bytechef.component.csv.file.util.CsvFileReadUtils;
import com.bytechef.component.csv.file.util.ReadConfiguration;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.datastream.ExecutionContext;
import com.bytechef.component.definition.datastream.ItemReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVRecord;

/**
 * @author Ivica Cardic
 */
public class CsvFileItemReader implements ItemReader {

    public static final ModifiableClusterElementDefinition<CsvFileItemReader> CLUSTER_ELEMENT_DEFINITION = ComponentDsl
        .<CsvFileItemReader>clusterElement("reader")
        .title("Read CSV file row")
        .description("Reads a single row from a CSV file.")
        .type(SOURCE)
        .object(CsvFileItemReader.class)
        .properties(READ_PROPERTIES);

    private BufferedReader bufferedReader;
    private ReadConfiguration configuration;
    private char enclosingCharacter;
    private Iterator<CSVRecord> iterator;

    @Override
    public void close() {
        if (bufferedReader != null) {
            try {
                bufferedReader.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void open(
        Parameters inputParameters, Parameters connectionParameters, Context context,
        ExecutionContext executionContext) {

        configuration = CsvFileReadUtils.getReadConfiguration(inputParameters);

        enclosingCharacter = CsvFileReadUtils.getEnclosingCharacter(configuration);

        bufferedReader = new BufferedReader(
            new InputStreamReader(
                context.file(file -> file.getInputStream(inputParameters.getRequiredFileEntry(FILE_ENTRY))),
                StandardCharsets.UTF_8));

        try {
            iterator = CsvFileReadUtils.getIterator(bufferedReader, configuration);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> read() throws Exception {
        if (configuration.headerRow()) {
            if (iterator.hasNext()) {
                Map<String, String> row = iterator.next()
                    .toMap();

                return CsvFileReadUtils.getHeaderRow(configuration, row, enclosingCharacter);
            }
        } else {
            if (iterator.hasNext()) {
                List<?> row = (List<?>) iterator.next()
                    .toList();

                return CsvFileReadUtils.getColumnRow(configuration, row, enclosingCharacter);
            }
        }

        return null;
    }
}

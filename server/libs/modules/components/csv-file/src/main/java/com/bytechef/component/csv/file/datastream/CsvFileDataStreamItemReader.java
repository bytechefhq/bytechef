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

package com.bytechef.component.csv.file.datastream;

import static com.bytechef.component.csv.file.constant.CsvFileConstants.FILE_ENTRY;

import com.bytechef.component.csv.file.util.CsvFileReadUtils;
import com.bytechef.component.csv.file.util.ReadConfiguration;
import com.bytechef.component.definition.DataStreamContext;
import com.bytechef.component.definition.DataStreamItemReader;
import com.bytechef.component.definition.Parameters;
import com.fasterxml.jackson.databind.MappingIterator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class CsvFileDataStreamItemReader implements DataStreamItemReader {

    private BufferedReader bufferedReader;
    private ReadConfiguration configuration;
    private char enclosingCharacter;
    private MappingIterator<Object> iterator;

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
    public void open(Parameters inputParameters, Parameters connectionParameters, DataStreamContext context) {
        configuration = CsvFileReadUtils.getReadConfiguration(inputParameters);

        enclosingCharacter = CsvFileReadUtils.getEnclosingCharacter(configuration);

        bufferedReader = new BufferedReader(
            new InputStreamReader(
                context.file(file -> file.getStream(inputParameters.getRequiredFileEntry(FILE_ENTRY))),
                StandardCharsets.UTF_8));

        try {
            iterator = CsvFileReadUtils.getIterator(bufferedReader, configuration);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, ?> read(DataStreamContext context) throws Exception {
        if (configuration.headerRow()) {
            if (iterator.hasNext()) {
                Map<String, String> row = (Map<String, String>) iterator.nextValue();

                context.log(log -> log.trace("row: {}", row));

                return CsvFileReadUtils.getHeaderRow(configuration, context, row, enclosingCharacter);
            }
        } else {
            if (iterator.hasNext()) {
                List<String> row = (List<String>) iterator.nextValue();

                context.log(log -> log.trace("row: {}", row));

                return CsvFileReadUtils.getColumnRow(configuration, context, row, enclosingCharacter);
            }
        }

        return null;
    }

    @Override
    public void update(DataStreamContext context) {
    }
}

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

import static com.bytechef.component.csv.file.constant.CsvFileConstants.FILE_ENTRY;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.READ_PROPERTIES;
import static com.bytechef.component.definition.ComponentDsl.action;

import com.bytechef.component.csv.file.util.CsvFileReadUtils;
import com.bytechef.component.csv.file.util.ReadConfiguration;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.fasterxml.jackson.databind.MappingIterator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
public class CsvFileReadAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("read")
        .title("Read from File")
        .description("Reads data from a csv file.")
        .properties(READ_PROPERTIES)
        .output()
        .perform(CsvFileReadAction::perform);

    protected static List<Map<String, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) throws IOException {

        ReadConfiguration readConfiguration = CsvFileReadUtils.getReadConfiguration(inputParameters);

        try (
            InputStream inputStream = context.file(
                file -> file.getInputStream(inputParameters.getRequiredFileEntry(FILE_ENTRY)))) {

            return read(inputStream, readConfiguration, context);
        }
    }

    protected static List<Map<String, Object>> read(
        InputStream inputStream, ReadConfiguration configuration, Context context) throws IOException {

        List<Map<String, Object>> rows = new ArrayList<>();
        int count = 0;

        try (BufferedReader bufferedReader = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            char enclosingCharacter = CsvFileReadUtils.getEnclosingCharacter(configuration);

            MappingIterator<Object> iterator = CsvFileReadUtils.getIterator(bufferedReader, configuration);

            if (configuration.headerRow()) {
                while (iterator.hasNext()) {
                    Map<?, ?> row = (Map<?, ?>) iterator.nextValue();

                    if (count >= configuration.rangeStartRow() && count < configuration.rangeEndRow()) {
                        Map<String, Object> map = CsvFileReadUtils.getHeaderRow(
                            configuration, row, enclosingCharacter);

                        rows.add(map);
                    } else {
                        if (count >= configuration.rangeEndRow()) {
                            break;
                        }
                    }

                    count++;
                }
            } else {
                while (iterator.hasNext()) {
                    List<?> row = (List<?>) iterator.nextValue();

                    context.log(log -> log.trace("row: {}", row));

                    if (count >= configuration.rangeStartRow() && count < configuration.rangeEndRow()) {
                        Map<String, Object> map = CsvFileReadUtils.getColumnRow(
                            configuration, row, enclosingCharacter);

                        rows.add(map);
                    } else {
                        if (count >= configuration.rangeEndRow()) {
                            break;
                        }
                    }

                    count++;
                }
            }
        }

        return rows;
    }
}

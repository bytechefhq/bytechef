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

package com.bytechef.component.csv.file.action;

import static com.bytechef.component.csv.file.constant.CsvFileConstants.FILE_ENTRY;
import static com.bytechef.component.csv.file.constant.CsvFileConstants.ROWS;
import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;

import com.bytechef.component.csv.file.constant.CsvFileConstants;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Ivica Cardic
 */
public class CsvFileWriteAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(CsvFileConstants.WRITE)
        .title("Write to file")
        .description("Writes the data to a csv file.")
        .properties(
            array(ROWS)
                .label("Rows")
                .description("The array of objects to write to the file.")
                .required(true)
                .items(object()
                    .additionalProperties(bool(), dateTime(), number(), string())),
            fileEntry(FILE_ENTRY)
                .label("File")
                .description("File you want to write to.")
                .required(true))
        .outputSchema(fileEntry())
        .perform(CsvFileWriteAction::perform);

    private CsvFileWriteAction() {}

    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) throws IOException {

        List<Map<String, ?>> rows =
            inputParameters.getList(ROWS, new TypeReference<>() {}, List.of());
        FileEntry requiredFileEntry = inputParameters.getRequiredFileEntry(CsvFileConstants.FILE_ENTRY);

        write(rows, requiredFileEntry, context);

        return requiredFileEntry;
    }

    private static void write(List<Map<String, ?>> rows, FileEntry requiredFileEntry, ActionContext context) throws IOException {
        InputStream inputStream = context.file(file -> file.getStream(requiredFileEntry));
        Set<String> keys = null;
        try (BufferedReader bufferedReader = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            CsvSchema headerSchema = CsvSchema
                .emptySchema()
                .withHeader()
                .withColumnSeparator(',');

            MappingIterator<Map<String, String>> iterator = CsvFileConstants.CSV_MAPPER
                .readerForMapOf(String.class)
                .with(headerSchema)
                .readValues(bufferedReader);

            keys = iterator.next().keySet();
        }

        FileOutputStream outputStream = new FileOutputStream(requiredFileEntry.getUrl());
        if(keys!=null) {
            for (Map<String, ?> row : rows) {
                Map<String, String> tempMap = keys.stream()
                    .collect(Collectors.toMap(key->key, key->""));
                for (Map.Entry<String, ?> enrty: row.entrySet()){
                    if(keys.contains(enrty.getKey())) tempMap.put(enrty.getKey(), enrty.getValue().toString());
                }

                for(Map.Entry<String, String> entry : tempMap.entrySet()) {
                    outputStream.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }

}

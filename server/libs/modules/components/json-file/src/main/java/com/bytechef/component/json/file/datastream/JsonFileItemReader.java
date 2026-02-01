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

package com.bytechef.component.json.file.datastream;

import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.json.file.constant.JsonFileConstants.FILE_ENTRY;
import static com.bytechef.component.json.file.constant.JsonFileConstants.FILE_TYPE;
import static com.bytechef.component.json.file.constant.JsonFileConstants.PATH;

import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.ComponentDsl.ModifiableClusterElementDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.datastream.ExecutionContext;
import com.bytechef.component.definition.datastream.FieldDefinition;
import com.bytechef.component.definition.datastream.ItemReader;
import com.bytechef.component.json.file.constant.FileType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public class JsonFileItemReader implements ItemReader {

    public static final ModifiableClusterElementDefinition<JsonFileItemReader> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<JsonFileItemReader>clusterElement("reader")
            .title("Read JSON file row")
            .description("Reads a single row from a JSON file.")
            .type(SOURCE)
            .object(JsonFileItemReader.class)
            .properties(
                string(FILE_TYPE)
                    .label("File Type")
                    .description("The file type to choose.")
                    .options(
                        option("JSON", FileType.JSON.name()),
                        option("JSON Line", FileType.JSONL.name()))
                    .defaultValue(FileType.JSON.name())
                    .required(true),
                fileEntry(FILE_ENTRY)
                    .label("File Entry")
                    .description("The object property which contains a reference to the JSON file to read from.")
                    .required(true),
                string(PATH)
                    .label("Path")
                    .description("The path where the array is e.g 'data'. Leave blank to use the top level object.")
                    .required(false));

    private BufferedReader bufferedReader;
    private FileType fileType;
    private InputStream inputStream;
    private Iterator<Object> iterator;

    @Override
    public void close() {
        if (bufferedReader != null) {
            try {
                bufferedReader.close();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }

        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void open(
        Parameters inputParameters, Parameters connectionParameters, Context context,
        ExecutionContext executionContext) {

        this.fileType = getFileType(inputParameters);

        FileEntry fileEntry = inputParameters.getRequiredFileEntry(FILE_ENTRY);

        this.inputStream = context.file(file -> file.getInputStream(fileEntry));

        if (fileType == FileType.JSONL) {
            this.bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            List<Object> jsonlItems = bufferedReader
                .lines()
                .map(line -> context.json(json -> json.read(line)))
                .collect(Collectors.toCollection(ArrayList::new));

            this.iterator = jsonlItems.iterator();
        } else {
            String path = inputParameters.getString(PATH);
            List<Object> items;

            if (path == null || path.isEmpty()) {
                try (Stream<Map<String, ?>> stream = context.json(json -> json.stream(inputStream))) {
                    items = stream.collect(Collectors.toCollection(ArrayList::new));
                }
            } else {
                items = (List<Object>) context.json(json -> json.read(inputStream, path));
            }

            this.iterator = items.iterator();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> read() {
        if (iterator.hasNext()) {
            return (Map<String, Object>) iterator.next();
        }

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<FieldDefinition> getFields(
        Parameters inputParameters, Parameters connectionParameters, ClusterElementContext context) {

        FileType fileType = getFileType(inputParameters);
        FileEntry fileEntry = inputParameters.getRequiredFileEntry(FILE_ENTRY);
        Map<String, Object> firstItem = null;

        try (InputStream inputStream = context.file(file -> file.getInputStream(fileEntry))) {
            if (fileType == FileType.JSONL) {
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                    String firstLine = reader.readLine();

                    if (firstLine != null && !firstLine.isEmpty()) {
                        firstItem = (Map<String, Object>) context.json(json -> json.read(firstLine));
                    }
                }
            } else {
                String path = inputParameters.getString(PATH);

                if (path == null || path.isEmpty()) {
                    try (Stream<Map<String, ?>> stream = context.json(json -> json.stream(inputStream))) {
                        firstItem = (Map<String, Object>) stream.findFirst()
                            .orElse(null);
                    }
                } else {
                    List<Map<String, ?>> items =
                        (List<Map<String, ?>>) context.json(json -> json.read(inputStream, path));

                    if (items != null && !items.isEmpty()) {
                        firstItem = (Map<String, Object>) items.getFirst();
                    }
                }
            }
        } catch (IOException exception) {
            throw new RuntimeException("Failed to read JSON file for field discovery", exception);
        }

        if (firstItem == null) {
            return List.of();
        }

        Map<String, Object> itemToFlatten = firstItem;

        Map<String, Object> flattenedItem = context.nested(nested -> nested.flatten(itemToFlatten));

        return flattenedItem.entrySet()
            .stream()
            .map(entry -> new FieldDefinition(
                entry.getKey(),
                entry.getKey(),
                entry.getValue() != null ? entry.getValue()
                    .getClass() : String.class))
            .toList();
    }

    private FileType getFileType(Parameters inputParameters) {
        String fileType = inputParameters.getString(FILE_TYPE, FileType.JSON.name());

        return FileType.valueOf(fileType.toUpperCase());
    }
}

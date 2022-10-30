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

package com.bytechef.component.json.file;

import static com.bytechef.component.json.file.constants.JsonFileTaskConstants.FILE_TYPE;
import static com.bytechef.component.json.file.constants.JsonFileTaskConstants.IS_ARRAY;
import static com.bytechef.component.json.file.constants.JsonFileTaskConstants.JSON_FILE;
import static com.bytechef.component.json.file.constants.JsonFileTaskConstants.PAGE_NUMBER;
import static com.bytechef.component.json.file.constants.JsonFileTaskConstants.PAGE_SIZE;
import static com.bytechef.component.json.file.constants.JsonFileTaskConstants.PATH;
import static com.bytechef.component.json.file.constants.JsonFileTaskConstants.READ;
import static com.bytechef.component.json.file.constants.JsonFileTaskConstants.SOURCE;
import static com.bytechef.component.json.file.constants.JsonFileTaskConstants.WRITE;
import static com.bytechef.hermes.component.ComponentDSL.any;
import static com.bytechef.hermes.component.ComponentDSL.array;
import static com.bytechef.hermes.component.ComponentDSL.bool;
import static com.bytechef.hermes.component.ComponentDSL.createComponent;
import static com.bytechef.hermes.component.ComponentDSL.display;
import static com.bytechef.hermes.component.ComponentDSL.fileEntry;
import static com.bytechef.hermes.component.ComponentDSL.integer;
import static com.bytechef.hermes.component.ComponentDSL.object;
import static com.bytechef.hermes.component.ComponentDSL.option;
import static com.bytechef.hermes.component.ComponentDSL.options;
import static com.bytechef.hermes.component.ComponentDSL.showWhen;
import static com.bytechef.hermes.component.ComponentDSL.string;
import static com.bytechef.hermes.component.constants.ComponentConstants.FILENAME;
import static com.bytechef.hermes.component.constants.ComponentConstants.FILE_ENTRY;

import com.bytechef.commons.json.JsonUtils;
import com.bytechef.component.json.file.constants.JsonFileTaskConstants.FileType;
import com.bytechef.hermes.component.ComponentDSL;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.exception.ActionExecutionException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Ivica Cardic
 */
public class JsonFileComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition = createComponent(JSON_FILE)
            .display(display("JSON File").description("Reads and writes data from a JSON file."))
            .actions(
                    ComponentDSL.action(READ)
                            .display(display("Read from file").description("Reads data from a JSON file."))
                            .inputs(
                                    string(FILE_TYPE)
                                            .label("File Type")
                                            .description("The file type to choose.")
                                            .options(
                                                    option("JSON", FileType.JSON.name()),
                                                    option("JSON Line", FileType.JSONL.name()))
                                            .defaultValue(FileType.JSON.name())
                                            .required(true),
                                    fileEntry(FILE_ENTRY)
                                            .label("File")
                                            .description(
                                                    "The object property which contains a reference to the JSON file to read from.")
                                            .required(true),
                                    bool(IS_ARRAY)
                                            .label("Is Array")
                                            .description("The object input is array?")
                                            .defaultValue(true),
                                    options()
                                            .label("Options")
                                            .placeholder("Add Option")
                                            .options(
                                                    string(PATH)
                                                            .label("Path")
                                                            .description(
                                                                    "The path where the array is e.g 'data'. Leave blank to use the top level object.")
                                                            .displayOption(showWhen(IS_ARRAY)
                                                                    .eq(true)),
                                                    integer(PAGE_SIZE)
                                                            .label("Page Size")
                                                            .description(
                                                                    "The amount of child elements to return in a page.")
                                                            .displayOption(showWhen(IS_ARRAY)
                                                                    .eq(true)),
                                                    integer(PAGE_NUMBER)
                                                            .label("Page Number")
                                                            .description("The page number to get.")
                                                            .displayOption(showWhen(IS_ARRAY)
                                                                    .eq(true))))
                            .outputSchema(array(), object())
                            .performFunction(this::performRead),
                    ComponentDSL.action(WRITE)
                            .display(display("Write to file").description("Writes the data to a JSON file."))
                            .inputs(
                                    string(FILE_TYPE)
                                            .label("File Type")
                                            .description("The file type to choose.")
                                            .options(
                                                    option("JSON", FileType.JSON.name()),
                                                    option("JSON Line", FileType.JSONL.name()))
                                            .defaultValue(FileType.JSON.name())
                                            .required(true),
                                    any(SOURCE)
                                            .label("Source")
                                            .description("The data to write to the file.")
                                            .required(true)
                                            .types(array(), object()),
                                    string(FILENAME)
                                            .label("Filename")
                                            .description(
                                                    "Filename to set for binary data. By default, \"file.json\" will be used.")
                                            .required(true)
                                            .defaultValue("file.json"))
                            .outputSchema(fileEntry())
                            .performFunction(this::performWrite));

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    @SuppressWarnings("unchecked")
    protected Object performRead(Context context, ExecutionParameters executionParameters)
            throws ActionExecutionException {

        FileType fileType = getFileType(executionParameters);
        FileEntry fileEntry = executionParameters.getFileEntry(FILE_ENTRY);
        boolean isArray = executionParameters.getBoolean(IS_ARRAY, true);
        Object result;

        if (isArray) {
            String path = executionParameters.getString(PATH);
            InputStream inputStream = context.getFileStream(fileEntry);
            List<Map<String, ?>> items;

            if (fileType == FileType.JSON) {
                if (path == null) {
                    try (Stream<Map<String, ?>> stream = JsonUtils.stream(inputStream)) {
                        items = stream.toList();
                    }
                } else {
                    items = JsonUtils.read(inputStream, path);
                }
            } else {
                try (BufferedReader bufferedReader =
                        new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    items = bufferedReader
                            .lines()
                            .map(line -> (Map<String, ?>) JsonUtils.read(line, Map.class))
                            .collect(Collectors.toList());
                } catch (IOException ioException) {
                    throw new ActionExecutionException("Unable to open json file " + executionParameters, ioException);
                }
            }

            Integer pageSize = executionParameters.getInteger(PAGE_SIZE);
            Integer pageNumber = executionParameters.getInteger(PAGE_NUMBER);
            Integer rangeStartIndex = null;
            Integer rangeEndIndex = null;

            if (pageSize != null && pageNumber != null) {
                rangeStartIndex = pageSize * pageNumber - pageSize;

                rangeEndIndex = rangeStartIndex + pageSize;
            }

            if ((rangeStartIndex != null && rangeStartIndex > 0)
                    || (rangeEndIndex != null && rangeEndIndex < items.size())) {
                items = items.subList(rangeStartIndex, rangeEndIndex);
            }

            result = items;
        } else {
            result = JsonUtils.read(context.readFileToString(fileEntry), Map.class);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    protected FileEntry performWrite(Context context, ExecutionParameters executionParameters)
            throws ActionExecutionException {
        FileType fileType = getFileType(executionParameters);
        Object source = executionParameters.getRequiredObject(SOURCE);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        if (fileType == FileType.JSON) {
            try (PrintWriter printWriter = new PrintWriter(byteArrayOutputStream, false, StandardCharsets.UTF_8)) {
                printWriter.println(JsonUtils.write(source));
            }
        } else {
            try (PrintWriter printWriter = new PrintWriter(byteArrayOutputStream, false, StandardCharsets.UTF_8)) {
                for (Map<String, ?> item : (List<Map<String, ?>>) source) {
                    printWriter.println(JsonUtils.write(item));
                }
            }
        }

        try (InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray())) {
            return context.storeFileContent(
                    getDefaultFileName(fileType, executionParameters.getString(FILENAME)), inputStream);
        } catch (IOException ioException) {
            throw new ActionExecutionException("Unable to create json file", ioException);
        }
    }

    private String getDefaultFileName(FileType fileType, String defaultFilename) {
        return defaultFilename == null ? ("file." + (fileType == FileType.JSON ? "json" : "jsonl")) : defaultFilename;
    }

    private FileType getFileType(ExecutionParameters executionParameters) {

        return FileType.valueOf(StringUtils.upperCase(executionParameters.getString(FILE_TYPE, FileType.JSON.name())));
    }
}

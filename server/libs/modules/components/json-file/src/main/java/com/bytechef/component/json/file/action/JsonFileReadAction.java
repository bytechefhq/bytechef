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

package com.bytechef.component.json.file.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.json.file.constant.JsonFileConstants.FILE_ENTRY;
import static com.bytechef.component.json.file.constant.JsonFileConstants.FILE_TYPE;
import static com.bytechef.component.json.file.constant.JsonFileConstants.IS_ARRAY;
import static com.bytechef.component.json.file.constant.JsonFileConstants.PAGE_NUMBER;
import static com.bytechef.component.json.file.constant.JsonFileConstants.PAGE_SIZE;
import static com.bytechef.component.json.file.constant.JsonFileConstants.PATH;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.json.file.constant.FileType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Ivica Cardic
 */
public class JsonFileReadAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("read")
        .title("Read from File")
        .description("Reads data from a JSON file.")
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
                .description(
                    "The object property which contains a reference to the JSON file to read from.")
                .required(true),
            bool(IS_ARRAY)
                .label("Is Array")
                .description("The object input is array?")
                .defaultValue(true),
            string(PATH)
                .label("Path")
                .description(
                    "The path where the array is e.g 'data'. Leave blank to use the top level object.")
                .displayCondition("%s == true".formatted(IS_ARRAY))
                .advancedOption(true),
            integer(PAGE_SIZE)
                .label("Page Size")
                .description("The amount of child elements to return in a page.")
                .displayCondition("%s == true".formatted(IS_ARRAY))
                .advancedOption(true),
            integer(PAGE_NUMBER)
                .label("Page Number")
                .description("The page number to get.")
                .displayCondition("%s == true".formatted(IS_ARRAY))
                .advancedOption(true))
        .output()
        .perform(JsonFileReadAction::perform);

    protected static FileType getFileType(Parameters inputParameters) {
        String fileType = inputParameters.getString(FILE_TYPE, FileType.JSON.name());

        return FileType.valueOf(fileType.toUpperCase());
    }

    @SuppressWarnings("unchecked")
    protected static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) throws IOException {

        FileType fileType = getFileType(inputParameters);
        FileEntry fileEntry = inputParameters.getRequiredFileEntry(FILE_ENTRY);
        boolean isArray = inputParameters.getBoolean(IS_ARRAY, true);
        Object result;

        if (isArray) {
            String path = inputParameters.getString(PATH);
            InputStream inputStream = context.file(file -> file.getInputStream(fileEntry));
            List<Map<String, ?>> items;

            if (fileType == FileType.JSON) {
                if (path == null) {
                    try (Stream<Map<String, ?>> stream = context.json(json -> json.stream(inputStream))) {
                        items = stream.toList();
                    }
                } else {
                    items =
                        (List<Map<String, ?>>) context.json(jsonOperations -> jsonOperations.read(inputStream, path));
                }
            } else {
                try (BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                    items = bufferedReader
                        .lines()
                        .map(line -> (Map<String, ?>) context.json(json -> json.read(line)))
                        .collect(Collectors.toList());
                }
            }

            Integer pageSize = inputParameters.getInteger(PAGE_SIZE);
            Integer pageNumber = inputParameters.getInteger(PAGE_NUMBER);
            Integer rangeStartIndex = null;
            Integer rangeEndIndex = null;

            if (pageSize != null && pageNumber != null) {
                rangeStartIndex = pageSize * pageNumber - pageSize;

                rangeEndIndex = rangeStartIndex + pageSize;
            }

            if (rangeStartIndex != null && rangeStartIndex > 0
                || rangeEndIndex != null && rangeEndIndex < items.size()) {
                items = items.subList(rangeStartIndex, rangeEndIndex);
            }

            result = items;
        } else {
            result = context.json(json -> json.read((String) context.file(file -> file.readToString(fileEntry))));
        }

        return result;
    }
}

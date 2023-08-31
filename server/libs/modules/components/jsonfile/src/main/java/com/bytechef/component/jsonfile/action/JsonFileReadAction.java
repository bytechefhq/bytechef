
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

package com.bytechef.component.jsonfile.action;

import com.bytechef.component.jsonfile.constant.JsonFileTaskConstants.FileType;
import com.bytechef.hermes.component.definition.Context;
import com.bytechef.hermes.component.definition.Context.FileEntry;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource.OutputSchemaFunction;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.component.util.JsonUtils;
import com.bytechef.hermes.component.util.MapUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.bytechef.component.jsonfile.constant.JsonFileTaskConstants.FILE_ENTRY;
import static com.bytechef.component.jsonfile.constant.JsonFileTaskConstants.FILE_TYPE;
import static com.bytechef.component.jsonfile.constant.JsonFileTaskConstants.IS_ARRAY;
import static com.bytechef.component.jsonfile.constant.JsonFileTaskConstants.PAGE_NUMBER;
import static com.bytechef.component.jsonfile.constant.JsonFileTaskConstants.PAGE_SIZE;
import static com.bytechef.component.jsonfile.constant.JsonFileTaskConstants.PATH;
import static com.bytechef.component.jsonfile.constant.JsonFileTaskConstants.READ;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.definition.DefinitionDSL.bool;

import static com.bytechef.hermes.definition.DefinitionDSL.integer;
import static com.bytechef.hermes.definition.DefinitionDSL.option;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class JsonFileReadAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(READ)
        .title("Read from file")
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
                .label("File")
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
                .displayCondition("%s === true".formatted(IS_ARRAY))
                .advancedOption(true),
            integer(PAGE_SIZE)
                .label("Page Size")
                .description("The amount of child elements to return in a page.")
                .displayCondition("%s === true".formatted(IS_ARRAY))
                .advancedOption(true),
            integer(PAGE_NUMBER)
                .label("Page Number")
                .description("The page number to get.")
                .displayCondition("%s === true".formatted(IS_ARRAY))
                .advancedOption(true))
        .outputSchema(getOutputSchemaFunction())
        .perform(JsonFileReadAction::perform);

    @SuppressWarnings("unchecked")
    protected static Object perform(Map<String, ?> inputParameters, Context context)
        throws ComponentExecutionException {

        FileType fileType = getFileType(inputParameters);
        FileEntry fileEntry = MapUtils.getRequired(inputParameters, FILE_ENTRY, FileEntry.class);
        boolean isArray = MapUtils.getBoolean(inputParameters, IS_ARRAY, true);
        Object result;

        if (isArray) {
            String path = MapUtils.getString(inputParameters, PATH);
            InputStream inputStream = context.getFileStream(fileEntry);
            List<Map<String, ?>> items;

            if (fileType == FileType.JSON) {
                if (path == null) {
                    try (Stream<Map<String, ?>> stream = JsonUtils.stream(inputStream)) {
                        items = stream.toList();
                    }
                } else {
                    items = (List<Map<String, ?>>) JsonUtils.read(inputStream, path);
                }
            } else {
                try (BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                    items = bufferedReader
                        .lines()
                        .map(line -> (Map<String, ?>) JsonUtils.read(line))
                        .collect(Collectors.toList());
                } catch (IOException ioException) {
                    throw new ComponentExecutionException("Unable to open json file " + inputParameters, ioException);
                }
            }

            Integer pageSize = MapUtils.getInteger(inputParameters, PAGE_SIZE);
            Integer pageNumber = MapUtils.getInteger(inputParameters, PAGE_NUMBER);
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
            result = JsonUtils.read(context.readFileToString(fileEntry));
        }

        return result;
    }

    protected static FileType getFileType(Map<String, ?> inputParameters) {
        String fileType = MapUtils.getString(inputParameters, FILE_TYPE, FileType.JSON.name());

        return FileType.valueOf(fileType.toUpperCase());
    }

    protected static OutputSchemaFunction getOutputSchemaFunction() {
        // TODO
        return (connection, inputParameters) -> null;
    }
}

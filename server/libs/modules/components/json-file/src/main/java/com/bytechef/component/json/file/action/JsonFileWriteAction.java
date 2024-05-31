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

package com.bytechef.component.json.file.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.json.file.constant.JsonFileConstants.FILENAME;
import static com.bytechef.component.json.file.constant.JsonFileConstants.FILE_TYPE;
import static com.bytechef.component.json.file.constant.JsonFileConstants.SOURCE;
import static com.bytechef.component.json.file.constant.JsonFileConstants.TYPE;
import static com.bytechef.component.json.file.constant.JsonFileConstants.WRITE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.json.file.constant.JsonFileConstants;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class JsonFileWriteAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(WRITE)
        .title("Write to file")
        .description("Writes the data to a JSON file.")
        .properties(
            string(FILE_TYPE)
                .label("File Type")
                .description("The file type to choose.")
                .options(
                    option("JSON", JsonFileConstants.FileType.JSON.name()),
                    option("JSON Line", JsonFileConstants.FileType.JSONL.name()))
                .defaultValue(JsonFileConstants.FileType.JSON.name())
                .required(true),
            integer(TYPE)
                .label("Type")
                .description("The value type.")
                .options(
                    option("Object", 1),
                    option("Array", 2)),
            object(SOURCE)
                .label("Source")
                .description("The object to write to the file.")
                .displayCondition("type == 1")
                .required(true),
            array(SOURCE)
                .label("Source")
                .description("The array to write to the file.")
                .displayCondition("type == 2")
                .required(true),
            string(FILENAME)
                .label("Filename")
                .description(
                    "Filename to set for binary data. By default, \"file.json\" will be used.")
                .required(true)
                .defaultValue("file.json")
                .advancedOption(true))
        .outputSchema(fileEntry())
        .perform(JsonFileWriteAction::perform);

    private static String getDefaultFileName(JsonFileConstants.FileType fileType, String defaultFilename) {
        return defaultFilename == null
            ? "file." + (fileType == JsonFileConstants.FileType.JSON ? "json" : "jsonl")
            : defaultFilename;
    }

    @SuppressWarnings("unchecked")
    protected static FileEntry perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) throws IOException {

        JsonFileConstants.FileType fileType = JsonFileReadAction.getFileType(inputParameters);
        Object source = inputParameters.getRequired(SOURCE);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        if (fileType == JsonFileConstants.FileType.JSON) {
            try (PrintWriter printWriter = new PrintWriter(byteArrayOutputStream, false, StandardCharsets.UTF_8)) {
                printWriter.println((String) context.json(json -> json.write(source)));
            }
        } else {
            try (PrintWriter printWriter = new PrintWriter(byteArrayOutputStream, false, StandardCharsets.UTF_8)) {
                for (Map<String, ?> item : (List<Map<String, ?>>) source) {
                    printWriter.println((String) context.json(json -> json.write(item)));
                }
            }
        }

        try (InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray())) {
            return context.file(file -> file.storeContent(
                getDefaultFileName(fileType, inputParameters.getString(FILENAME)), inputStream));
        }
    }
}

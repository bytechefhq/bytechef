
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

import com.bytechef.component.jsonfile.constant.JsonFileTaskConstants;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.component.util.JsonUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.bytechef.component.jsonfile.constant.JsonFileTaskConstants.FILENAME;
import static com.bytechef.component.jsonfile.constant.JsonFileTaskConstants.FILE_TYPE;
import static com.bytechef.component.jsonfile.constant.JsonFileTaskConstants.SOURCE;
import static com.bytechef.component.jsonfile.constant.JsonFileTaskConstants.WRITE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.definition.DefinitionDSL.array;
import static com.bytechef.hermes.definition.DefinitionDSL.display;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.oneOf;
import static com.bytechef.hermes.definition.DefinitionDSL.option;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class JsonFileWriteAction {

    public static final ActionDefinition ACTION_DEFINITION = action(WRITE)
        .display(display("Write to file").description("Writes the data to a JSON file."))
        .properties(
            string(FILE_TYPE)
                .label("File Type")
                .description("The file type to choose.")
                .options(
                    option("JSON", JsonFileTaskConstants.FileType.JSON.name()),
                    option("JSON Line", JsonFileTaskConstants.FileType.JSONL.name()))
                .defaultValue(JsonFileTaskConstants.FileType.JSON.name())
                .required(true),
            oneOf(SOURCE)
                .label("Source")
                .description("The data to write to the file.")
                .required(true)
                .types(array(), object()),
            string(FILENAME)
                .label("Filename")
                .description(
                    "Filename to set for binary data. By default, \"file.json\" will be used.")
                .required(true)
                .defaultValue("file.json")
                .advancedOption(true))
        .outputSchema(fileEntry())
        .execute(JsonFileWriteAction::executeWrite);

    @SuppressWarnings("unchecked")
    public static Context.FileEntry executeWrite(Context context, InputParameters inputParameters)
        throws ComponentExecutionException {
        JsonFileTaskConstants.FileType fileType = JsonFileReadAction.getFileType(inputParameters);
        Object source = inputParameters.getRequired(SOURCE);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        if (fileType == JsonFileTaskConstants.FileType.JSON) {
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
                getDefaultFileName(fileType, inputParameters.getString(FILENAME)), inputStream);
        } catch (IOException ioException) {
            throw new ComponentExecutionException("Unable to create json file", ioException);
        }
    }

    private static String getDefaultFileName(JsonFileTaskConstants.FileType fileType, String defaultFilename) {
        return defaultFilename == null ? "file." + (fileType == JsonFileTaskConstants.FileType.JSON ? "json" : "jsonl")
            : defaultFilename;
    }
}

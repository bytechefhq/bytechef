
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

package com.bytechef.component.filesystem.action;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.definition.ActionDefinition;

import java.io.File;

import static com.bytechef.component.filesystem.constant.FilesystemConstants.FILENAME;
import static com.bytechef.component.filesystem.constant.FilesystemConstants.GET_FILE_PATH;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;

import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class FilesystemGetFilePathAction {

    public static final ActionDefinition ACTION_DEFINITION = action(GET_FILE_PATH)
        .title("File Path")
        .description(
            "Gets the full path from a full filename, which is the prefix + path, and also excluding the final directory separator.")
        .properties(string(FILENAME)
            .label("Filename")
            .description("The path to full filename.")
            .placeholder("/data/your_file.pdf")
            .required(true))
        .execute(FilesystemGetFilePathAction::executeGetFilePath)
        .outputSchema(string())
        .exampleOutput("/data");

    /**
     * Gets the full path from a full filename, which is the prefix + path, and also excluding the final directory
     * separator.
     *
     * <p>
     * This method will handle a file in either Unix or Windows format. The method is entirely text based and returns
     * the text before the last forward or backslash.
     */
    public static String executeGetFilePath(Context context, InputParameters inputParameters) {
        String filename = inputParameters.getRequiredString("filename");

        return filename.substring(0, filename.lastIndexOf(File.separator));
    }
}

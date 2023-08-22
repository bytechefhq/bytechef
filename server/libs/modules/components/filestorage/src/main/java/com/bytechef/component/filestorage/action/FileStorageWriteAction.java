
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

package com.bytechef.component.filestorage.action;

import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.Context.FileEntry;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.util.MapUtils;

import java.util.Map;

import static com.bytechef.component.filestorage.constant.FileStorageConstants.CONTENT;
import static com.bytechef.component.filestorage.constant.FileStorageConstants.FILENAME;
import static com.bytechef.component.filestorage.constant.FileStorageConstants.WRITE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;

import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class FileStorageWriteAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(WRITE)
        .title("Write to file")
        .description("Writes the data to the file.")
        .properties(
            string(CONTENT)
                .label("Content")
                .description("String to write to the file.")
                .required(true),
            string(FILENAME)
                .label("Filename")
                .description(
                    "Filename to set for data. By default, \"file.txt\" will be used.")
                .defaultValue("file.txt"))
        .outputSchema(fileEntry())
        .perform(FileStorageWriteAction::perform);

    protected static FileEntry perform(Map<String, ?> inputParameters, Context context) {
        Object content = MapUtils.getRequired(inputParameters, CONTENT);
        String fileName = MapUtils.getString(inputParameters, FILENAME, "file.txt");

        return context.storeFileContent(fileName, content instanceof String ? (String) content : content.toString());
    }
}

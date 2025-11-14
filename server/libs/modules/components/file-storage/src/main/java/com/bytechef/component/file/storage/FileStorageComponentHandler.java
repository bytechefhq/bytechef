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

package com.bytechef.component.file.storage;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.file.storage.action.FileStorageDownloadAction;
import com.bytechef.component.file.storage.action.FileStorageReadAction;
import com.bytechef.component.file.storage.action.FileStorageWriteAction;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class FileStorageComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("fileStorage")
        .title("File Storage")
        .description("Reads and writes data from a file stored inside the file storage.")
        .icon("path:assets/file-storage.svg")
        .categories(ComponentCategory.FILE_STORAGE, ComponentCategory.HELPERS)
        .actions(
            FileStorageReadAction.ACTION_DEFINITION,
            FileStorageWriteAction.ACTION_DEFINITION,
            FileStorageDownloadAction.ACTION_DEFINITION)
        .clusterElements(
            tool(FileStorageReadAction.ACTION_DEFINITION),
            tool(FileStorageWriteAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

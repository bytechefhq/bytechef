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

package com.bytechef.component.file.storage;

import static com.bytechef.hermes.component.definition.ComponentDSL.component;

import com.bytechef.component.file.storage.action.FileStorageDownloadAction;
import com.bytechef.component.file.storage.action.FileStorageReadAction;
import com.bytechef.component.file.storage.action.FileStorageWriteAction;
import com.bytechef.component.file.storage.constant.FileStorageConstants;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class FileStorageComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(FileStorageConstants.FILE_STORAGE)
        .title("File Storage")
        .description("Reads and writes data from a file stored inside the file storage.")
        .icon("path:assets/file-storage.svg")
        .actions(
            FileStorageReadAction.ACTION_DEFINITION,
            FileStorageWriteAction.ACTION_DEFINITION,
            FileStorageDownloadAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}


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

package com.bytechef.component.filestorage;

import static com.bytechef.component.filestorage.constant.FileStorageConstants.FILE_STORAGE;
import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;

import com.bytechef.component.filestorage.action.FileStorageDownloadAction;
import com.bytechef.component.filestorage.action.FileStorageReadAction;
import com.bytechef.component.filestorage.action.FileStorageWriteAction;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.definition.ComponentDefinition;

/**
 * @author Ivica Cardic
 */
public class FileStorageComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(FILE_STORAGE)
        .display(display("File Storage").description("Reads and writes data from a file"))
        .actions(
            FileStorageReadAction.READ_ACTION,
            FileStorageWriteAction.WRITE_ACTION,
            FileStorageDownloadAction.DOWNLOAD_ACTION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

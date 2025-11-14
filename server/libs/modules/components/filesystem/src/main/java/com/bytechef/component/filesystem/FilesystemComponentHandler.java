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

package com.bytechef.component.filesystem;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.filesystem.action.FilesystemCreateTempDirAction;
import com.bytechef.component.filesystem.action.FilesystemGetParentFolderAction;
import com.bytechef.component.filesystem.action.FilesystemLsAction;
import com.bytechef.component.filesystem.action.FilesystemMkdirAction;
import com.bytechef.component.filesystem.action.FilesystemReadFileAction;
import com.bytechef.component.filesystem.action.FilesystemRmAction;
import com.bytechef.component.filesystem.action.FilesystemWriteFileAction;
import com.google.auto.service.AutoService;

/**
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class FilesystemComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("filesystem")
        .title("Filesystem")
        .description("Allows multiple operations over files on the filesystem.")
        .icon("path:assets/filesystem.svg")
        .categories(ComponentCategory.HELPERS)
        .actions(
            FilesystemCreateTempDirAction.ACTION_DEFINITION,
            FilesystemMkdirAction.ACTION_DEFINITION,
            FilesystemGetParentFolderAction.ACTION_DEFINITION,
            FilesystemLsAction.ACTION_DEFINITION,
            FilesystemReadFileAction.ACTION_DEFINITION,
            FilesystemRmAction.ACTION_DEFINITION,
            FilesystemWriteFileAction.ACTION_DEFINITION)
        .clusterElements(
            tool(FilesystemCreateTempDirAction.ACTION_DEFINITION),
            tool(FilesystemMkdirAction.ACTION_DEFINITION),
            tool(FilesystemGetParentFolderAction.ACTION_DEFINITION),
            tool(FilesystemLsAction.ACTION_DEFINITION),
            tool(FilesystemReadFileAction.ACTION_DEFINITION),
            tool(FilesystemRmAction.ACTION_DEFINITION),
            tool(FilesystemWriteFileAction.ACTION_DEFINITION));

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

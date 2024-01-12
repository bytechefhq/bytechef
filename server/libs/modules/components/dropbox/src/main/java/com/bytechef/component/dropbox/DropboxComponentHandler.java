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

package com.bytechef.component.dropbox;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.dropbox.constant.DropboxConstants.DROPBOX;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.dropbox.action.DropboxCopyAction;
import com.bytechef.component.dropbox.action.DropboxCreateNewFolderAction;
import com.bytechef.component.dropbox.action.DropboxCreateNewTextFileAction;
import com.bytechef.component.dropbox.action.DropboxDeleteAction;
import com.bytechef.component.dropbox.action.DropboxGetFileLinkAction;
import com.bytechef.component.dropbox.action.DropboxListFolderAction;
import com.bytechef.component.dropbox.action.DropboxMoveAction;
import com.bytechef.component.dropbox.action.DropboxSearchAction;
import com.bytechef.component.dropbox.action.DropboxUploadFileAction;
import com.bytechef.component.dropbox.connection.DropboxConnection;
import com.google.auto.service.AutoService;

/**
 * @author Mario Cvjetojevic
 */
@AutoService(ComponentHandler.class)
public class DropboxComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(DROPBOX)
        .title("Dropbox")
        .description(
            "Dropbox is a file hosting service that offers cloud storage, file synchronization, personal cloud, " +
                "and client software.")
        .icon("path:assets/dropbox.svg")
        .connection(DropboxConnection.CONNECTION_DEFINITION)
        .actions(
            DropboxCopyAction.ACTION_DEFINITION,
            DropboxCreateNewFolderAction.ACTION_DEFINITION,
            DropboxCreateNewTextFileAction.ACTION_DEFINITION,
            DropboxDeleteAction.ACTION_DEFINITION,
            DropboxGetFileLinkAction.ACTION_DEFINITION,
            DropboxListFolderAction.ACTION_DEFINITION,
            DropboxMoveAction.ACTION_DEFINITION,
            DropboxSearchAction.ACTION_DEFINITION,
            DropboxUploadFileAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

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

package com.bytechef.component.google.drive;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;
import static com.bytechef.component.google.drive.connection.GoogleDriveConnection.CONNECTION_DEFINITION;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.google.drive.action.GoogleDriveCopyFileAction;
import com.bytechef.component.google.drive.action.GoogleDriveCreateNewFolderAction;
import com.bytechef.component.google.drive.action.GoogleDriveCreateNewTextFileAction;
import com.bytechef.component.google.drive.action.GoogleDriveDeleteFileAction;
import com.bytechef.component.google.drive.action.GoogleDriveDownloadFileAction;
import com.bytechef.component.google.drive.action.GoogleDriveGetFileAction;
import com.bytechef.component.google.drive.action.GoogleDriveListFilesAction;
import com.bytechef.component.google.drive.action.GoogleDriveListFoldersAction;
import com.bytechef.component.google.drive.action.GoogleDriveUploadFileAction;
import com.bytechef.component.google.drive.trigger.GoogleDriveNewFileTrigger;
import com.bytechef.component.google.drive.trigger.GoogleDriveNewFolderTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Mario Cvjetojevic
 * @author Monika Kušter
 */
@AutoService(ComponentHandler.class)
public class GoogleDriveComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("googleDrive")
        .title("Google Drive")
        .description(
            "Google Drive is a cloud storage service by Google that enables users to store, sync, share files, and " +
                "collaborate online.")
        .customAction(true)
        .icon("path:assets/google-drive.svg")
        .categories(ComponentCategory.FILE_STORAGE)
        .connection(CONNECTION_DEFINITION)
        .actions(
            GoogleDriveCopyFileAction.ACTION_DEFINITION,
            GoogleDriveCreateNewFolderAction.ACTION_DEFINITION,
            GoogleDriveCreateNewTextFileAction.ACTION_DEFINITION,
            GoogleDriveDeleteFileAction.ACTION_DEFINITION,
            GoogleDriveDownloadFileAction.ACTION_DEFINITION,
            GoogleDriveGetFileAction.ACTION_DEFINITION,
            GoogleDriveListFilesAction.ACTION_DEFINITION,
            GoogleDriveListFoldersAction.ACTION_DEFINITION,
            GoogleDriveUploadFileAction.ACTION_DEFINITION)
        .clusterElements(
            tool(GoogleDriveCopyFileAction.ACTION_DEFINITION),
            tool(GoogleDriveCreateNewFolderAction.ACTION_DEFINITION),
            tool(GoogleDriveCreateNewTextFileAction.ACTION_DEFINITION),
            tool(GoogleDriveDeleteFileAction.ACTION_DEFINITION),
            tool(GoogleDriveDownloadFileAction.ACTION_DEFINITION),
            tool(GoogleDriveGetFileAction.ACTION_DEFINITION),
            tool(GoogleDriveListFilesAction.ACTION_DEFINITION),
            tool(GoogleDriveListFoldersAction.ACTION_DEFINITION),
            tool(GoogleDriveUploadFileAction.ACTION_DEFINITION))
        .triggers(
            GoogleDriveNewFileTrigger.TRIGGER_DEFINITION,
            GoogleDriveNewFolderTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }

}

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

package com.bytechef.component.microsoft.one.drive;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.component.definition.ComponentDsl.tool;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.microsoft.one.drive.action.MicrosoftOneDriveCopyFileAction;
import com.bytechef.component.microsoft.one.drive.action.MicrosoftOneDriveCreateFolderAction;
import com.bytechef.component.microsoft.one.drive.action.MicrosoftOneDriveCreateNewTextFileAction;
import com.bytechef.component.microsoft.one.drive.action.MicrosoftOneDriveDeleteFileAction;
import com.bytechef.component.microsoft.one.drive.action.MicrosoftOneDriveDownloadFileAction;
import com.bytechef.component.microsoft.one.drive.action.MicrosoftOneDriveGetFileAction;
import com.bytechef.component.microsoft.one.drive.action.MicrosoftOneDriveListFilesAction;
import com.bytechef.component.microsoft.one.drive.action.MicrosoftOneDriveListFoldersAction;
import com.bytechef.component.microsoft.one.drive.action.MicrosoftOneDriveUploadFileAction;
import com.bytechef.component.microsoft.one.drive.connection.MicrosoftOneDriveConnection;
import com.bytechef.component.microsoft.one.drive.trigger.MicrosoftOneDriveNewFileTrigger;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class MicrosoftOneDriveComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("microsoftOneDrive")
        .title("Microsoft OneDrive")
        .description(
            "Microsoft OneDrive is a cloud storage service provided by Microsoft for storing, accessing, and sharing " +
                "files online.")
        .customAction(true)
        .icon("path:assets/microsoft-one-drive.svg")
        .categories(ComponentCategory.FILE_STORAGE)
        .connection(MicrosoftOneDriveConnection.CONNECTION_DEFINITION)
        .actions(
            MicrosoftOneDriveCopyFileAction.ACTION_DEFINITION,
            MicrosoftOneDriveCreateFolderAction.ACTION_DEFINITION,
            MicrosoftOneDriveCreateNewTextFileAction.ACTION_DEFINITION,
            MicrosoftOneDriveDeleteFileAction.ACTION_DEFINITION,
            MicrosoftOneDriveDownloadFileAction.ACTION_DEFINITION,
            MicrosoftOneDriveGetFileAction.ACTION_DEFINITION,
            MicrosoftOneDriveListFilesAction.ACTION_DEFINITION,
            MicrosoftOneDriveListFoldersAction.ACTION_DEFINITION,
            MicrosoftOneDriveUploadFileAction.ACTION_DEFINITION)
        .clusterElements(
            tool(MicrosoftOneDriveCopyFileAction.ACTION_DEFINITION),
            tool(MicrosoftOneDriveCreateFolderAction.ACTION_DEFINITION),
            tool(MicrosoftOneDriveCreateNewTextFileAction.ACTION_DEFINITION),
            tool(MicrosoftOneDriveDeleteFileAction.ACTION_DEFINITION),
            tool(MicrosoftOneDriveDownloadFileAction.ACTION_DEFINITION),
            tool(MicrosoftOneDriveGetFileAction.ACTION_DEFINITION),
            tool(MicrosoftOneDriveListFilesAction.ACTION_DEFINITION),
            tool(MicrosoftOneDriveListFoldersAction.ACTION_DEFINITION),
            tool(MicrosoftOneDriveUploadFileAction.ACTION_DEFINITION))
        .triggers(MicrosoftOneDriveNewFileTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}

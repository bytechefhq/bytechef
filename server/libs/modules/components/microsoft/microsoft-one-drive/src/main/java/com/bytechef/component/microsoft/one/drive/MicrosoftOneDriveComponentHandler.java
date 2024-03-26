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

package com.bytechef.component.microsoft.one.drive;

import static com.bytechef.component.definition.ComponentDSL.component;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.MICROSOFT_ONEDRIVE;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.component.microsoft.one.drive.action.MicrosoftOneDriveDownloadFileAction;
import com.bytechef.component.microsoft.one.drive.action.MicrosoftOneDriveListFilesAction;
import com.bytechef.component.microsoft.one.drive.action.MicrosoftOneDriveListFoldersAction;
import com.bytechef.component.microsoft.one.drive.action.MicrosoftOneDriveUploadFileAction;
import com.bytechef.component.microsoft.one.drive.connection.MicrosoftOneDriveConnection;
import com.google.auto.service.AutoService;

/**
 * @author Monika Domiter
 */
@AutoService(ComponentHandler.class)
public class MicrosoftOneDriveComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(MICROSOFT_ONEDRIVE)
        .title("Microsoft OneDrive")
        .description(
            "Microsoft OneDrive is a cloud storage service provided by Microsoft for storing, accessing, and sharing " +
                "files online.")
        .icon("path:assets/microsoft-one-drive.svg")
        .connection(MicrosoftOneDriveConnection.CONNECTION_DEFINITION)
        .actions(
            MicrosoftOneDriveDownloadFileAction.ACTION_DEFINITION,
            MicrosoftOneDriveListFilesAction.ACTION_DEFINITION,
            MicrosoftOneDriveListFoldersAction.ACTION_DEFINITION,
            MicrosoftOneDriveUploadFileAction.ACTION_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
